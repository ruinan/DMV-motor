package com.dmvmotor.api.aiqgen.application;

import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion;
import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion.Choice;
import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion.Variant;
import com.dmvmotor.api.aiqgen.domain.GenerationGateResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FormatValidatorTest {

    private final FormatValidator validator = new FormatValidator();

    @Test
    void happyPath_passesEveryRule() {
        GeneratedQuestion q = canonicalQuestion();
        assertThat(validator.check(q).passed()).isTrue();
    }

    @Test
    void invalidCorrectKey_fails() {
        GeneratedQuestion q = new GeneratedQuestion(
                "TEST_ST", "E", canonicalVariantEn(), canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("A/B/C/D");
    }

    @Test
    void missingEnVariant_fails() {
        GeneratedQuestion q = new GeneratedQuestion(
                "TEST_ST", "A", null, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("en variant missing");
    }

    @Test
    void missingZhVariant_fails() {
        GeneratedQuestion q = new GeneratedQuestion(
                "TEST_ST", "A", canonicalVariantEn(), null);
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("zh variant missing");
    }

    @Test
    void threeChoicesOnly_fails() {
        Variant v = new Variant("Stem", List.of(
                new Choice("A", "alpha"),
                new Choice("B", "beta"),
                new Choice("C", "gamma")
        ), validExplanation());
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("exactly 4 choices");
    }

    @Test
    void duplicateChoiceText_fails() {
        Variant v = new Variant("Stem", List.of(
                new Choice("A", "Same answer"),
                new Choice("B", "same answer  "),
                new Choice("C", "gamma"),
                new Choice("D", "delta")
        ), validExplanation());
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("duplicate choice text");
    }

    @Test
    void duplicateChoiceKey_fails() {
        Variant v = new Variant("Stem", List.of(
                new Choice("A", "alpha"),
                new Choice("A", "beta"),
                new Choice("C", "gamma"),
                new Choice("D", "delta")
        ), validExplanation());
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("duplicate choice key");
    }

    @Test
    void blankStem_fails() {
        Variant v = new Variant("   ", canonicalChoices(), validExplanation());
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("stem blank");
    }

    @Test
    void explanationTooShort_fails() {
        Variant v = new Variant("Stem", canonicalChoices(), "tiny.");
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("explanation length");
    }

    @Test
    void explanationTooLong_fails() {
        String tooLong = "x".repeat(501);
        Variant v = new Variant("Stem", canonicalChoices(), tooLong);
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("explanation length");
    }

    @Test
    void nullStem_fails() {
        Variant v = new Variant(null, canonicalChoices(), validExplanation());
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("stem blank");
    }

    @Test
    void nullChoicesList_fails() {
        Variant v = new Variant("Stem", null, validExplanation());
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("exactly 4 choices");
        assertThat(result.reason()).contains("null");
    }

    @Test
    void nullChoiceInList_fails() {
        Variant v = new Variant("Stem", java.util.Arrays.asList(
                new Choice("A", "alpha"),
                null,
                new Choice("C", "gamma"),
                new Choice("D", "delta")
        ), validExplanation());
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("choice key invalid");
    }

    @Test
    void nullChoiceKey_fails() {
        Variant v = new Variant("Stem", List.of(
                new Choice("A", "alpha"),
                new Choice("B", "beta"),
                new Choice("C", "gamma"),
                new Choice(null, "delta")
        ), validExplanation());
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("choice key invalid");
    }

    @Test
    void blankChoiceText_fails() {
        Variant v = new Variant("Stem", List.of(
                new Choice("A", "alpha"),
                new Choice("B", "  "),
                new Choice("C", "gamma"),
                new Choice("D", "delta")
        ), validExplanation());
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("text blank");
    }

    @Test
    void nullExplanation_fails() {
        Variant v = new Variant("Stem", canonicalChoices(), null);
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("explanation blank");
    }

    @Test
    void blankExplanation_fails() {
        Variant v = new Variant("Stem", canonicalChoices(), "   ");
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("explanation blank");
    }

    @Test
    void nullCandidate_fails() {
        GenerationGateResult result = validator.check(null);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("candidate is null");
    }

    @Test
    void stemExceedsMax_fails() {
        String tooLongStem = "x".repeat(1001);
        Variant v = new Variant(tooLongStem, canonicalChoices(), validExplanation());
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("exceeds");
    }

    @Test
    void nullCorrectKey_fails() {
        GeneratedQuestion q = new GeneratedQuestion(
                "TEST_ST", null, canonicalVariantEn(), canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("A/B/C/D");
    }

    @Test
    void invalidKeyLetter_fails() {
        Variant v = new Variant("Stem", List.of(
                new Choice("A", "alpha"),
                new Choice("B", "beta"),
                new Choice("C", "gamma"),
                new Choice("E", "delta")
        ), validExplanation());
        GeneratedQuestion q = new GeneratedQuestion("TEST_ST", "A", v, canonicalVariantZh());
        GenerationGateResult result = validator.check(q);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("choice key invalid");
    }

    // ---------- helpers ----------

    private static GeneratedQuestion canonicalQuestion() {
        return new GeneratedQuestion("TEST_ST", "A", canonicalVariantEn(), canonicalVariantZh());
    }

    private static Variant canonicalVariantEn() {
        return new Variant("What is the correct procedure?", canonicalChoices(), validExplanation());
    }

    private static Variant canonicalVariantZh() {
        return new Variant("正确的做法是什么？", List.of(
                new Choice("A", "选项 A"),
                new Choice("B", "选项 B"),
                new Choice("C", "选项 C"),
                new Choice("D", "选项 D")
        ), "解释解释解释解释解释解释解释解释解释解释解释解释解释解释解释解释解释解释解释解释解释解释解释解释解释解释");
    }

    private static List<Choice> canonicalChoices() {
        return List.of(
                new Choice("A", "alpha"),
                new Choice("B", "beta"),
                new Choice("C", "gamma"),
                new Choice("D", "delta")
        );
    }

    private static String validExplanation() {
        return "This is a sufficiently long explanation that exceeds the fifty-character minimum.";
    }
}
