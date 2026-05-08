package com.dmvmotor.api.aisupport.domain;

import java.time.OffsetDateTime;

public record AiExplanation(
        Long           id,
        Long           userId,
        Long           questionId,
        String         language,
        String         selectedChoiceKey,
        String         explanation,
        String         model,
        Integer        tokensIn,
        Integer        tokensOut,
        OffsetDateTime createdAt
) {}
