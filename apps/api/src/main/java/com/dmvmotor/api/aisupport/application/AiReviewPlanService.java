package com.dmvmotor.api.aisupport.application;

import com.dmvmotor.api.aisupport.config.AiProperties;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.AttemptRow;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.WrongAnswerDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * AI review plan for a completed mock-exam attempt.
 *
 * <p>Generation is system-driven, not user-driven: when a mock reaches a
 * terminal state, {@code MockReviewPlanListener} calls
 * {@link #generateAndCache(Long, Long)} on a background thread. There is no
 * user-facing "generate" trigger — the client only ever <em>reads</em> the
 * result via {@link #getCachedPlan(Long, Long)}. This keeps the AI on a fixed,
 * server-controlled payload (no free-text), and the user never waits on the LLM.
 */
@Service
public class AiReviewPlanService {

    private static final Logger log = LoggerFactory.getLogger(AiReviewPlanService.class);
    private static final double PASS_THRESHOLD = 0.85;

    private final MockExamRepository   mockExamRepo;
    private final AiReviewPlanProvider provider;
    private final AiProperties         props;

    public AiReviewPlanService(MockExamRepository mockExamRepo,
                                AiReviewPlanProvider provider,
                                AiProperties props) {
        this.mockExamRepo = mockExamRepo;
        this.provider     = provider;
        this.props        = props;
    }

    /**
     * Background job: generate and persist the plan for a finished attempt.
     * Fire-and-forget — never throws to the caller. Skips when AI is off, the
     * attempt isn't terminal, or a plan is already cached (idempotent). Provider
     * failures are logged and swallowed; the client simply keeps seeing
     * {@code pending} until a later attempt regenerates (it won't auto-retry,
     * but a failed plan is non-critical).
     */
    @Transactional
    public void generateAndCache(Long attemptId, Long userId) {
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
        if (mockExamRepo.findReviewPlan(attemptId).isPresent()) {
            return; // idempotent — already generated
        }

        try {
            String lang = attempt.language();
            int correctCount = mockExamRepo.countCorrectAnswers(attemptId);
            int total = mockExamRepo.findMockExamQuestionCount(attempt.mockExamId());
            int scorePercent = total == 0 ? 0 : (int) Math.round(100.0 * correctCount / total);
            boolean passed = scorePercent >= (int) Math.round(PASS_THRESHOLD * 100);

            List<WrongAnswerDetail> wrong = mockExamRepo.findWrongAnswerDetails(attemptId, lang);
            List<AiReviewPlanProvider.WrongItem> wrongItems = wrong.stream()
                    .map(w -> new AiReviewPlanProvider.WrongItem(
                            w.stem(), w.topicLabel(), w.subTopicLabel(),
                            w.selectedChoiceKey(), w.correctChoiceKey()))
                    .toList();

            AiReviewPlanProvider.Output out = provider.generate(
                    new AiReviewPlanProvider.Input(
                            scorePercent, correctCount, total, passed, wrongItems, lang));

            mockExamRepo.saveReviewPlan(attemptId, out.text(), provider.modelName());
        } catch (RuntimeException e) {
            // AI provider error (timeout / 5xx / parse). Non-critical — log and
            // leave the plan absent so the client shows the neutral state.
            log.warn("AI review plan generation failed for attempt {}: {}",
                    attemptId, e.toString());
        }
    }

    /**
     * Read path for the client. Reports whether the plan is ready, still being
     * generated, or unavailable (AI off). Ownership / existence are still hard
     * errors (403 / 404) so a status probe can't leak other users' attempts.
     */
    public PlanView getCachedPlan(Long attemptId, Long userId) {
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
        Optional<String> cached = mockExamRepo.findReviewPlan(attemptId);
        return cached
                .map(plan -> new PlanView(Status.READY, plan))
                .orElseGet(() -> new PlanView(Status.PENDING, null));
    }

    public enum Status { READY, PENDING, UNAVAILABLE }

    public record PlanView(Status status, String plan) {}
}
