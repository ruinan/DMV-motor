package com.dmvmotor.api.reminder.domain;

import java.time.OffsetDateTime;

/** A persisted reminder task. */
public record Reminder(
        Long           id,
        Long           userId,
        String         type,
        String         status,
        int            priority,
        OffsetDateTime createdAt,
        OffsetDateTime respondedAt
) {}
