package com.dmvmotor.api.aisupport.controller;

import com.dmvmotor.api.aisupport.application.RecommendationService;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Proactive "what to reinforce next" recommendations (mvp.md §5 #10).
 * {@code GET /api/v1/ai/recommendations} — ranked topics with a structured
 * reason and a topic_filter the client can hand straight to start-practice.
 * Deterministic ranking; an LLM-phrased reason can layer on later (§34-B).
 */
@RestController
@RequestMapping("/api/v1/ai")
public class RecommendationController {

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping("/recommendations")
    public ApiResponse<?> recommendations(
            @CurrentUser Long userId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false, defaultValue = "3") int limit
    ) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
        String lang = (language == null || language.isBlank()) ? "en" : language;
        List<RecommendationDto> items = service.recommend(userId, lang, limit).stream()
                .map(RecommendationDto::from).toList();
        return ApiResponse.ok(new RecommendationsDto(items));
    }

    record RecommendationsDto(List<RecommendationDto> recommendations) {}

    record RecommendationDto(String topicId, String label, String reasonCode,
                             int mistakeCount, List<String> topicFilter) {
        static RecommendationDto from(RecommendationService.Recommendation r) {
            return new RecommendationDto(
                    String.valueOf(r.topicId()), r.label(), r.reasonCode(),
                    r.mistakeCount(), List.of(String.valueOf(r.topicId())));
        }
    }
}
