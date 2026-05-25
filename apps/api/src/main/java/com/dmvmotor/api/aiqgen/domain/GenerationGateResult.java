package com.dmvmotor.api.aiqgen.domain;

/**
 * Outcome of a single gate inspecting a candidate question. Reason is always
 * populated (success says what was checked; failure explains why) so the
 * orchestrator's retry-with-feedback loop has something concrete to feed back
 * into the next generation prompt.
 */
public record GenerationGateResult(
        boolean passed,
        String reason
) {

    public static GenerationGateResult pass(String reason) {
        return new GenerationGateResult(true, reason);
    }

    public static GenerationGateResult fail(String reason) {
        return new GenerationGateResult(false, reason);
    }
}
