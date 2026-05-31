package com.dmvmotor.api.reminder.domain;

/**
 * The kinds of reminder the backend can generate, in trigger-priority order
 * (docs/parameters.md §10 "reminder 触发优先顺序", remapped to the post-Phase-B
 * model where explicit review packs folded into personalized practice):
 *
 * <ol>
 *   <li>{@link #RESUME_PRACTICE} — an unfinished practice session to come back
 *       to (covers "今日复习包未完成" + "学习被中断需要回流").</li>
 *   <li>{@link #REVIEW_WEAK_POINTS} — active mistakes still need clearing
 *       ("关键薄弱点长期未补").</li>
 *   <li>{@link #START_MOCK} — studied and no open weak points, time to validate
 *       with a mock ("适合进入下一次 mock exam").</li>
 * </ol>
 */
public enum ReminderType {
    RESUME_PRACTICE("resume_practice", 1),
    REVIEW_WEAK_POINTS("review_weak_points", 2),
    START_MOCK("start_mock", 3);

    private final String code;
    private final int    priority;

    ReminderType(String code, int priority) {
        this.code     = code;
        this.priority = priority;
    }

    public String code()    { return code; }
    public int    priority() { return priority; }
}
