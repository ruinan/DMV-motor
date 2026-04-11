package com.dmvmotor.api.content.domain;

public record Topic(
        Long id,
        Long parentTopicId,
        String code,
        String nameEn,
        String nameZh,
        boolean isKeyTopic,
        String riskLevel,
        int sortOrder
) {}
