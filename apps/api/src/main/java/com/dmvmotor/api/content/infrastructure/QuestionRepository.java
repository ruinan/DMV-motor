package com.dmvmotor.api.content.infrastructure;

import com.dmvmotor.api.content.domain.Choice;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class QuestionRepository {

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public QuestionRepository(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
    }

    /**
     * Internal primitive used by Practice / Review / Mock once the caller has
     * verified that the question is in the user's authorized pool.
     * Filters {@code questions.status='active' AND question_variants.status='active'}
     * as defense-in-depth so inactive content can never escape via these flows.
     */
    public Optional<QuestionDetail> findByIdAndLanguage(Long questionId, String language) {
        return load(questionId, language, /* freeTrialOnly */ false);
    }

    /**
     * Public-facing read for the locked-down {@code GET /api/v1/questions/{id}}.
     * Adds {@code allow_in_free_trial=true} on top of the active filter, so
     * anonymous / free-trial / expired callers can only enumerate the
     * documented free-trial pool — never the paid bank.
     */
    public Optional<QuestionDetail> findFreeTrialActiveByIdAndLanguage(Long questionId, String language) {
        return load(questionId, language, /* freeTrialOnly */ true);
    }

    private Optional<QuestionDetail> load(Long questionId, String language, boolean freeTrialOnly) {
        var q = Tables.QUESTIONS;
        var qv = Tables.QUESTION_VARIANTS;

        var step = dsl.select()
                .from(q)
                .join(qv).on(qv.QUESTION_ID.eq(q.ID).and(qv.LANGUAGE_CODE.eq(language)))
                .where(q.ID.eq(questionId))
                .and(q.STATUS.eq("active"))
                .and(qv.STATUS.eq("active"));

        Record record = (freeTrialOnly ? step.and(q.ALLOW_IN_FREE_TRIAL.isTrue()) : step)
                .fetchOne();

        if (record == null) return Optional.empty();

        List<Choice> choices = parseChoices(record.get(qv.CHOICES_PAYLOAD).data());

        return Optional.of(new QuestionDetail(
                record.get(q.ID),
                record.get(qv.ID),
                record.get(q.PRIMARY_TOPIC_ID),
                record.get(q.CORRECT_CHOICE_KEY),
                language,
                record.get(qv.STEM_TEXT),
                choices,
                record.get(qv.EXPLANATION_TEXT)
        ));
    }

    public static List<Choice> parseChoices(ObjectMapper mapper, String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse choices_payload: " + json, e);
        }
    }

    private List<Choice> parseChoices(String json) {
        return parseChoices(objectMapper, json);
    }
}
