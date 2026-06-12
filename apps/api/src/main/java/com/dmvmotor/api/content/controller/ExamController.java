package com.dmvmotor.api.content.controller;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.authaccess.infrastructure.ExamUnlockRepository;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.content.domain.Exam;
import com.dmvmotor.api.content.infrastructure.ExamRepository;
import com.dmvmotor.api.practice.infrastructure.PracticeSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

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
    private final PracticeSessionRepository practiceSessions;
    private final ExamUnlockRepository examUnlocks;

    public ExamController(ExamRepository examRepo, AccessService accessService,
                          PracticeSessionRepository practiceSessions,
                          ExamUnlockRepository examUnlocks) {
        this.examRepo         = examRepo;
        this.accessService    = accessService;
        this.practiceSessions = practiceSessions;
        this.examUnlocks      = examUnlocks;
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
        // "Opened" exams read as Free even without a pass, so the switcher shows
        // them unlocked. An exam counts as opened if the user explicitly opened
        // it (tapped Free trial → exam_free_unlocks) OR has any practice activity
        // for it. Paid stays authoritative via getAccess; this only softens the
        // locked-vs-free UX gate.
        Set<Long> opened = practiceSessions.examIdsWithActivity(userId);
        opened.addAll(examUnlocks.freeUnlockedExamIds(userId));
        List<EntitlementDto> entitlements = examRepo.findAllActive().stream()
                .map(e -> new EntitlementDto(
                        String.valueOf(e.id()),
                        accessService.getAccess(userId, e.id()).hasActivePass(),
                        opened.contains(e.id())))
                .toList();
        return ApiResponse.ok(new EntitlementListDto(entitlements));
    }

    /**
     * Free-open an exam — the "Free trial" path: persistently marks the exam as
     * opened (Free) for the user so it stays unlocked in the switcher even before
     * they practice. Idempotent. Grants nothing beyond the free tier (free-trial
     * practice is already open to everyone); paid access is unaffected.
     */
    @PostMapping("/{examId}/open-free")
    public ApiResponse<?> openFree(@PathVariable Long examId, @CurrentUser Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED",
                    "Authentication required", HttpStatus.UNAUTHORIZED);
        }
        boolean active = examRepo.findAllActive().stream()
                .anyMatch(e -> e.id().equals(examId));
        if (!active) {
            throw new BusinessException("INVALID_EXAM",
                    "Unknown or inactive exam.", HttpStatus.BAD_REQUEST);
        }
        examUnlocks.openFree(userId, examId);
        return ApiResponse.ok(new OpenFreeDto(String.valueOf(examId), true));
    }

    record OpenFreeDto(String examId, boolean opened) {}

    record ExamListDto(List<ExamDto> exams) {}

    record ExamDto(String id, String stateCode, String licenseClass, String name) {
        static ExamDto from(Exam e, boolean zh) {
            return new ExamDto(String.valueOf(e.id()), e.stateCode(), e.licenseClass(),
                    zh ? e.nameZh() : e.nameEn());
        }
    }

    record EntitlementListDto(List<EntitlementDto> entitlements) {}

    record EntitlementDto(String examId, boolean subscribed, boolean opened) {}
}
