package com.dmvmotor.api.authaccess.domain;

import java.time.OffsetDateTime;

public record AccessPass(
        Long   id,
        Long   userId,
        String status,
        OffsetDateTime startsAt,
        OffsetDateTime expiresAt,
        int    mockExamTotalCount,
        int    mockExamUsedCount
) {
    public boolean isActive() { return "active".equals(status); }
    public int mockRemaining() { return mockExamTotalCount - mockExamUsedCount; }
}
