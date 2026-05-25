package com.dmvmotor.api.aiqgen.application;

import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion;
import com.dmvmotor.api.aiqgen.domain.GenerationGateResult;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic shape gate — no LLM call. Runs first because it filters out
 * the most common failure mode (LLM emits malformed JSON or skips a field)
 * before any expensive judge call.
 *
 * <p>Checks:
 * <ul>
 *   <li>both en + zh variants present</li>
 *   <li>stem non-blank, ≤ 1000 chars</li>
 *   <li>exactly 4 choices per variant, keys = A,B,C,D, all unique non-blank texts</li>
 *   <li>correct_choice_key ∈ {A,B,C,D}</li>
 *   <li>explanation present in both variants, length 50-500 chars</li>
 * </ul>
 */
public final class FormatValidator {

    private static final Set<String> ALLOWED_KEYS = Set.of("A", "B", "C", "D");
    // 30 chars accommodates dense Chinese explanations (~15-20 hanzi = a clear
    // sentence) while still rejecting unhelpful one-liners.
    private static final int EXPLANATION_MIN = 30;
    private static final int EXPLANATION_MAX = 500;
    private static final int STEM_MAX = 1000;

    public GenerationGateResult check(GeneratedQuestion q) {
        if (q == null) {
            return GenerationGateResult.fail("candidate is null");
        }
        if (q.correctChoiceKey() == null || !ALLOWED_KEYS.contains(q.correctChoiceKey())) {
            return GenerationGateResult.fail("correct_choice_key must be one of A/B/C/D, got: " + q.correctChoiceKey());
        }
        GenerationGateResult enResult = checkVariant("en", q.en());
        if (!enResult.passed()) return enResult;
        GenerationGateResult zhResult = checkVariant("zh", q.zh());
        if (!zhResult.passed()) return zhResult;
        return GenerationGateResult.pass("format ok");
    }

    private GenerationGateResult checkVariant(String lang, GeneratedQuestion.Variant v) {
        if (v == null) {
            return GenerationGateResult.fail(lang + " variant missing");
        }
        if (v.stem() == null || v.stem().isBlank()) {
            return GenerationGateResult.fail(lang + " stem blank");
        }
        if (v.stem().length() > STEM_MAX) {
            return GenerationGateResult.fail(lang + " stem exceeds " + STEM_MAX + " chars");
        }
        List<GeneratedQuestion.Choice> choices = v.choices();
        if (choices == null || choices.size() != 4) {
            return GenerationGateResult.fail(lang + " must have exactly 4 choices, got: "
                    + (choices == null ? "null" : choices.size()));
        }
        Set<String> seenKeys = new HashSet<>();
        Set<String> seenTexts = new HashSet<>();
        for (GeneratedQuestion.Choice c : choices) {
            if (c == null || c.key() == null || !ALLOWED_KEYS.contains(c.key())) {
                return GenerationGateResult.fail(lang + " choice key invalid: " + (c == null ? "null" : c.key()));
            }
            if (!seenKeys.add(c.key())) {
                return GenerationGateResult.fail(lang + " duplicate choice key: " + c.key());
            }
            if (c.text() == null || c.text().isBlank()) {
                return GenerationGateResult.fail(lang + " choice " + c.key() + " text blank");
            }
            String normalized = c.text().trim().toLowerCase(Locale.ROOT);
            if (!seenTexts.add(normalized)) {
                return GenerationGateResult.fail(lang + " duplicate choice text: " + c.text());
            }
        }
        // No need to assert seenKeys == ALLOWED_KEYS — once we've seen 4 distinct
        // keys each in {A,B,C,D}, the set must equal {A,B,C,D}.
        if (v.explanation() == null || v.explanation().isBlank()) {
            return GenerationGateResult.fail(lang + " explanation blank");
        }
        int len = v.explanation().length();
        if (len < EXPLANATION_MIN || len > EXPLANATION_MAX) {
            return GenerationGateResult.fail(lang + " explanation length " + len
                    + " outside [" + EXPLANATION_MIN + ", " + EXPLANATION_MAX + "]");
        }
        return GenerationGateResult.pass(lang + " ok");
    }
}
