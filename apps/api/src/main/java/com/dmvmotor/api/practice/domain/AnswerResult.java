package com.dmvmotor.api.practice.domain;

public record AnswerResult(
        Long questionId,
        boolean isCorrect,
        String correctChoiceKey,
        String explanation,
        int answeredCount
) {}
