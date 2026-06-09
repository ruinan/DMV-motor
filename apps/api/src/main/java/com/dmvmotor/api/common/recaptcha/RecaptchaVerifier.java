package com.dmvmotor.api.common.recaptcha;

/**
 * Verifies a reCAPTCHA token (assesses how human-like the action was). The real
 * implementation talks to reCAPTCHA Enterprise; tests use a fake. Kept behind an
 * interface so the GCP call is isolated (and excluded from coverage), mirroring
 * the Stripe gateway seam.
 */
public interface RecaptchaVerifier {

    /**
     * @param token          the client-side reCAPTCHA token
     * @param expectedAction the action name the client claimed (e.g. "login")
     * @return the assessment — valid token + risk score
     */
    Assessment verify(String token, String expectedAction);

    /** {@code valid} = token well-formed for this site/action; {@code score} 0..1. */
    record Assessment(boolean valid, double score, String reason) {}
}
