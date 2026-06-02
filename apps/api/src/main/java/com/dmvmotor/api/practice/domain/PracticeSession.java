package com.dmvmotor.api.practice.domain;

import java.time.OffsetDateTime;

import java.util.List;

public record PracticeSession(
        Long id,
        Long userId,
        String status,
        String entryType,
        String languageCode,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Long examId,
        List<Long> topicFilter
) {
    public boolean isInProgress() { return "in_progress".equals(status); }
    public boolean isCompleted()  { return "completed".equals(status); }
}
