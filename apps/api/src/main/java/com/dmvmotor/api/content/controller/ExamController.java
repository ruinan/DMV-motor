package com.dmvmotor.api.content.controller;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.content.domain.Exam;
import com.dmvmotor.api.content.infrastructure.ExamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exam catalog — the (state × license type) options a learner can prepare for.
 * Public (no auth): the picker needs it before/at sign-in. Today it returns the
 * single seeded CA-M1 exam; more are added as content is sourced.
 */
@RestController
@RequestMapping("/api/v1/exams")
public class ExamController {

    private final ExamRepository examRepo;
    private final AccessService  accessService;

    public ExamController(ExamRepository examRepo, AccessService accessService) {
        this.examRepo      = examRepo;
        this.accessService = accessService;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String language) {
        boolean zh = "zh".equalsIgnoreCase(language);
        List<ExamDto> exams = examRepo.findAllActive().stream()
                .map(e -> ExamDto.from(e, zh)).toList();
        return ApiResponse.ok(new ExamListDto(exams));
    }

    /**
     * Per-exam subscription state for the signed-in user — drives the settings
     * catalog (Subscribe / Unsubscribe per exam). One entry per active exam;
     * {@code subscribed} is true when the user holds an active pass that unlocks
     * that exam (a per-exam pass, or a legacy/dev global pass). Authed only — the
     * anonymous catalog uses {@link #list} (everything reads as not-subscribed).
     */
    @GetMapping("/entitlements")
    public ApiResponse<?> entitlements(@CurrentUser Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED",
                    "Authentication required", HttpStatus.UNAUTHORIZED);
        }
        List<EntitlementDto> entitlements = examRepo.findAllActive().stream()
                .map(e -> new EntitlementDto(
                        String.valueOf(e.id()),
                        accessService.getAccess(userId, e.id()).hasActivePass()))
                .toList();
        return ApiResponse.ok(new EntitlementListDto(entitlements));
    }

    record ExamListDto(List<ExamDto> exams) {}

    record ExamDto(String id, String stateCode, String licenseClass, String name) {
        static ExamDto from(Exam e, boolean zh) {
            return new ExamDto(String.valueOf(e.id()), e.stateCode(), e.licenseClass(),
                    zh ? e.nameZh() : e.nameEn());
        }
    }

    record EntitlementListDto(List<EntitlementDto> entitlements) {}

    record EntitlementDto(String examId, boolean subscribed) {}
}
