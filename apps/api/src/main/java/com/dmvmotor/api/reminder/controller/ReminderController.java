package com.dmvmotor.api.reminder.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.reminder.application.ReminderService;
import com.dmvmotor.api.reminder.domain.Reminder;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reminder API (docs/api.md §11). Backend decides what to remind; the client
 * surfaces the in-app list and reports back when the user acts.
 *
 * <ul>
 *   <li>{@code GET /api/v1/reminders} — the user's active in-app reminders.</li>
 *   <li>{@code POST /api/v1/reminders/generate} — (re)evaluate learning state and
 *       emit at most one reminder; idempotent within the daily cap.</li>
 *   <li>{@code POST /api/v1/reminders/{id}/respond} — mark a reminder handled.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

    private final ReminderService service;

    public ReminderController(ReminderService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<?> list(@CurrentUser Long userId) {
        requireAuth(userId);
        List<ReminderDto> items = service.list(userId).stream().map(ReminderDto::from).toList();
        return ApiResponse.ok(new ReminderListDto(items));
    }

    @PostMapping("/generate")
    public ApiResponse<?> generate(@CurrentUser Long userId) {
        requireAuth(userId);
        ReminderDto generated = service.generate(userId).map(ReminderDto::from).orElse(null);
        return ApiResponse.ok(new GenerateDto(generated != null, generated));
    }

    @PostMapping("/{id}/respond")
    public ApiResponse<?> respond(@CurrentUser Long userId, @PathVariable Long id) {
        requireAuth(userId);
        service.respond(userId, id);
        return ApiResponse.ok(new RespondDto("responded"));
    }

    private void requireAuth(Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    // ---- response DTOs ----

    record ReminderListDto(List<ReminderDto> reminders) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerateDto(boolean generated, ReminderDto reminder) {}

    record RespondDto(String status) {}

    record ReminderDto(String id, String type, int priority, String status, String createdAt) {
        static ReminderDto from(Reminder r) {
            return new ReminderDto(String.valueOf(r.id()), r.type(), r.priority(),
                    r.status(), r.createdAt().toString());
        }
    }
}
