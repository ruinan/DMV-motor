package com.dmvmotor.api.content.domain;

/** An exam = a (state, license class) the app prepares for, with its own pass standard. */
public record Exam(
        Long   id,
        String stateCode,
        String licenseClass,
        String nameEn,
        String nameZh,
        int    passThresholdPercent,
        String status,
        int    sortOrder
) {}
