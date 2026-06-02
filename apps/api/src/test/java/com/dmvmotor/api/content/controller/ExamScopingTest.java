package com.dmvmotor.api.content.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Multi-exam foundation (V26): the exam catalog, the per-user "current exam"
 * preference, and content scoping by exam. CA-M1 is the only seeded exam, so
 * these tests stand up a second exam to prove isolation.
 */
class ExamScopingTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;
    private Long caM1;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId = fixtures.insertUser("scope@example.com"); // current_exam_id null
        caM1   = fixtures.defaultExamId();
    }

    // ---------------------------------------------------------------
    // GET /api/v1/exams  (public catalog)
    // ---------------------------------------------------------------

    @Test
    void getExams_returnsActiveCaM1_englishByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/exams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exams", hasSize(1)))
                .andExpect(jsonPath("$.data.exams[0].id").value(String.valueOf(caM1)))
                .andExpect(jsonPath("$.data.exams[0].state_code").value("CA"))
                .andExpect(jsonPath("$.data.exams[0].license_class").value("M1"))
                .andExpect(jsonPath("$.data.exams[0].name").value("California M1 (Motorcycle)"));
    }

    @Test
    void getExams_localizesNameToZh() throws Exception {
        mockMvc.perform(get("/api/v1/exams").param("language", "zh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exams[0].name").value("加州 M1（摩托车）"));
    }

    @Test
    void getExams_excludesInactiveExams() throws Exception {
        Long examB = fixtures.insertExam("TX", "M1", "Texas Motorcycle", "德州摩托车", 80);
        fixtures.setExamStatus(examB, "inactive");

        mockMvc.perform(get("/api/v1/exams"))
                .andExpect(status().isOk())
                // only CA-M1 is active; the inactive TX exam is filtered out
                .andExpect(jsonPath("$.data.exams", hasSize(1)))
                .andExpect(jsonPath("$.data.exams[0].state_code").value("CA"));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/me  →  current_exam
    // ---------------------------------------------------------------

    @Test
    void getMe_newUser_currentExamIsNull() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current_exam").doesNotExist());
    }

    @Test
    void getMe_afterPickingExam_returnsCurrentExam() throws Exception {
        fixtures.setUserCurrentExam(userId, caM1);

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current_exam.id").value(String.valueOf(caM1)))
                .andExpect(jsonPath("$.data.current_exam.state_code").value("CA"))
                .andExpect(jsonPath("$.data.current_exam.license_class").value("M1"))
                .andExpect(jsonPath("$.data.current_exam.name_en").value("California M1 (Motorcycle)"))
                .andExpect(jsonPath("$.data.current_exam.name_zh").value("加州 M1（摩托车）"));
    }

    // ---------------------------------------------------------------
    // PUT /api/v1/me/exam
    // ---------------------------------------------------------------

    @Test
    void putExam_valid_setsCurrentExam() throws Exception {
        mockMvc.perform(put("/api/v1/me/exam")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exam_id":"%d"}
                                """.formatted(caM1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current_exam.id").value(String.valueOf(caM1)));

        // persisted: a follow-up /me reflects it
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.current_exam.id").value(String.valueOf(caM1)));
    }

    @Test
    void putExam_unknownExam_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/me/exam")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exam_id":"999999"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_EXAM"));
    }

    @Test
    void putExam_inactiveExam_returns400() throws Exception {
        Long examB = fixtures.insertExam("TX", "M1", "Texas Motorcycle", "德州摩托车", 80);
        fixtures.setExamStatus(examB, "inactive");

        mockMvc.perform(put("/api/v1/me/exam")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exam_id":"%d"}
                                """.formatted(examB)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_EXAM"));
    }

    @Test
    void putExam_anonymous_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/me/exam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exam_id":"%d"}
                                """.formatted(caM1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putExam_blankExamId_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/me/exam")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exam_id":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/topics  scoped by exam
    // ---------------------------------------------------------------

    @Test
    void listTopics_scopedToCurrentExam() throws Exception {
        // CA-M1 topic + an exam-B-only topic. A user on exam B sees only B's topic.
        fixtures.insertTopic("CA_TOPIC", "CA Topic", "加州题", true, 1);
        Long examB = fixtures.insertExam("TX", "M1", "Texas Motorcycle", "德州摩托车", 80);
        fixtures.insertTopicForExam(examB, "TX_TOPIC", "TX Topic", "德州题", true, 1);
        fixtures.setUserCurrentExam(userId, examB);

        mockMvc.perform(get("/api/v1/topics").header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].code").value("TX_TOPIC"));
    }

    @Test
    void listTopics_explicitExamIdParam_overridesResolvedExam() throws Exception {
        // ?exam_id wins over the caller's resolved exam — used by the picker to
        // preview another exam's topics before switching.
        fixtures.insertTopic("CA_TOPIC", "CA Topic", "加州题", true, 1);
        Long examB = fixtures.insertExam("TX", "M1", "Texas Motorcycle", "德州摩托车", 80);
        fixtures.insertTopicForExam(examB, "TX_TOPIC", "TX Topic", "德州题", true, 1);

        // user defaults to CA-M1, but ?exam_id=examB returns TX topics
        mockMvc.perform(get("/api/v1/topics")
                        .header("Authorization", "Bearer " + userId)
                        .param("exam_id", String.valueOf(examB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].code").value("TX_TOPIC"));
    }

    @Test
    void listTopics_anonymousFallsBackToDefaultExam() throws Exception {
        fixtures.insertTopic("CA_TOPIC", "CA Topic", "加州题", true, 1);
        Long examB = fixtures.insertExam("TX", "M1", "Texas Motorcycle", "德州摩托车", 80);
        fixtures.insertTopicForExam(examB, "TX_TOPIC", "TX Topic", "德州题", true, 1);

        // anonymous → default exam (CA-M1) → only CA topic
        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].code").value("CA_TOPIC"));
    }

    // ---------------------------------------------------------------
    // Practice pool isolation
    // ---------------------------------------------------------------

    @Test
    void practice_servesOnlyCurrentExamQuestions() throws Exception {
        // CA-M1 question (should NOT be served to an exam-B user)…
        Long caTopic = fixtures.insertTopic("CA_T", "CA", "加州", true, 1);
        Long caQ = fixtures.insertQuestion(caTopic, "A");
        fixtures.insertEnVariant(caQ, "CA question?", "ca expl");

        // …and one exam-B question (the only thing a B user may see).
        Long examB = fixtures.insertExam("TX", "M1", "Texas Motorcycle", "德州摩托车", 80);
        Long bTopic = fixtures.insertTopicForExam(examB, "TX_T", "TX", "德州", true, 1);
        Long bQ = fixtures.insertQuestion(bTopic, "B");
        fixtures.insertEnVariant(bQ, "TX question?", "tx expl");

        fixtures.setUserCurrentExam(userId, examB);

        String start = mockMvc.perform(post("/api/v1/practice/sessions")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"en"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.next_question.question_id").value(bQ.toString()))
                .andReturn().getResponse().getContentAsString();

        // Answer the only B question; the CA question must never be served →
        // pool exhausted → SESSION_COMPLETED (proves CA content is out of scope).
        String sessionId = JsonPath.read(start, "$.data.session_id");
        String variantId = JsonPath.read(start, "$.data.next_question.variant_id");
        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                .header("Authorization", "Bearer " + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                        """.formatted(bQ, variantId)));

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SESSION_COMPLETED"));
    }

    // ---------------------------------------------------------------
    // Mock isolation + per-exam pass threshold
    // ---------------------------------------------------------------

    @Test
    void mock_startsCurrentExamTemplate_notAnotherExams() throws Exception {
        // CA-M1 mock (2 q) + exam-B mock (2 q). A B user must get B's template.
        Long caTopic = fixtures.insertTopic("CA_T", "CA", "加州", true, 1);
        Long caMock = fixtures.insertMockExam("CA_MOCK", 2);
        seedMockQuestion(caMock, caTopic, 1);
        seedMockQuestion(caMock, caTopic, 2);

        Long examB = fixtures.insertExam("TX", "M1", "Texas Motorcycle", "德州摩托车", 80);
        Long bTopic = fixtures.insertTopicForExam(examB, "TX_T", "TX", "德州", true, 1);
        Long bMock = fixtures.insertMockExamForExam("TX_MOCK", 2, examB);
        Long bq1 = seedMockQuestion(bMock, bTopic, 1);
        seedMockQuestion(bMock, bTopic, 2);

        fixtures.setUserCurrentExam(userId, examB);
        fixtures.insertAccessPass(userId, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);

        mockMvc.perform(post("/api/v1/mock-exams/attempts")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"en"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.questions", hasSize(2)))
                // first served question is exam-B's (sort_order 1), not CA's
                .andExpect(jsonPath("$.data.questions[0].question_id").value(bq1.toString()));
    }

    @Test
    void mock_perExamThreshold_drivesTermination() throws Exception {
        // Exam B demands 100% → max_allowed_wrong = ceil(2 × 0) = 0, so the FIRST
        // wrong answer auto-terminates. Under CA-M1's 85% the same first wrong
        // would NOT terminate (max_allowed_wrong = 1) — proving the threshold is
        // read from the exam, not hardcoded.
        Long examB = fixtures.insertExam("TX", "M1", "Texas Motorcycle", "德州摩托车", 100);
        Long bTopic = fixtures.insertTopicForExam(examB, "TX_T", "TX", "德州", true, 1);
        Long bMock = fixtures.insertMockExamForExam("TX_MOCK", 2, examB);
        Long bq1 = seedMockQuestion(bMock, bTopic, 1);   // correct key = "A"
        Long bv1 = lastVariantId;
        seedMockQuestion(bMock, bTopic, 2);

        fixtures.setUserCurrentExam(userId, examB);
        fixtures.insertAccessPass(userId, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);

        String start = mockMvc.perform(post("/api/v1/mock-exams/attempts")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"en"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String attemptId = JsonPath.read(start, "$.data.mock_attempt_id");

        // Answer Q1 wrong (correct is "A", we send "B").
        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(bq1, bv1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_correct").value(false))
                .andExpect(jsonPath("$.data.max_allowed_wrong").value(0))
                .andExpect(jsonPath("$.data.should_terminate").value(true));
    }

    // ---------------------------------------------------------------
    // Recommendations + mastery donut scoped by exam
    // ---------------------------------------------------------------

    @Test
    void recommendations_ignoreOtherExamMistakes_andDontDuplicateKeyTopic() throws Exception {
        // User is on exam B. They have a mistake on a B key-topic AND a stray
        // mistake on a CA-M1 topic (cross-exam). Recommendations must:
        //  - surface the B key-topic once (weak-topic pass), and
        //  - drop the CA-M1 mistake (its topic isn't in exam B's taxonomy), and
        //  - NOT re-list the B key-topic again in the uncovered-key-topic pass.
        Long examB = fixtures.insertExam("TX", "M1", "Texas Motorcycle", "德州摩托车", 80);
        Long bKey = fixtures.insertTopicForExam(examB, "TX_KEY", "TX Key", "德州重点", true, 1);
        Long bq = fixtures.insertQuestion(bKey, "A");
        fixtures.insertMistakeRecord(userId, bq, bKey, 2, "practice");

        Long caTopic = fixtures.insertTopic("CA_T", "CA", "加州", true, 1);
        Long caq = fixtures.insertQuestion(caTopic, "A");
        fixtures.insertMistakeRecord(userId, caq, caTopic, 3, "practice");

        fixtures.setUserCurrentExam(userId, examB);

        mockMvc.perform(get("/api/v1/ai/recommendations")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                // exactly one recommendation: the B key-topic (CA mistake dropped,
                // no duplicate from the key-topic pass)
                .andExpect(jsonPath("$.data.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.data.recommendations[0].topic_id").value(bKey.toString()))
                .andExpect(jsonPath("$.data.recommendations[0].reason_code").value("active_mistakes"));
    }

    @Test
    void mastery_childlessTopic_isNotMastered() throws Exception {
        // A topic with no sub-topics can't be mastered — exercises the donut's
        // childless-topic path under the user's (default CA-M1) exam.
        Long topicId = fixtures.insertTopicWithoutSubTopic("NO_SUBS");

        mockMvc.perform(get("/api/v1/topics/mastery")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topics[?(@.code=='NO_SUBS')].is_mastered")
                        .value(org.hamcrest.Matchers.hasItem(false)));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Tracks the variant id created by the most recent seedMockQuestion call. */
    private Long lastVariantId;

    /** Adds an active question (correct key "A") with an en variant to a topic,
     *  and wires it into a mock exam at the given sort order. Returns question id. */
    private Long seedMockQuestion(Long mockExamId, Long topicId, int sortOrder) {
        Long q = fixtures.insertQuestion(topicId, "A");
        lastVariantId = fixtures.insertVariantReturningId(q, "en", "stem " + sortOrder,
                "[{\"key\":\"A\",\"text\":\"Right\"},{\"key\":\"B\",\"text\":\"Wrong\"}]",
                "expl " + sortOrder);
        fixtures.insertMockExamQuestion(mockExamId, q, sortOrder);
        return q;
    }
}
