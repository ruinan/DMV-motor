package com.dmvmotor.api.progressreadiness.controller;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.authaccess.application.AccessService.AccessInfo;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.progressreadiness.application.SummaryService;
import com.dmvmotor.api.progressreadiness.application.SummaryService.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SummaryController {

    private final SummaryService summaryService;
    private final AccessService  accessService;

    public SummaryController(SummaryService summaryService, AccessService accessService) {
        this.summaryService = summaryService;
        this.accessService  = accessService;
    }

    @GetMapping("/summary")
    public ApiResponse<?> getSummary(@CurrentUser Long userId) {
        requireAuth(userId);
        AccessInfo access = accessService.getAccess(userId);
        SummaryResult result = summaryService.getSummary(userId);

        // docs/parameters.md §3.1: free users see partial summary — no full readiness conclusion.
        // Paid (active pass) users see full readiness + is_ready_candidate.
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("access_state", access.state());
        data.put("completion_score", result.completionScore());
        data.put("weak_topics", result.weakTopics().stream()
                .map(t -> Map.of(
                        "topic_id", String.valueOf(t.topicId()),
                        "label",    t.label()))
                .toList());
        data.put("next_action", Map.of(
                "type",  result.nextActionType(),
                "label", result.nextActionLabel()));

        if (access.hasActivePass()) {
            data.put("readiness_score",    result.readinessScore());
            data.put("is_ready_candidate", result.isReadyCandidate());
        }
        return ApiResponse.ok(data);
    }

    @GetMapping("/readiness")
    public ApiResponse<?> getReadiness(@CurrentUser Long userId) {
        requireAuth(userId);
        AccessInfo access = accessService.getAccess(userId);
        // Full readiness is a paid-tier feature (docs/parameters.md §3.1).
        if (!access.hasActivePass()) {
            throw new BusinessException("ACCESS_DENIED",
                    "Full readiness requires an active access pass",
                    HttpStatus.FORBIDDEN);
        }
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
