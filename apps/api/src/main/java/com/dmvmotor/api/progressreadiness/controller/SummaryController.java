package com.dmvmotor.api.progressreadiness.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.progressreadiness.application.SummaryService;
import com.dmvmotor.api.progressreadiness.application.SummaryService.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    public ApiResponse<?> getSummary(@CurrentUser Long userId) {
        requireAuth(userId);
        SummaryResult result = summaryService.getSummary(userId);
        return ApiResponse.ok(Map.of(
                "completion_score",  result.completionScore(),
                "readiness_score",   result.readinessScore(),
                "is_ready_candidate", result.isReadyCandidate(),
                "weak_topics", result.weakTopics().stream()
                        .map(t -> Map.of(
                                "topic_id", String.valueOf(t.topicId()),
                                "label",    t.label()))
                        .toList(),
                "next_action", Map.of(
                        "type",  result.nextActionType(),
                        "label", result.nextActionLabel())
        ));
    }

    @GetMapping("/readiness")
    public ApiResponse<?> getReadiness(@CurrentUser Long userId) {
        requireAuth(userId);
        ReadinessResult result = summaryService.getReadiness(userId);
        return ApiResponse.ok(Map.of(
                "readiness_score",    result.readinessScore(),
                "is_ready_candidate", result.isReadyCandidate(),
                "missing_gates",      result.missingGates()
        ));
    }

    private void requireAuth(Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
    }
}
