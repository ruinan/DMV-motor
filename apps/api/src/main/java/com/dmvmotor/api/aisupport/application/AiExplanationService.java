package com.dmvmotor.api.aisupport.application;

import com.dmvmotor.api.aisupport.config.AiProperties;
import com.dmvmotor.api.aisupport.domain.AiExplanation;
import com.dmvmotor.api.aisupport.infrastructure.AiExplanationRepository;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.content.application.ContentService;
import com.dmvmotor.api.content.domain.QuestionDetail;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Returns an AI-generated explanation for why the user's selected choice was wrong.
 *
 * <p>Order of checks (cache wins; rate-limit only on cache miss):
 * <ol>
 *   <li>Cache hit on {@code (user_id, question_id, language)} → return saved row,
 *       no provider call, no rate-limit.
 *   <li>Access gate: defer to {@link ContentService#getQuestion} so a free-trial
 *       user asking about a paid-only question gets the same 404 they would for
 *       any other paid-only path — no information leakage about ID existence.
 *   <li>Rate limit (decision §27.2 #6): cooldown grows with usage (base + N×inc,
 *       capped at max). A daily cap closes the loop against persistent abuse.
 *   <li>Provider call → persist → return.
 * </ol>
 */
@Service
public class AiExplanationService {

    private final AiExplanationRepository repo;
    private final AiExplanationProvider   provider;
    private final ContentService          contentService;
    private final AiProperties            props;

    public AiExplanationService(AiExplanationRepository repo,
                                 AiExplanationProvider provider,
                                 ContentService contentService,
                                 AiProperties props) {
        this.repo           = repo;
        this.provider       = provider;
        this.contentService = contentService;
        this.props          = props;
    }

    public Result explain(Long userId, Long questionId, Long variantId,
                          String selectedChoiceKey, String language) {

        // 1. Cache hit short-circuit — same (user, question, language) returns the
        //    persisted row. No provider call, no rate-limit consumption.
        Optional<AiExplanation> cached =
                repo.findByUserQuestionLanguage(userId, questionId, language);
        if (cached.isPresent()) {
            AiExplanation hit = cached.get();
            return new Result(hit.explanation(), true, hit.model(), language);
        }

        // 2. Access gate via ContentService — paid-only question for a free-trial
        //    user → ResourceNotFoundException → 404 (matches the answer-leak
        //    hardening in QuestionController, sec audit #1).
        QuestionDetail question = contentService.getQuestion(userId, questionId, language);

        // 3. Rate limit — only on the cache-miss path.
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime since = now.minusHours(24);
        int callsToday = repo.countByUserSince(userId, since);

        if (callsToday >= props.maxCallsPerDay()) {
            throw new BusinessException("RATE_LIMITED",
                    "Daily AI cap reached (" + props.maxCallsPerDay() + " per 24h)",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        long cooldownSec = Math.min(
                (long) props.baseCooldownSeconds()
                        + (long) callsToday * props.cooldownIncrementSeconds(),
                (long) props.maxCooldownSeconds());

        Optional<OffsetDateTime> lastCall = repo.findLatestCreatedAtByUser(userId);
        if (lastCall.isPresent()) {
            long elapsed = Duration.between(lastCall.get(), now).getSeconds();
            if (elapsed < cooldownSec) {
                long retryAfter = cooldownSec - elapsed;
                throw new BusinessException("RATE_LIMITED",
                        "AI cooling down — try again in " + retryAfter + "s",
                        HttpStatus.TOO_MANY_REQUESTS);
            }
        }

        // 4. Provider call — minimal context per docs/ai-architecture.md §11.
        List<Map<String, String>> choices = question.choices().stream()
                .map(c -> Map.of("key", c.key(), "text", c.text()))
                .toList();

        AiExplanationProvider.Output out = provider.explain(
                new AiExplanationProvider.Input(
                        questionId,
                        question.stem(),
                        choices,
                        question.correctChoiceKey(),
                        selectedChoiceKey,
                        question.explanation(),
                        language
                ));

        // 5. Persist + return.
        AiExplanation saved = repo.insert(
                userId, questionId, language, selectedChoiceKey,
                out.text(), provider.modelName(), out.tokensIn(), out.tokensOut());

        return new Result(saved.explanation(), false, saved.model(), language);
    }

    public record Result(
            String  explanation,
            boolean cached,
            String  model,
            String  language
    ) {}
}
