package com.dmvmotor.api.authaccess.application;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** CSPRNG activation-code generation — format, alphabet, and uniqueness. */
class RedemptionCodeGeneratorTest {

    private static final Pattern FORMAT =
            Pattern.compile("^DMVPREP(-[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}){3}$");

    private final RedemptionCodeGenerator generator = new RedemptionCodeGenerator();

    @Test
    void generate_matchesPrefixedGroupedFormat() {
        assertThat(generator.generate()).matches(FORMAT);
    }

    @Test
    void generate_neverUsesAmbiguousCharacters() {
        for (int i = 0; i < 500; i++) {
            String random = generator.generate().substring("DMVPREP".length()).replace("-", "");
            // 0/O, 1/I, L are excluded to avoid mis-typing the code.
            assertThat(random).doesNotContain("0", "O", "1", "I", "L");
        }
    }

    @Test
    void generate_producesUniqueCodes() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 5_000; i++) {
            seen.add(generator.generate());
        }
        // 31^12 entropy → collisions across 5k draws are astronomically unlikely.
        assertThat(seen).hasSize(5_000);
    }
}
