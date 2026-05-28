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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PostMapping("/review-plan")
    public ApiResponse<?> reviewPlan(@CurrentUser Long userId,
                                      @Valid @RequestBody ReviewPlanRequest req) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
        long attemptId = Ids.parse(req.mockAttemptId(), "mock_attempt_id");
        // Pass language through (may be null/blank) — the service falls back to
        // the attempt's stored language when it's absent.
        AiReviewPlanService.Result result =
                reviewPlanService.generate(attemptId, userId, req.language());
        return ApiResponse.ok(Map.of(
                "plan",   result.plan(),
                "cached", result.cached()
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

    record ReviewPlanRequest(
            @NotBlank(message = "must not be blank") String mock_attempt_id,
            String language
    ) {
        String mockAttemptId() { return mock_attempt_id; }
    }
}
