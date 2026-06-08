package com.dmvmotor.api.progressreadiness.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.progressreadiness.application.ProgressSnapshotService;
import com.dmvmotor.api.progressreadiness.infrastructure.ProgressSnapshotRepository.SnapshotRow;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Progress backup (paid perk). Snapshots a learner's progress for their current
 * exam so it's durable + restorable across a learning-cycle reset or a new
 * device. Creating requires an active pass (enforced in the service); listing is
 * open to any authed user so a downgraded account keeps its recorded history.
 */
@RestController
@RequestMapping("/api/v1/backup")
public class BackupController {

    private final ProgressSnapshotService service;

    public BackupController(ProgressSnapshotService service) {
        this.service = service;
    }

    @PostMapping("/snapshots")
    public ApiResponse<?> create(@CurrentUser Long userId) {
        requireAuth(userId);
        return ApiResponse.ok(SnapshotDto.from(service.create(userId)));
    }

    @GetMapping("/snapshots")
    public ApiResponse<?> list(@CurrentUser Long userId) {
        requireAuth(userId);
        List<SnapshotDto> items = service.list(userId).stream().map(SnapshotDto::from).toList();
        return ApiResponse.ok(new SnapshotListDto(items));
    }

    private static void requireAuth(Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    record SnapshotListDto(List<SnapshotDto> snapshots) {}

    record SnapshotDto(
            String  id,
            int     readinessScore,
            int     completionScore,
            int     mockTotalAttempts,
            Integer mockBestScorePercent,
            Integer mockRecent3AvgPercent,
            int     practiceTotalSessions,
            int     practiceAccuracyPercent,
            int     activeMistakesCount,
            String  createdAt
    ) {
        static SnapshotDto from(SnapshotRow r) {
            return new SnapshotDto(
                    String.valueOf(r.id()),
                    r.readinessScore(), r.completionScore(),
                    r.mockTotalAttempts(), r.mockBestScorePercent(), r.mockRecent3AvgPercent(),
                    r.practiceTotalSessions(), r.practiceAccuracyPercent(), r.activeMistakesCount(),
                    r.createdAt().toString());
        }
    }
}
