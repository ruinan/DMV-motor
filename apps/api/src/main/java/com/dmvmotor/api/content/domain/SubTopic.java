package com.dmvmotor.api.content.domain;

public record SubTopic(
        Long id,
        Long parentTopicId,
        String code,
        String nameEn,
        String nameZh,
        String description,
        int sortOrder
) {}
