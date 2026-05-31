package com.dmvmotor.api.reminder.application;

import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.ResourceNotFoundException;
import com.dmvmotor.api.mistakereview.infrastructure.MistakeListRepository;
import com.dmvmotor.api.practice.infrastructure.PracticeHistoryDao;
import com.dmvmotor.api.practice.infrastructure.PracticeSessionRepository;
import com.dmvmotor.api.reminder.domain.Reminder;
import com.dmvmotor.api.reminder.domain.ReminderType;
import com.dmvmotor.api.reminder.infrastructure.ReminderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generates and serves reminders from the user's live learning state
 * (docs/reminder-and-readiness.md). Reminders never run free of that state:
 * each generation picks the single most-worthwhile task, in trigger priority,
 * and respects the frequency rules — at most one new reminder per 24h, and a
 * type pauses once its last {@value #PAUSE_AFTER_UNRESPONDED} reminders went
 * unanswered (responding to any one un-pauses it).
 */
@Service
public class ReminderService {

    private static final int DAILY_WINDOW_HOURS     = 24;
    private static final int PAUSE_AFTER_UNRESPONDED = 3;

    private final ReminderRepository       repo;
    private final PracticeSessionRepository practiceRepo;
    private final MistakeListRepository    mistakeListRepo;
    private final PracticeHistoryDao       historyDao;
    private final UserRepository           userRepo;

    public ReminderService(ReminderRepository repo,
                           PracticeSessionRepository practiceRepo,
                           MistakeListRepository mistakeListRepo,
                           PracticeHistoryDao historyDao,
                           UserRepository userRepo) {
        this.repo            = repo;
        this.practiceRepo    = practiceRepo;
        this.mistakeListRepo = mistakeListRepo;
        this.historyDao      = historyDao;
        this.userRepo        = userRepo;
    }

    /**
     * Emit at most one reminder for the user, or none if the daily cap is spent
     * or no scenario applies (or every applicable type is paused).
     */
    @Transactional
    public Optional<Reminder> generate(Long userId) {
        // Daily cap — one new reminder per 24h, no overflow.
        if (repo.existsCreatedSince(userId,
                OffsetDateTime.now().minusHours(DAILY_WINDOW_HOURS))) {
            return Optional.empty();
        }

        int cycle = userRepo.findById(userId)
                .map(UserRepository.UserRow::resetCount).orElse(0);

        // First applicable, non-paused type wins (priority order).
        for (ReminderType type : applicableTypes(userId, cycle)) {
            if (!isPaused(userId, type)) {
                Long id = repo.insert(userId, type.code(), type.priority());
                return repo.findById(id);
            }
        }
        return Optional.empty();
    }

    /** Scenarios that currently apply, already in priority order. */
    private List<ReminderType> applicableTypes(Long userId, int cycle) {
        List<ReminderType> out = new ArrayList<>();
        boolean inProgress    = practiceRepo.existsInProgressByUserId(userId, cycle);
        boolean hasWeakPoints = mistakeListRepo.countActive(userId, cycle) > 0;

        // 1 — an unfinished session to resume (复习包未完成 / 学习被中断).
        if (inProgress) out.add(ReminderType.RESUME_PRACTICE);
        // 2 — active mistakes still need clearing (关键薄弱点未补).
        if (hasWeakPoints) out.add(ReminderType.REVIEW_WEAK_POINTS);
        // 3 — studied with nothing open → validate with a mock (适合下一次 mock).
        if (!inProgress && !hasWeakPoints && historyDao.countByUser(userId) > 0) {
            out.add(ReminderType.START_MOCK);
        }
        return out;
    }

    /** A type is paused once its last N reminders are all still unresponded. */
    private boolean isPaused(Long userId, ReminderType type) {
        List<String> recent =
                repo.recentStatusesByType(userId, type.code(), PAUSE_AFTER_UNRESPONDED);
        return recent.size() >= PAUSE_AFTER_UNRESPONDED
                && recent.stream().allMatch("pending"::equals);
    }

    public List<Reminder> list(Long userId) {
        return repo.findActiveByUser(userId);
    }

    @Transactional
    public void respond(Long userId, Long reminderId) {
        Reminder r = repo.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reminder not found: " + reminderId));
        if (!r.userId().equals(userId)) {
            throw new BusinessException("FORBIDDEN",
                    "Reminder belongs to a different user", HttpStatus.FORBIDDEN);
        }
        repo.markResponded(reminderId);  // no-op if already responded (idempotent)
    }
}
