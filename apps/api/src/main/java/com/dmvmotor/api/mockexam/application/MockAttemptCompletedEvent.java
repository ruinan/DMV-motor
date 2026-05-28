package com.dmvmotor.api.mockexam.application;

/**
 * Published when a mock attempt reaches a terminal state (submitted,
 * ended_by_failure, or ended_by_exit). Drives the automatic, async generation
 * of an AI review plan — the user neither triggers nor waits for it.
 */
public record MockAttemptCompletedEvent(Long attemptId, Long userId) {}
