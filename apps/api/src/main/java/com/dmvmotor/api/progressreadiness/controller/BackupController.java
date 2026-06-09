package com.dmvmotor.api.progressreadiness.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.progressreadiness.application.ProgressBackupService;
import com.dmvmotor.api.progressreadiness.application.ProgressBackupService.RestoreOutcome;
import com.dmvmotor.api.progressreadiness.application.ProgressBackupService.SyncOutcome;
import com.dmvmotor.api.progressreadiness.infrastructure.ProgressBackupRepository.BackupRow;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single-slot progress backup (bug1), a paid cloud-save. Server-authoritative —
 * the snapshot is computed from the user's real progress, never an uploaded blob.
 *
 * <ul>
 *   <li>{@code POST /backup/sync} — recompute + persist the single slot (paid).
 *       Frontend calls this in the background after a session/mock; no-ops when
 *       nothing changed.</li>
 *   <li>{@code GET /backup/latest} — download the snapshot to rehydrate a
 *       cache-wiped or new-platform client (owner-only; allowed after downgrade).</li>
 *   <li>{@code POST /backup/restore} — re-apply the snapshot into the current
 *       cycle (paid + recent re-auth + throttled).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/backup")
public class BackupController {

    private final ProgressBackupService service;

    public BackupController(ProgressBackupService service) {
        this.service = service;
    }

    @PostMapping("/sync")
    public ApiResponse<?> sync(@CurrentUser Long userId) {
        requireAuth(userId);
        SyncOutcome out = service.sync(userId);
        return ApiResponse.ok(SyncDto.from(out));
    }

    @GetMapping("/latest")
    public ApiResponse<?> latest(@CurrentUser Long userId) {
        requireAuth(userId);
        return ApiResponse.ok(service.getLatest(userId)
                .map(BackupDto::from)
                .map(LatestDto::present)
                .orElseGet(LatestDto::absent));
    }

    @PostMapping("/restore")
    public ApiResponse<?> restore(@CurrentUser Long userId) {
        requireAuth(userId);
        RestoreOutcome out = service.restore(userId);
        return ApiResponse.ok(new RestoreDto(out.restoredMistakes(), BackupDto.from(out.row())));
    }

    private static void requireAuth(Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    record LatestDto(boolean hasBackup, BackupDto backup) {
        static LatestDto present(BackupDto b) { return new LatestDto(true, b); }
        static LatestDto absent()             { return new LatestDto(false, null); }
    }

    record SyncDto(boolean changed, BackupDto backup) {
        static SyncDto from(SyncOutcome o) { return new SyncDto(o.changed(), BackupDto.from(o.row())); }
    }

    record RestoreDto(int restoredMistakes, BackupDto backup) {}

    record BackupDto(
            String  id,
            int     readinessScore,
            int     completionScore,
            int     mockTotalAttempts,
            Integer mockBestScorePercent,
            Integer mockRecent3AvgPercent,
            int     practiceTotalSessions,
            int     practiceAccuracyPercent,
            int     activeMistakesCount,
            String  updatedAt
    ) {
        static BackupDto from(BackupRow r) {
            return new BackupDto(
                    String.valueOf(r.id()),
                    r.readinessScore(), r.completionScore(),
                    r.mockTotalAttempts(), r.mockBestScorePercent(), r.mockRecent3AvgPercent(),
                    r.practiceTotalSessions(), r.practiceAccuracyPercent(), r.activeMistakesCount(),
                    r.updatedAt().toString());
        }
    }
}
