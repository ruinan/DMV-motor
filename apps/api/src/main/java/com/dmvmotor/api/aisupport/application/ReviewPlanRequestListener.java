package com.dmvmotor.api.aisupport.application;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Lazily generates a review plan in a language that wasn't cached yet (fired by
 * a GET that found no cached plan for the requested language). Runs on the AI
 * background executor so the request doesn't wait on the LLM. A plain
 * {@code @EventListener} (not transactional) since the triggering GET isn't a
 * write transaction; the cache claim was already taken before publishing.
 */
@Component
public class ReviewPlanRequestListener {

    private final AiReviewPlanService reviewPlanService;

    public ReviewPlanRequestListener(AiReviewPlanService reviewPlanService) {
        this.reviewPlanService = reviewPlanService;
    }

    @Async("aiTaskExecutor")
    @EventListener
    public void onRequested(ReviewPlanRequestedEvent event) {
        reviewPlanService.generateAndCache(event.attemptId(), event.userId(), event.language());
    }
}
