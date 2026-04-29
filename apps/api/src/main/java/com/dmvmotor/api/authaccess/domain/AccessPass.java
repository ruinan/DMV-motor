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
    /**
     * Active iff the row is marked active AND the current time falls in [startsAt, expiresAt).
     * The status flag alone is not enough — there's no background job flipping expired rows,
     * so a pass whose window has elapsed must still be treated as inactive for paywall checks.
     */
    public boolean isActive(OffsetDateTime now) {
        if (!"active".equals(status)) return false;
        if (startsAt != null && now.isBefore(startsAt)) return false;
        if (expiresAt != null && !now.isBefore(expiresAt)) return false;
        return true;
    }

    public int mockRemaining() { return mockExamTotalCount - mockExamUsedCount; }
}
