package com.dmvmotor.api.mistakereview.domain;

import java.time.OffsetDateTime;

public record MistakeRecord(
        Long           id,
        Long           questionId,
        Long           topicId,
        int            wrongCount,
        OffsetDateTime lastWrongAt,
        String         lastEntrySource
) {}
