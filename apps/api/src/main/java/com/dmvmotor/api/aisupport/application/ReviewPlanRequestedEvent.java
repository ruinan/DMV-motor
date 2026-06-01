package com.dmvmotor.api.aisupport.application;

/**
 * Published when a review plan is requested in a language that isn't cached yet
 * (e.g. the user switched language). Handled asynchronously so the GET doesn't
 * block on the LLM — the client keeps polling until the plan is ready.
 */
public record ReviewPlanRequestedEvent(Long attemptId, Long userId, String language) {}
