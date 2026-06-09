package com.dmvmotor.api.progressreadiness.application;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.ReauthGuard;
import com.dmvmotor.api.content.application.ExamContext;
import com.dmvmotor.api.mockexam.application.MockExamService;
import com.dmvmotor.api.mockexam.infrastructure.MockHistoryDao.AttemptStats;
import com.dmvmotor.api.mistakereview.domain.MistakeRecord;
import com.dmvmotor.api.mistakereview.infrastructure.MistakeListRepository;
import com.dmvmotor.api.mistakereview.infrastructure.MistakeRepository;
import com.dmvmotor.api.practice.application.PracticeService;
import com.dmvmotor.api.practice.application.PracticeService.PracticeStats;
import com.dmvmotor.api.progressreadiness.application.SummaryService.SummaryResult;
import com.dmvmotor.api.progressreadiness.infrastructure.ProgressBackupRepository;
import com.dmvmotor.api.progressreadiness.infrastructure.ProgressBackupRepository.BackupRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Single-slot, restorable progress backup (bug1) — a paid "cloud-save".
 *
 * <p><b>Source of truth = the server.</b> All progress is recorded server-side
 * via authenticated APIs; this snapshot is SERVER-COMPUTED from that
 * authoritative data, never an unvalidated client upload, so nothing about a
 * user's progress can be forged through the backup/restore service. The client
 * (web now, mini-program / app later) is just a cache that DOWNLOADS this
 * snapshot to rehydrate after its local data is deleted or on a new platform.
 *
 * <p>Security posture:
 * <ul>
 *   <li>{@link #sync} (write) requires an active pass — it's a paid perk.</li>
 *   <li>{@link #getLatest} (read) is owner-only, allowed even after a downgrade
 *       so a paid-then-lapsed user keeps the data they recorded.</li>
 *   <li>{@link #restore} (mutates progress) requires an active pass AND a recent
 *       re-auth AND is throttled — it's the only state-changing path and the
 *       most abuse-prone.</li>
 *   <li>All three resolve the user from the auth token, never a request param,
 *       so one user can't touch another's backup.</li>
 * </ul>
 */
@Service
public class ProgressBackupService {

    /** Bump if the payload shape changes so a restore can branch on it. */
    private static final int PAYLOAD_VERSION = 1;
    /** Cap the mistake list we snapshot (defensive — far above any real bank). */
    private static final int MAX_MISTAKES = 2000;
    /** Minimum gap between restores (defense-in-depth on top of reauth). */
    private static final long RESTORE_COOLDOWN_SECONDS = 60;

    private final ProgressBackupRepository repo;
    private final ExamContext              examContext;
    private final AccessService            accessService;
    private final ReauthGuard              reauthGuard;
    private final SummaryService           summaryService;
    private final PracticeService          practiceService;
    private final MockExamService          mockExamService;
    private final MistakeListRepository    mistakeListRepo;
    private final MistakeRepository        mistakeRepo;
    private final UserRepository           userRepo;
    private final ObjectMapper             objectMapper;

    public ProgressBackupService(ProgressBackupRepository repo,
                                 ExamContext examContext,
                                 AccessService accessService,
                                 ReauthGuard reauthGuard,
                                 SummaryService summaryService,
                                 PracticeService practiceService,
                                 MockExamService mockExamService,
                                 MistakeListRepository mistakeListRepo,
                                 MistakeRepository mistakeRepo,
                                 UserRepository userRepo,
                                 ObjectMapper objectMapper) {
        this.repo            = repo;
        this.examContext     = examContext;
        this.accessService   = accessService;
        this.reauthGuard     = reauthGuard;
        this.summaryService  = summaryService;
        this.practiceService = practiceService;
        this.mockExamService = mockExamService;
        this.mistakeListRepo = mistakeListRepo;
        this.mistakeRepo     = mistakeRepo;
        this.userRepo        = userRepo;
        this.objectMapper    = objectMapper;
    }

    /**
     * Computes the current snapshot from authoritative data and persists it to
     * the single slot. Incremental: if the content hash matches the stored slot,
     * it's a no-op (the row, including updated_at, is left untouched) so frequent
     * auto-sync calls don't hammer the DB. Requires an active pass.
     */
    public SyncOutcome sync(Long userId) {
        Long examId = examContext.resolveExamId(userId);
        requirePass(userId, examId);
        int cycle = cycleFor(userId);

        SummaryResult summary  = summaryService.getSummary(userId);
        PracticeStats practice = practiceService.getStats(userId);
        AttemptStats  mock     = mockExamService.getStats(userId);
        List<MistakeRecord> mistakes = mistakeListRepo.findActiveMistakes(
                userId, examId, null, 1, MAX_MISTAKES, cycle);

        List<MistakeEntry> entries = mistakes.stream()
                .map(m -> new MistakeEntry(m.questionId(), m.topicId(), m.wrongCount(), m.lastEntrySource()))
                .toList();
        BackupPayload payload = new BackupPayload(PAYLOAD_VERSION, cycle,
                new SummarySnapshot(summary.readinessScore(), summary.completionScore(),
                        mock.totalAttempts(), mock.bestScorePercent(), mock.recent3AvgScorePercent(),
                        practice.totalSessions(), practice.overallAccuracyPercent(),
                        practice.activeMistakesCount()),
                entries);

        String json = writeJson(payload);
        String hash = sha256(json);

        Optional<BackupRow> existing = repo.find(userId, examId);
        if (existing.isPresent() && hash.equals(existing.get().contentHash())) {
            return new SyncOutcome(existing.get(), false); // unchanged → no write
        }

        BackupRow row = repo.upsert(userId, examId, cycle,
                summary.readinessScore(), summary.completionScore(),
                mock.totalAttempts(), mock.bestScorePercent(), mock.recent3AvgScorePercent(),
                practice.totalSessions(), practice.overallAccuracyPercent(),
                practice.activeMistakesCount(), json, hash);
        return new SyncOutcome(row, true);
    }

    /** The user's latest backup for their current exam (owner-only read). */
    public Optional<BackupRow> getLatest(Long userId) {
        return repo.find(userId, examContext.resolveExamId(userId));
    }

    /**
     * Re-applies the backed-up active mistakes into the user's CURRENT learning
     * cycle (idempotent upsert — safe to re-run). This is what recovers a user's
     * weak-points after a learning-cycle reset / on a cache-wiped client.
     * Requires an active pass, a recent re-auth, and is throttled.
     */
    public RestoreOutcome restore(Long userId) {
        Long examId = examContext.resolveExamId(userId);
        requirePass(userId, examId);
        reauthGuard.requireRecentReauth(); // mutates progress → recent password proof

        BackupRow row = repo.find(userId, examId)
                .orElseThrow(() -> new BusinessException("NO_BACKUP",
                        "No backup to restore for this exam", HttpStatus.NOT_FOUND));

        if (row.restoredAt() != null
                && row.restoredAt().isAfter(OffsetDateTime.now().minusSeconds(RESTORE_COOLDOWN_SECONDS))) {
            throw new BusinessException("RESTORE_THROTTLED",
                    "A restore was just performed — please wait a moment before retrying",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        BackupPayload payload = readJson(row.payloadJson());
        int cycle = cycleFor(userId);
        int restored = 0;
        for (MistakeEntry m : payload.activeMistakes()) {
            mistakeRepo.upsertMistake(userId, m.questionId(), m.topicId(), m.source(), cycle);
            restored++;
        }
        repo.markRestored(userId, examId);
        return new RestoreOutcome(restored, row);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void requirePass(Long userId, Long examId) {
        if (!accessService.getAccess(userId, examId).hasActivePass()) {
            throw new BusinessException("ACCESS_DENIED",
                    "An active pass is required to back up or restore your progress",
                    HttpStatus.FORBIDDEN);
        }
    }

    private int cycleFor(Long userId) {
        return userRepo.findById(userId).map(UserRepository.UserRow::resetCount).orElse(0);
    }

    private String writeJson(BackupPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException("BACKUP_SERIALIZE_ERROR",
                    "Could not serialize the backup", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private BackupPayload readJson(String json) {
        try {
            return objectMapper.readValue(json, BackupPayload.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("BACKUP_CORRUPT",
                    "The backup could not be read", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    // ------------------------------------------------------------------
    // payload + result types
    // ------------------------------------------------------------------

    public record SyncOutcome(BackupRow row, boolean changed) {}
    public record RestoreOutcome(int restoredMistakes, BackupRow row) {}

    record BackupPayload(int version, int learningCycle, SummarySnapshot summary,
                         List<MistakeEntry> activeMistakes) {}
    record SummarySnapshot(int readiness, int completion, int mockTotal,
                           Integer mockBest, Integer mockAvg, int practiceSessions,
                           int practiceAccuracy, int activeMistakesCount) {}
    record MistakeEntry(long questionId, long topicId, int wrongCount, String source) {}
}
