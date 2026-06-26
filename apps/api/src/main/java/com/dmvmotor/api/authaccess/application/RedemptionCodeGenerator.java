package com.dmvmotor.api.authaccess.application;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates activation / redemption codes from a CSPRNG so they can't be guessed
 * — replacing the hand-typed founder codes (which had little entropy). Codes look
 * like {@code DMVPREP-XXXX-XXXX-XXXX}: a fixed prefix plus three groups of four
 * characters drawn from an unambiguous alphabet (no 0/O/1/I/L) to avoid
 * transcription mistakes when a user types the code.
 *
 * <p>Entropy is {@code 31^12 ≈ 2^59}, which — together with the per-user redeem
 * rate limit — makes brute-forcing a valid code infeasible.
 */
@Component
public class RedemptionCodeGenerator {

    /** Crockford-style alphabet minus the easily-confused 0/O/1/I/L → 31 chars. */
    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final String PREFIX   = "DMVPREP";
    private static final int GROUPS    = 3;
    private static final int GROUP_LEN = 4;

    private final SecureRandom random = new SecureRandom();

    /** A fresh, unguessable code. Callers must persist it with collision retry
     *  (the unique index on {@code UPPER(code)} is the real guard). */
    public String generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int g = 0; g < GROUPS; g++) {
            sb.append('-');
            for (int i = 0; i < GROUP_LEN; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
        }
        return sb.toString();
    }
}
