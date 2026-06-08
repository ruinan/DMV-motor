package com.dmvmotor.api.progressreadiness.application;

import com.dmvmotor.api.progressreadiness.infrastructure.EngagementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

/**
 * Engagement signals for the Study Hub: the current practice streak and how
 * many questions the user has answered today against a daily goal. Cheap
 * arithmetic over indexed practice_attempts (no AI / paid API) — see
 * {@link EngagementRepository} for the local-day bucketing.
 */
@Service
public class EngagementService {

    private final EngagementRepository repo;
    private final int dailyGoal;

    public EngagementService(EngagementRepository repo,
                             @Value("${app.engagement.daily-goal:10}") int dailyGoal) {
        this.repo = repo;
        this.dailyGoal = dailyGoal;
    }

    public Engagement getEngagement(Long userId, int offsetMinutes) {
        // Clamp to the real-world tz range (−12:00 … +14:00) so a bad / abusive
        // param can't shift "today" to an arbitrary day.
        int off = Math.max(-12 * 60, Math.min(14 * 60, offsetMinutes));
        Map<LocalDate, Integer> byDay = repo.answeredCountByLocalDay(userId, off);
        LocalDate today = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(off).toLocalDate();
        int answeredToday = byDay.getOrDefault(today, 0);
        int streak = computeStreak(byDay.keySet(), today);
        return new Engagement(streak, answeredToday, dailyGoal);
    }

    /**
     * Consecutive days of activity ending today — with a one-day grace so a
     * streak shown in the morning, before today's practice, still reflects the
     * run you're continuing and only resets once you actually miss a day.
     * Returns 0 when the most recent activity is older than yesterday (broken)
     * or there's none.
     */
    static int computeStreak(Set<LocalDate> activeDays, LocalDate today) {
        if (activeDays.isEmpty()) return 0;
        LocalDate cursor;
        if (activeDays.contains(today)) {
            cursor = today;
        } else if (activeDays.contains(today.minusDays(1))) {
            cursor = today.minusDays(1);
        } else {
            return 0;
        }
        int streak = 0;
        while (activeDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    public record Engagement(int currentStreakDays, int answeredToday, int dailyGoal) {}
}
