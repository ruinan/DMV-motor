package com.dmvmotor.api.progressreadiness.application;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.content.application.ExamContext;
import com.dmvmotor.api.mockexam.application.MockExamService;
import com.dmvmotor.api.mockexam.infrastructure.MockHistoryDao.AttemptStats;
import com.dmvmotor.api.practice.application.PracticeService;
import com.dmvmotor.api.practice.application.PracticeService.PracticeStats;
import com.dmvmotor.api.progressreadiness.application.SummaryService.SummaryResult;
import com.dmvmotor.api.progressreadiness.infrastructure.ProgressSnapshotRepository;
import com.dmvmotor.api.progressreadiness.infrastructure.ProgressSnapshotRepository.SnapshotRow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Paid "remote backup" of a learner's progress: captures a point-in-time
 * snapshot of the headline readiness/completion scores plus the recent-mock and
 * practice summaries (近3模考 / 近N练习 / 总进度评分) for the user's CURRENT exam,
 * and lists past snapshots. Reuses the existing exam-scoped stats services, so a
 * snapshot reflects exactly what the dashboard shows.
 *
 * <p>Gating: creating a snapshot requires an active pass (it's a paid perk);
 * VIEWING past snapshots is always allowed so a downgraded user keeps the
 * history they paid to record.
 */
@Service
public class ProgressSnapshotService {

    private static final int LIST_LIMIT = 20;

    private final ProgressSnapshotRepository repo;
    private final ExamContext                examContext;
    private final AccessService              accessService;
    private final SummaryService             summaryService;
    private final PracticeService            practiceService;
    private final MockExamService            mockExamService;

    public ProgressSnapshotService(ProgressSnapshotRepository repo,
                                   ExamContext examContext,
                                   AccessService accessService,
                                   SummaryService summaryService,
                                   PracticeService practiceService,
                                   MockExamService mockExamService) {
        this.repo            = repo;
        this.examContext     = examContext;
        this.accessService   = accessService;
        this.summaryService  = summaryService;
        this.practiceService = practiceService;
        this.mockExamService = mockExamService;
    }

    /** Captures a snapshot for the user's current exam. Requires an active pass. */
    public SnapshotRow create(Long userId) {
        Long examId = examContext.resolveExamId(userId);
        if (!accessService.getAccess(userId, examId).hasActivePass()) {
            throw new BusinessException("ACCESS_DENIED",
                    "An active pass is required to back up your progress",
                    HttpStatus.FORBIDDEN);
        }
        SummaryResult  summary  = summaryService.getSummary(userId);
        PracticeStats  practice = practiceService.getStats(userId);
        AttemptStats   mock     = mockExamService.getStats(userId);

        return repo.insert(userId, examId,
                summary.readinessScore(), summary.completionScore(),
                mock.totalAttempts(), mock.bestScorePercent(), mock.recent3AvgScorePercent(),
                practice.totalSessions(), practice.overallAccuracyPercent(),
                practice.activeMistakesCount());
    }

    /** Past snapshots for the user's current exam, newest first. Not gated. */
    public List<SnapshotRow> list(Long userId) {
        return repo.findRecent(userId, examContext.resolveExamId(userId), LIST_LIMIT);
    }
}
