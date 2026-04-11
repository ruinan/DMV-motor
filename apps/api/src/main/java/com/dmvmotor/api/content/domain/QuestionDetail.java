package com.dmvmotor.api.content.domain;

import java.util.List;

public record QuestionDetail(
        Long questionId,
        Long topicId,
        String correctChoiceKey,
        String language,
        String stem,
        List<Choice> choices,
        String explanation
) {}
