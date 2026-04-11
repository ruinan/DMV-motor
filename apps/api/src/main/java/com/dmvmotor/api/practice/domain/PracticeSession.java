package com.dmvmotor.api.practice.domain;

import java.time.OffsetDateTime;

public record PracticeSession(
        Long id,
        Long userId,
        String status,
        String entryType,
        String languageCode,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {
    public boolean isInProgress() { return "in_progress".equals(status); }
    public boolean isCompleted()  { return "completed".equals(status); }
}
