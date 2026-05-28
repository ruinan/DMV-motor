package com.dmvmotor.api.aisupport.application;

import com.dmvmotor.api.mockexam.application.MockAttemptCompletedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Kicks off AI review-plan generation after a mock attempt completes.
 *
 * <p>{@code AFTER_COMMIT} guarantees the attempt + its answer rows are durably
 * persisted before the job reads them; {@code @Async} runs the LLM call on a
 * background thread so the user's submit / exit response isn't blocked.
 */
@Component
public class MockReviewPlanListener {

    private final AiReviewPlanService reviewPlanService;

    public MockReviewPlanListener(AiReviewPlanService reviewPlanService) {
        this.reviewPlanService = reviewPlanService;
    }

    @Async("aiTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMockCompleted(MockAttemptCompletedEvent event) {
        reviewPlanService.generateAndCache(event.attemptId(), event.userId());
    }
}
