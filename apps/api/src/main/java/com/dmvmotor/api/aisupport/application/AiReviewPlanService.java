package com.dmvmotor.api.aisupport.application;

import com.dmvmotor.api.aisupport.config.AiProperties;
import com.dmvmotor.api.aisupport.infrastructure.ReviewPlanRepository;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.AttemptRow;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.WrongAnswerDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * AI review plan for a completed mock attempt, cached <em>per language</em>
 * (V25) so switching the UI language shows the plan in that language.
 *
 * <p>Generation stays system-driven (no free-text user trigger): the mock's
 * language is generated eagerly on completion ({@code MockReviewPlanListener});
 * any other language is generated lazily the first time it's read — the GET
 * publishes a {@link ReviewPlanRequestedEvent} and keeps returning PENDING
 * while the background job runs. A {@code claim()} placeholder row dedups
 * concurrent generations so polling doesn't fire duplicate LLM calls.
 */
@Service
public class AiReviewPlanService {

    private static final Logger log = LoggerFactory.getLogger(AiReviewPlanService.class);

    private final MockExamRepository       mockExamRepo;
    private final ReviewPlanRepository     reviewPlanRepo;
    private final AiReviewPlanProvider     provider;
    private final AiProperties             props;
    private final ApplicationEventPublisher events;

    public AiReviewPlanService(MockExamRepository mockExamRepo,
                                ReviewPlanRepository reviewPlanRepo,
                                AiReviewPlanProvider provider,
                                AiProperties props,
                                ApplicationEventPublisher events) {
        this.mockExamRepo   = mockExamRepo;
        this.reviewPlanRepo = reviewPlanRepo;
        this.provider       = provider;
        this.props          = props;
        this.events         = events;
    }

    /** Eager entry (mock completion): generate the plan in the mock's language. */
    public void generateAndCache(Long attemptId, Long userId) {
        mockExamRepo.findAttemptById(attemptId)
                .ifPresent(a -> generateAndCache(attemptId, userId, a.language()));
    }

    /**
     * Generate + cache the plan for one language. Fire-and-forget (never throws
     * to the caller). Not {@code @Transactional}: the {@code claim} commits
     * immediately so concurrent callers see it and skip (no duplicate LLM call);
     * the long LLM call runs outside any transaction.
     */
    public void generateAndCache(Long attemptId, Long userId, String language) {
        if (!props.enabled()) {
            return;
        }
        Optional<AttemptRow> maybe = mockExamRepo.findAttemptById(attemptId);
        if (maybe.isEmpty() || !maybe.get().userId().equals(userId)) {
            log.warn("Skipping review plan: attempt {} missing or not owned by user {}",
                    attemptId, userId);
            return;
        }
        AttemptRow attempt = maybe.get();
        if ("in_progress".equals(attempt.status())) {
            return;
        }
        if (reviewPlanRepo.findReadyPlan(attemptId, language).isPresent()) {
            return; // already generated for this language
        }
        if (!reviewPlanRepo.claim(attemptId, language)) {
            return; // another generation is already in flight — don't duplicate
        }

        try {
            int correctCount = mockExamRepo.countCorrectAnswers(attemptId);
            int total = mockExamRepo.findMockExamQuestionCount(attempt.mockExamId());
            int scorePercent = total == 0 ? 0 : (int) Math.round(100.0 * correctCount / total);
            // Pass standard comes from the attempt's exam (exams.pass_threshold_percent),
            // not a hardcoded 85% — each state × license type sets its own bar.
            int passThresholdPercent = mockExamRepo.findPassThresholdPercent(attempt.mockExamId());
            boolean passed = scorePercent >= passThresholdPercent;

            // Wrong-answer detail in the REQUESTED language so the plan's
            // question references read in that language too.
            List<WrongAnswerDetail> wrong = mockExamRepo.findWrongAnswerDetails(attemptId, language);
            List<AiReviewPlanProvider.WrongItem> wrongItems = wrong.stream()
                    .map(w -> new AiReviewPlanProvider.WrongItem(
                            w.stem(), w.topicLabel(), w.subTopicLabel(),
                            w.selectedChoiceKey(), w.correctChoiceKey()))
                    .toList();

            AiReviewPlanProvider.Output out = provider.generate(
                    new AiReviewPlanProvider.Input(
                            scorePercent, correctCount, total, passed, wrongItems, language));

            reviewPlanRepo.markReady(attemptId, language, out.text(), provider.modelName());
        } catch (RuntimeException e) {
            // Release the claim so a later poll retries; non-critical.
            reviewPlanRepo.releaseClaim(attemptId, language);
            log.warn("AI review plan generation failed for attempt {} ({}): {}",
                    attemptId, language, e.toString());
        }
    }

    /**
     * Read path. Returns the plan for the requested language if ready; otherwise
     * lazily kicks off generation for that language and reports PENDING.
     * Ownership / existence stay hard errors so a probe can't leak attempts.
     */
    public PlanView getCachedPlan(Long attemptId, Long userId, String language) {
        AttemptRow attempt = mockExamRepo.findAttemptById(attemptId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND",
                        "Mock attempt not found: " + attemptId, HttpStatus.NOT_FOUND));
        if (!attempt.userId().equals(userId)) {
            throw new BusinessException("FORBIDDEN",
                    "Attempt belongs to a different user", HttpStatus.FORBIDDEN);
        }
        if (!props.enabled()) {
            return new PlanView(Status.UNAVAILABLE, null);
        }
        Optional<String> ready = reviewPlanRepo.findReadyPlan(attemptId, language);
        if (ready.isPresent()) {
            return new PlanView(Status.READY, ready.get());
        }
        // Not cached for this language — kick off lazy generation (dedup'd by the
        // claim inside generateAndCache) and report PENDING while it runs.
        if (!"in_progress".equals(attempt.status())) {
            events.publishEvent(new ReviewPlanRequestedEvent(attemptId, userId, language));
        }
        return new PlanView(Status.PENDING, null);
    }

    public enum Status { READY, PENDING, UNAVAILABLE }

    public record PlanView(Status status, String plan) {}
}
