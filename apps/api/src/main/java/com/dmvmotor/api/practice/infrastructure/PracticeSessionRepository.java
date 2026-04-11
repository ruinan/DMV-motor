package com.dmvmotor.api.practice.infrastructure;

import com.dmvmotor.api.content.domain.Choice;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.content.infrastructure.QuestionRepository;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import com.dmvmotor.api.practice.domain.PracticeSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PracticeSessionRepository {

    private final DSLContext    dsl;
    private final ObjectMapper  objectMapper;

    public PracticeSessionRepository(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl          = dsl;
        this.objectMapper = objectMapper;
    }

    public Long create(Long userId, String entryType, String languageCode) {
        var ps = Tables.PRACTICE_SESSIONS;
        return dsl.insertInto(ps)
                .set(ps.USER_ID,       userId)
                .set(ps.ENTRY_TYPE,    entryType)
                .set(ps.LANGUAGE_CODE, languageCode)
                .returningResult(ps.ID)
                .fetchOne()
                .value1();
    }

    public Optional<PracticeSession> findById(Long sessionId) {
        var ps = Tables.PRACTICE_SESSIONS;
        Record r = dsl.selectFrom(ps).where(ps.ID.eq(sessionId)).fetchOne();
        if (r == null) return Optional.empty();
        return Optional.of(map(r));
    }

    /** Returns the next unanswered question for this session, or empty if all answered. */
    public Optional<QuestionDetail> findNextUnansweredQuestion(Long sessionId, String languageCode) {
        var q  = Tables.QUESTIONS;
        var qv = Tables.QUESTION_VARIANTS;
        var pa = Tables.PRACTICE_ATTEMPTS;

        Record r = dsl.select()
                .from(q)
                .join(qv).on(qv.QUESTION_ID.eq(q.ID).and(qv.LANGUAGE_CODE.eq(languageCode)))
                .where(q.ALLOW_IN_PRACTICE.isTrue()
                        .and(q.STATUS.eq("active"))
                        .and(q.ID.notIn(
                                dsl.select(pa.QUESTION_ID)
                                   .from(pa)
                                   .where(pa.PRACTICE_SESSION_ID.eq(sessionId))
                        )))
                .orderBy(q.ID.asc())
                .limit(1)
                .fetchOne();

        if (r == null) return Optional.empty();

        List<Choice> choices = QuestionRepository.parseChoices(objectMapper,
                r.get(qv.CHOICES_PAYLOAD).data());

        return Optional.of(new QuestionDetail(
                r.get(q.ID),
                r.get(qv.ID),
                r.get(q.PRIMARY_TOPIC_ID),
                r.get(q.CORRECT_CHOICE_KEY),
                languageCode,
                r.get(qv.STEM_TEXT),
                choices,
                r.get(qv.EXPLANATION_TEXT)
        ));
    }

    public int countAnswered(Long sessionId) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        return dsl.fetchCount(pa, pa.PRACTICE_SESSION_ID.eq(sessionId));
    }

    public int countTotal(String languageCode) {
        var q  = Tables.QUESTIONS;
        var qv = Tables.QUESTION_VARIANTS;
        return dsl.fetchCount(
                dsl.select().from(q)
                   .join(qv).on(qv.QUESTION_ID.eq(q.ID).and(qv.LANGUAGE_CODE.eq(languageCode)))
                   .where(q.ALLOW_IN_PRACTICE.isTrue().and(q.STATUS.eq("active")))
        );
    }

    public boolean hasAttempt(Long sessionId, Long questionId) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        return dsl.fetchExists(pa,
                pa.PRACTICE_SESSION_ID.eq(sessionId).and(pa.QUESTION_ID.eq(questionId)));
    }

    public void saveAttempt(Long sessionId, Long userId, Long questionId,
                             Long variantId, String selectedKey, boolean isCorrect) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        dsl.insertInto(pa)
                .set(pa.PRACTICE_SESSION_ID,   sessionId)
                .set(pa.USER_ID,               userId)
                .set(pa.QUESTION_ID,           questionId)
                .set(pa.QUESTION_VARIANT_ID,   variantId)
                .set(pa.SELECTED_CHOICE_KEY,   selectedKey)
                .set(pa.IS_CORRECT,            isCorrect)
                .execute();
    }

    public void updateStatus(Long sessionId, String status) {
        var ps = Tables.PRACTICE_SESSIONS;
        dsl.update(ps)
                .set(ps.STATUS, status)
                .where(ps.ID.eq(sessionId))
                .execute();
    }

    private PracticeSession map(Record r) {
        var ps = Tables.PRACTICE_SESSIONS;
        return new PracticeSession(
                r.get(ps.ID),
                r.get(ps.USER_ID),
                r.get(ps.STATUS),
                r.get(ps.ENTRY_TYPE),
                r.get(ps.LANGUAGE_CODE),
                r.get(ps.STARTED_AT),
                r.get(ps.COMPLETED_AT)
        );
    }
}
