package com.dmvmotor.api.progressreadiness.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.progressreadiness.application.EngagementService;
import com.dmvmotor.api.progressreadiness.application.EngagementService.Engagement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Engagement strip on the Study Hub — streak + daily goal (D1). Personal, so
 * authed only. The client passes {@code tz_offset_minutes} (minutes to add to
 * UTC) so day boundaries match the user's clock, not the server's UTC midnight.
 */
@RestController
@RequestMapping("/api/v1")
public class EngagementController {

    private final EngagementService engagementService;

    public EngagementController(EngagementService engagementService) {
        this.engagementService = engagementService;
    }

    @GetMapping("/engagement")
    public ApiResponse<?> getEngagement(
            @CurrentUser Long userId,
            @RequestParam(name = "tz_offset_minutes", required = false, defaultValue = "0")
            int tzOffsetMinutes) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
        Engagement e = engagementService.getEngagement(userId, tzOffsetMinutes);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("current_streak_days", e.currentStreakDays());
        data.put("answered_today", e.answeredToday());
        data.put("daily_goal", e.dailyGoal());
        return ApiResponse.ok(data);
    }
}
