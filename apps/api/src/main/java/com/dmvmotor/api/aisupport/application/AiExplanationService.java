package com.dmvmotor.api.aisupport.application;

import com.dmvmotor.api.aisupport.config.AiProperties;
import com.dmvmotor.api.aisupport.domain.AiExplanation;
import com.dmvmotor.api.aisupport.infrastructure.AiDeepDiveLogRepository;
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
 * AI-generated explanations. Two depths:
 * <ul>
 *   <li><b>Base (depth 0)</b> — why the user's pick was wrong. DB-cached on
 *       {@code (user, question, language)} (the cost ceiling); a cache hit
 *       skips the provider and the rate-limit.</li>
 *   <li><b>Deep dive (depth ≥ 1)</b> — "深入分析", a deeper layer. NOT persisted
 *       server-side (the client keeps the thread in localStorage); the server
 *       only logs that a call happened, to enforce a per-question depth cap and
 *       to bill it against the rate-limit. Re-burning after a cache clear still
 *       counts toward the cap.</li>
 * </ul>
 *
 * <p>Rate limit (decision §27.2 #6, extended by enhance1): the daily cap and
 * cooldown count base cache-misses <em>and</em> deep-dives — both are billable
 * LLM calls.
 */
@Service
public class AiExplanationService {

    private final AiExplanationRepository repo;
    private final AiDeepDiveLogRepository deepDiveRepo;
    private final AiExplanationProvider   provider;
    private final ContentService          contentService;
    private final AiProperties            props;

    public AiExplanationService(AiExplanationRepository repo,
                                 AiDeepDiveLogRepository deepDiveRepo,
                                 AiExplanationProvider provider,
                                 ContentService contentService,
                                 AiProperties props) {
        this.repo           = repo;
        this.deepDiveRepo   = deepDiveRepo;
        this.provider       = provider;
        this.contentService = contentService;
        this.props          = props;
    }

    public Result explain(Long userId, Long questionId, Long variantId,
                          String selectedChoiceKey, String language, int depth) {
        // Feature flag — stable AI_UNAVAILABLE so the client shows "off" rather
        // than leaking a stub / canned text.
        if (!props.enabled()) {
            throw new BusinessException("AI_UNAVAILABLE",
                    "AI explanations are currently turned off",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return depth <= 0
                ? explainBase(userId, questionId, selectedChoiceKey, language)
                : deepDive(userId, questionId, selectedChoiceKey, language, depth);
    }

    // ---- depth 0: base explanation (DB-cached) -------------------------------

    private Result explainBase(Long userId, Long questionId,
                               String selectedChoiceKey, String language) {
        // 1. Cache hit short-circuit — no provider call, no rate-limit.
        Optional<AiExplanation> cached =
                repo.findByUserQuestionLanguage(userId, questionId, language);
        if (cached.isPresent()) {
            AiExplanation hit = cached.get();
            return new Result(hit.explanation(), true, hit.model(), language,
                    0, props.maxDeepDivesPerQuestion());
        }

        // 2. Access gate via ContentService — paid-only question for a free-trial
        //    user → 404 (matches the answer-leak hardening, sec audit #1).
        QuestionDetail question = contentService.getQuestion(userId, questionId, language);

        // 3. Rate limit (cache-miss path only).
        enforceRateLimit(userId);

        // 4. Provider call (depth 0) — minimal context per docs/ai-architecture.md §11.
        AiExplanationProvider.Output out = provider.explain(toInput(question, selectedChoiceKey, language, 0));

        // 5. Persist + return.
        AiExplanation saved = repo.insert(
                userId, questionId, language, selectedChoiceKey,
                out.text(), provider.modelName(), out.tokensIn(), out.tokensOut());
        return new Result(saved.explanation(), false, saved.model(), language,
                0, props.maxDeepDivesPerQuestion());
    }

    // ---- depth ≥ 1: deep dive (not persisted, only counted) ------------------

    private Result deepDive(Long userId, Long questionId,
                            String selectedChoiceKey, String language, int depth) {
        // Access gate (also loads the question content for the provider).
        QuestionDetail question = contentService.getQuestion(userId, questionId, language);

        // Per-question depth cap — survives a client localStorage clear, so
        // repeatedly clearing + re-burning still hits the ceiling.
        int used = deepDiveRepo.countByUserQuestionLanguage(userId, questionId, language);
        if (used >= props.maxDeepDivesPerQuestion()) {
            throw new BusinessException("RATE_LIMITED",
                    "Deep-dive limit reached for this question ("
                            + props.maxDeepDivesPerQuestion() + " layers)",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        // Daily cap + cooldown (counts deep-dives too).
        enforceRateLimit(userId);

        AiExplanationProvider.Output out =
                provider.explain(toInput(question, selectedChoiceKey, language, depth));

        // Log metadata only (no text). One row per LLM call, including re-burns.
        deepDiveRepo.insert(userId, questionId, language, depth);

        int remaining = Math.max(0, props.maxDeepDivesPerQuestion() - (used + 1));
        return new Result(out.text(), false, provider.modelName(), language, depth, remaining);
    }

    // ---- shared helpers ------------------------------------------------------

    private AiExplanationProvider.Input toInput(QuestionDetail question,
                                                String selectedChoiceKey,
                                                String language, int depth) {
        List<Map<String, String>> choices = question.choices().stream()
                .map(c -> Map.of("key", c.key(), "text", c.text()))
                .toList();
        return new AiExplanationProvider.Input(
                question.questionId(),
                question.stem(),
                choices,
                question.correctChoiceKey(),
                selectedChoiceKey,
                question.explanation(),
                language,
                depth);
    }

    /**
     * Daily cap + thinking-time cooldown, counting base cache-misses and
     * deep-dives together (both are billable LLM calls).
     */
    private void enforceRateLimit(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime since = now.minusHours(24);

        int callsToday = repo.countByUserSince(userId, since)
                + deepDiveRepo.countByUserSince(userId, since);
        if (callsToday >= props.maxCallsPerDay()) {
            throw new BusinessException("RATE_LIMITED",
                    "Daily AI cap reached (" + props.maxCallsPerDay() + " per 24h)",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        long cooldownSec = Math.min(
                (long) props.baseCooldownSeconds()
                        + (long) callsToday * props.cooldownIncrementSeconds(),
                (long) props.maxCooldownSeconds());

        Optional<OffsetDateTime> lastCall = latestCallAcrossSources(userId);
        if (lastCall.isPresent()) {
            long elapsed = Duration.between(lastCall.get(), now).getSeconds();
            if (elapsed < cooldownSec) {
                throw new BusinessException("RATE_LIMITED",
                        "AI cooling down — try again in " + (cooldownSec - elapsed) + "s",
                        HttpStatus.TOO_MANY_REQUESTS);
            }
        }
    }

    private Optional<OffsetDateTime> latestCallAcrossSources(Long userId) {
        Optional<OffsetDateTime> base = repo.findLatestCreatedAtByUser(userId);
        Optional<OffsetDateTime> deep = deepDiveRepo.findLatestCreatedAtByUser(userId);
        if (base.isEmpty()) return deep;
        if (deep.isEmpty()) return base;
        return Optional.of(base.get().isAfter(deep.get()) ? base.get() : deep.get());
    }

    public record Result(
            String  explanation,
            boolean cached,
            String  model,
            String  language,
            int     depth,
            int     depthRemaining
    ) {}
}
