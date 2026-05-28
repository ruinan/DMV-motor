package com.dmvmotor.api.aisupport.controller;

import com.dmvmotor.api.aisupport.application.AiExplanationService;
import com.dmvmotor.api.aisupport.application.AiReviewPlanService;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.common.Ids;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiExplanationController {

    private final AiExplanationService service;
    private final AiReviewPlanService  reviewPlanService;

    public AiExplanationController(AiExplanationService service,
                                   AiReviewPlanService reviewPlanService) {
        this.service           = service;
        this.reviewPlanService = reviewPlanService;
    }

    @PostMapping("/explain")
    public ApiResponse<?> explain(@CurrentUser Long userId,
                                   @Valid @RequestBody ExplainRequest req) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
        long questionId = Ids.parse(req.questionId(), "question_id");
        Long variantId = req.variantId() == null ? null : Ids.parse(req.variantId(), "variant_id");
        String language = req.language() == null ? "en" : req.language();

        AiExplanationService.Result result = service.explain(userId, questionId, variantId,
                req.selectedChoiceKey(), language);

        return ApiResponse.ok(Map.of(
                "explanation", result.explanation(),
                "cached",      result.cached(),
                "model",       result.model(),
                "language",    result.language()
        ));
    }

    /**
     * Read-only fetch of the auto-generated review plan. Generation is kicked
     * off in the background when the mock completes (see MockReviewPlanListener)
     * — the client never triggers the LLM, it only polls for the result here.
     * status ∈ {ready, pending, unavailable}.
     */
    @GetMapping("/review-plan")
    public ApiResponse<?> reviewPlan(@CurrentUser Long userId,
                                      @RequestParam("mock_attempt_id") String mockAttemptId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
        long attemptId = Ids.parse(mockAttemptId, "mock_attempt_id");
        AiReviewPlanService.PlanView view = reviewPlanService.getCachedPlan(attemptId, userId);
        return ApiResponse.ok(Map.of(
                "status", view.status().name().toLowerCase(),
                "plan",   view.plan() != null ? view.plan() : ""
        ));
    }

    record ExplainRequest(
            @NotBlank(message = "must not be blank") String question_id,
            String variant_id,
            String selected_choice_key,
            String language
    ) {
        String questionId()         { return question_id; }
        String variantId()          { return variant_id; }
        String selectedChoiceKey()  { return selected_choice_key; }
    }
}
