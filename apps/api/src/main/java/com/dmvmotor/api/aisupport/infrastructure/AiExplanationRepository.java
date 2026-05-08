package com.dmvmotor.api.aisupport.infrastructure;

import com.dmvmotor.api.aisupport.domain.AiExplanation;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import com.dmvmotor.api.infrastructure.jooq.generated.tables.AiExplanations;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class AiExplanationRepository {

    private final DSLContext dsl;

    public AiExplanationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<AiExplanation> findByUserQuestionLanguage(Long userId, Long questionId, String language) {
        AiExplanations t = Tables.AI_EXPLANATIONS;
        return dsl.selectFrom(t)
                .where(t.USER_ID.eq(userId))
                .and(t.QUESTION_ID.eq(questionId))
                .and(t.LANGUAGE.eq(language))
                .fetchOptional()
                .map(AiExplanationRepository::toDomain);
    }

    public int countByUserSince(Long userId, OffsetDateTime since) {
        AiExplanations t = Tables.AI_EXPLANATIONS;
        return dsl.fetchCount(t,
                t.USER_ID.eq(userId).and(t.CREATED_AT.greaterOrEqual(since)));
    }

    public Optional<OffsetDateTime> findLatestCreatedAtByUser(Long userId) {
        AiExplanations t = Tables.AI_EXPLANATIONS;
        return dsl.select(t.CREATED_AT)
                .from(t)
                .where(t.USER_ID.eq(userId))
                .orderBy(t.CREATED_AT.desc())
                .limit(1)
                .fetchOptional()
                .map(r -> r.get(t.CREATED_AT));
    }

    public AiExplanation insert(Long userId, Long questionId, String language,
                                 String selectedChoiceKey, String explanation,
                                 String model, Integer tokensIn, Integer tokensOut) {
        AiExplanations t = Tables.AI_EXPLANATIONS;
        Record record = dsl.insertInto(t)
                .set(t.USER_ID, userId)
                .set(t.QUESTION_ID, questionId)
                .set(t.LANGUAGE, language)
                .set(t.SELECTED_CHOICE_KEY, selectedChoiceKey)
                .set(t.EXPLANATION, explanation)
                .set(t.MODEL, model)
                .set(t.TOKENS_IN, tokensIn)
                .set(t.TOKENS_OUT, tokensOut)
                .returning()
                .fetchOne();
        return toDomain(record);
    }

    private static AiExplanation toDomain(Record r) {
        AiExplanations t = Tables.AI_EXPLANATIONS;
        return new AiExplanation(
                r.get(t.ID),
                r.get(t.USER_ID),
                r.get(t.QUESTION_ID),
                r.get(t.LANGUAGE),
                r.get(t.SELECTED_CHOICE_KEY),
                r.get(t.EXPLANATION),
                r.get(t.MODEL),
                r.get(t.TOKENS_IN),
                r.get(t.TOKENS_OUT),
                r.get(t.CREATED_AT)
        );
    }
}
