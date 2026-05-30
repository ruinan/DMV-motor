package com.dmvmotor.api.practice.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PracticeSessionControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired TestFixtures fixtures;

    private Long topicId;
    private Long questionId;
    private Long variantEnId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();

        topicId   = fixtures.insertTopic("TRAFFIC_SIGNS", "Traffic Signs", "交通标志", true, 1);
        questionId = fixtures.insertQuestion(topicId, "B");
        variantEnId = fixtures.insertVariantReturningId(questionId, "en",
                "What does a stop sign look like?",
                "[{\"key\":\"A\",\"text\":\"Yellow triangle\"},{\"key\":\"B\",\"text\":\"Red octagon\"},{\"key\":\"C\",\"text\":\"Green circle\"}]",
                "A stop sign is a red octagon.");
        fixtures.insertVariant(questionId, "zh",
                "停车标志是什么样子的？",
                "[{\"key\":\"A\",\"text\":\"黄色三角形\"},{\"key\":\"B\",\"text\":\"红色八角形\"},{\"key\":\"C\",\"text\":\"绿色圆形\"}]",
                "停车标志是红色八角形。");
    }

    // ---------------------------------------------------------------
    // POST /api/v1/practice/sessions
    // ---------------------------------------------------------------

    @Test
    void startSession_noLanguage_defaultsToEnglish() throws Exception {
        mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.language").value("en"));
    }

    @Test
    void startSession_freeTrial_returnsSessionWithFirstQuestion() throws Exception {
        mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"en"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.session_id").isString())
                .andExpect(jsonPath("$.data.entry_type").value("free_trial"))
                .andExpect(jsonPath("$.data.status").value("in_progress"))
                .andExpect(jsonPath("$.data.language").value("en"))
                .andExpect(jsonPath("$.data.next_question.question_id").isString())
                .andExpect(jsonPath("$.data.next_question.stem").isString())
                .andExpect(jsonPath("$.data.next_question.choices", hasSize(3)));
    }

    @Test
    void startSession_withTopicFilter_restrictsPoolToThoseTopics() throws Exception {
        // A second topic + free-trial question. A session filtered to topic 1
        // must only ever serve topic 1's question.
        Long topic2 = fixtures.insertTopic("SPEED", "Speed", "速度", false, 2);
        Long q2 = fixtures.insertQuestion(topic2, "A");
        fixtures.insertVariantReturningId(q2, "en", "Speed limit?",
                "[{\"key\":\"A\",\"text\":\"25\"},{\"key\":\"B\",\"text\":\"35\"},{\"key\":\"C\",\"text\":\"45\"}]",
                "25 mph.");

        String startBody = mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"en","topic_filter":[%d]}
                                """.formatted(topicId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.next_question.question_id")
                        .value(questionId.toString()))
                .andReturn().getResponse().getContentAsString();

        String sessionId = extractSessionId(startBody);

        // Answer topic-1's only question, then ask for the next one. The filter
        // is persisted on the session (decoded on read-back), so topic-2's
        // question must NOT be served — pool is exhausted → SESSION_COMPLETED.
        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                        """.formatted(questionId, variantEnId)));

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SESSION_COMPLETED"));
    }

    @Test
    void freeTrialSession_cappedAt15() throws Exception {
        // setUp's 1 + 24 more = 25 active questions, but a free-trial session
        // is the smaller 15-question taster.
        seedExtraFreeTrialQuestions(24);

        String startBody = mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"en"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = extractSessionId(startBody);

        mockMvc.perform(get("/api/v1/practice/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_count").value(15))
                .andExpect(jsonPath("$.data.answered_count").value(0));
    }

    @Test
    void fullSession_cappedAt30() throws Exception {
        // 35 active questions; a paid full-practice session gives the bigger
        // 30-question round (more than the free taster).
        seedExtraFreeTrialQuestions(34);
        Long paidUser = fixtures.insertUser("full-practice@example.com");
        fixtures.insertAccessPass(paidUser, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);

        String startBody = mockMvc.perform(post("/api/v1/practice/sessions")
                        .header("Authorization", "Bearer " + paidUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"full","language":"en"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = extractSessionId(startBody);

        mockMvc.perform(get("/api/v1/practice/sessions/{id}", sessionId)
                        .header("Authorization", "Bearer " + paidUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_count").value(30));
    }

    @Test
    void getSessionStatus_topicFilteredSession_totalReflectsFilteredPoolNotFullBank()
            throws Exception {
        // Dev-quality audit #1 (correctness): a topic-scoped "Practice these"
        // session must report total_count = its FILTERED pool size, not
        // min(cap, full bank). topic1 (setUp) already holds 1 free-trial
        // question; add 2 more so the filtered pool is 3.
        Long fq1 = fixtures.insertQuestion(topicId, "B");
        fixtures.insertVariantReturningId(fq1, "en", "Topic1 Q-b",
                "[{\"key\":\"A\",\"text\":\"a\"},{\"key\":\"B\",\"text\":\"b\"}]", "x");
        Long fq2 = fixtures.insertQuestion(topicId, "B");
        fixtures.insertVariantReturningId(fq2, "en", "Topic1 Q-c",
                "[{\"key\":\"A\",\"text\":\"a\"},{\"key\":\"B\",\"text\":\"b\"}]", "x");

        // Pad the full bank with 20 questions in a DIFFERENT topic so an
        // unfiltered count would (wrongly) hit the 15 free-trial cap.
        Long otherTopic = fixtures.insertTopic("OTHER_BANK", "Other", "其他", false, 9);
        for (int i = 0; i < 20; i++) {
            Long qid = fixtures.insertQuestion(otherTopic, "B");
            fixtures.insertVariantReturningId(qid, "en", "Bank Q" + i,
                    "[{\"key\":\"A\",\"text\":\"a\"},{\"key\":\"B\",\"text\":\"b\"}]", "x");
        }

        String startBody = mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"en","topic_filter":[%d]}
                                """.formatted(topicId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = extractSessionId(startBody);

        mockMvc.perform(get("/api/v1/practice/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_count").value(3))
                .andExpect(jsonPath("$.data.answered_count").value(0));
    }

    @Test
    void freeTrialSession_completesAfter15Answers() throws Exception {
        QuestionPool pool = seedExtraFreeTrialQuestions(24);

        String startBody = mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"en"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = extractSessionId(startBody);

        // Answer exactly 15 distinct questions (the free-trial cap).
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                    """.formatted(pool.qIds().get(i), pool.vIds().get(i))))
                    .andExpect(status().isOk());
        }

        // The 16th request is refused — the free-trial session is full.
        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SESSION_COMPLETED"));
    }

    /** Seeds {@code n} extra active free-trial questions (correct key "B") and
     *  returns all question/variant ids including setUp's original. */
    private QuestionPool seedExtraFreeTrialQuestions(int n) {
        List<Long> qIds = new ArrayList<>(List.of(questionId));
        List<Long> vIds = new ArrayList<>(List.of(variantEnId));
        for (int i = 0; i < n; i++) {
            Long qid = fixtures.insertQuestion(topicId, "B");
            Long vid = fixtures.insertVariantReturningId(qid, "en", "Capped Q" + i,
                    "[{\"key\":\"A\",\"text\":\"a\"},{\"key\":\"B\",\"text\":\"b\"}]",
                    "explanation " + i);
            qIds.add(qid);
            vIds.add(vid);
        }
        return new QuestionPool(qIds, vIds);
    }

    private record QuestionPool(List<Long> qIds, List<Long> vIds) {}

    private static String extractSessionId(String json) {
        String key = "\"session_id\":\"";
        int s = json.indexOf(key) + key.length();
        return json.substring(s, json.indexOf("\"", s));
    }

    @Test
    void startSession_topicFilterNoMatchingQuestions_returns422() throws Exception {
        // Filter to a topic id that has no questions → empty pool → 422.
        mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"en","topic_filter":[999999]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NO_QUESTIONS_AVAILABLE"));
    }

    @Test
    void startSession_missingEntryType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void startSession_noQuestionsAvailable_returns422() throws Exception {
        fixtures.truncateAll();

        mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"en"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NO_QUESTIONS_AVAILABLE"));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/practice/sessions/{id}/next-question
    // ---------------------------------------------------------------

    @Test
    void nextQuestion_activeSession_returnsQuestion() throws Exception {
        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.question_id").isString())
                .andExpect(jsonPath("$.data.stem").isString())
                .andExpect(jsonPath("$.data.choices", hasSize(3)))
                .andExpect(jsonPath("$.data.progress.answered_count").value(0));
    }

    @Test
    void nextQuestion_sessionNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void nextQuestion_allQuestionsAnswered_returns404WithSessionCompleted() throws Exception {
        String sessionId = startSessionAndGetId("en");
        submitAnswer(sessionId, questionId, variantEnId, "B");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SESSION_COMPLETED"));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/practice/sessions/{id}/answers
    // ---------------------------------------------------------------

    @Test
    void submitAnswer_correctAnswer_returnsIsCorrectTrue() throws Exception {
        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId, variantEnId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_correct").value(true))
                .andExpect(jsonPath("$.data.correct_choice_key").value("B"))
                .andExpect(jsonPath("$.data.explanation").isString())
                .andExpect(jsonPath("$.data.progress.answered_count").value(1));
    }

    @Test
    void submitAnswer_wrongAnswer_recordsMistakeAndReturnsCorrectKey() throws Exception {
        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(questionId, variantEnId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_correct").value(false))
                .andExpect(jsonPath("$.data.correct_choice_key").value("B"));
    }

    @Test
    void submitAnswer_nullExplanation_returnsEmptyString() throws Exception {
        fixtures.truncateAll();
        Long t2 = fixtures.insertTopic("NULL_EXPL_TOPIC");
        Long q2 = fixtures.insertQuestion(t2, "A");
        Long v2 = fixtures.insertVariantReturningId(q2, "en", "Which option?",
                "[{\"key\":\"A\",\"text\":\"Opt A\"},{\"key\":\"B\",\"text\":\"Opt B\"}]",
                null);

        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(q2, v2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.explanation").value(""));
    }

    // ---------------------------------------------------------------
    // submitAnswer — question pool / session-status guards (A1–A5)
    // ---------------------------------------------------------------

    @Test
    void submitAnswer_freeTrialSession_paidOnlyQuestion_returns400() throws Exception {
        // A1: free_trial session, question is allow_in_free_trial=false
        Long paidQid = fixtures.insertPaidOnlyQuestion(topicId, "A");
        Long paidVid = fixtures.insertVariantReturningId(paidQid, "en", "Paid only?",
                "[{\"key\":\"A\",\"text\":\"x\"},{\"key\":\"B\",\"text\":\"y\"}]", "expl");

        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(paidQid, paidVid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("QUESTION_NOT_IN_SESSION"));
    }

    @Test
    void submitAnswer_fullSession_inactiveQuestion_returns400() throws Exception {
        // A2: full session with active pass, question.status = 'inactive'
        Long uid = fixtures.insertUser("a2_full@example.com");
        fixtures.insertAccessPass(uid, "active",
                java.time.OffsetDateTime.now().minusDays(1),
                java.time.OffsetDateTime.now().plusDays(30), 3, 0);
        Long deadQid = fixtures.insertInactiveQuestion(topicId, "A");
        Long deadVid = fixtures.insertVariantReturningId(deadQid, "en", "Dead?",
                "[{\"key\":\"A\",\"text\":\"x\"},{\"key\":\"B\",\"text\":\"y\"}]", "expl");

        String sessionId = startFullSessionAsUser(uid, "en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .header("Authorization", "Bearer " + uid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(deadQid, deadVid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("QUESTION_NOT_IN_SESSION"));
    }

    @Test
    void submitAnswer_completedSession_returns409() throws Exception {
        // A3: session.status = completed
        String sessionId = startSessionAndGetId("en");
        mockMvc.perform(post("/api/v1/practice/sessions/{id}/complete", sessionId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId, variantEnId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT_STATE"));
    }

    @Test
    void submitAnswer_fullSession_paidOnlyQuestion_returns200() throws Exception {
        // A4 control: full session + paid-only question → allowed
        Long uid = fixtures.insertUser("a4_full@example.com");
        fixtures.insertAccessPass(uid, "active",
                java.time.OffsetDateTime.now().minusDays(1),
                java.time.OffsetDateTime.now().plusDays(30), 3, 0);
        Long paidQid = fixtures.insertPaidOnlyQuestion(topicId, "A");
        Long paidVid = fixtures.insertVariantReturningId(paidQid, "en", "Paid only A4?",
                "[{\"key\":\"A\",\"text\":\"x\"},{\"key\":\"B\",\"text\":\"y\"}]", "expl");

        String sessionId = startFullSessionAsUser(uid, "en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .header("Authorization", "Bearer " + uid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(paidQid, paidVid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_correct").value(true));
    }

    @Test
    void submitAnswer_alreadyAnswered_returns409() throws Exception {
        String sessionId = startSessionAndGetId("en");
        submitAnswer(sessionId, questionId, variantEnId, "A");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId, variantEnId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("QUESTION_ALREADY_SUBMITTED"));
    }

    // ---------------------------------------------------------------
    // INVALID_ID_FORMAT — path / body string IDs that aren't numeric
    // ---------------------------------------------------------------

    @Test
    void submitAnswer_pathIdNotNumeric_returns400() throws Exception {
        // F1: path variable can't be parsed as Long. Currently bubbles up as Spring's default
        // 400 (or 500 depending on the converter); we want a uniform envelope.
        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId, variantEnId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ID_FORMAT"));
    }

    @Test
    void submitAnswer_bodyQuestionIdNotNumeric_returns400() throws Exception {
        // F2: body question_id is the string "abc". Long.parseLong throws unchecked → 500;
        // we want 400 INVALID_ID_FORMAT.
        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"abc","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(variantEnId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ID_FORMAT"))
                .andExpect(jsonPath("$.error.message", containsString("question_id")));
    }

    @Test
    void submitAnswer_bodyVariantIdNotNumeric_returns400() throws Exception {
        // F3: body variant_id is non-numeric.
        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"x","selected_choice_key":"B"}
                                """.formatted(questionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ID_FORMAT"))
                .andExpect(jsonPath("$.error.message", containsString("variant_id")));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/practice/sessions/{id}
    // ---------------------------------------------------------------

    @Test
    void getSession_returnsStatusAndProgress() throws Exception {
        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session_id").value(sessionId))
                .andExpect(jsonPath("$.data.status").value("in_progress"))
                .andExpect(jsonPath("$.data.answered_count").value(0))
                .andExpect(jsonPath("$.data.total_count").isNumber());
    }

    // ---------------------------------------------------------------
    // POST /api/v1/practice/sessions/{id}/complete
    // ---------------------------------------------------------------

    @Test
    void completeSession_setsStatusCompleted() throws Exception {
        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/complete", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session_id").value(sessionId))
                .andExpect(jsonPath("$.data.status").value("completed"));
    }

    @Test
    void completeSession_alreadyCompleted_returns409() throws Exception {
        String sessionId = startSessionAndGetId("en");
        mockMvc.perform(post("/api/v1/practice/sessions/{id}/complete", sessionId));

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/complete", sessionId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT_STATE"));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/practice/sessions/{id}/attempts — review history
    // (Round 4 #2). Read-only list of past attempts in submission order.
    // No write side-effects; safe for in-progress and completed sessions.
    // ---------------------------------------------------------------

    @Test
    void getAttempts_unstartedSession_returnsEmptyList() throws Exception {
        String sessionId = startSessionAndGetId("en");
        mockMvc.perform(get("/api/v1/practice/sessions/{id}/attempts", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    @Test
    void getAttempts_anonymous_afterTwoAnswers_returnsBothChronologically() throws Exception {
        Long t2 = fixtures.insertTopic("ATTEMPTS_TOPIC2");
        Long q2 = fixtures.insertQuestion(t2, "A");
        Long v2 = fixtures.insertVariantReturningId(q2, "en", "Q2 stem en?",
                "[{\"key\":\"A\",\"text\":\"a-text\"},{\"key\":\"B\",\"text\":\"b-text\"}]",
                "Q2 explanation");

        String sessionId = startSessionAndGetId("en");
        // 1st: correct (q1, B is correct)
        submitAnswer(sessionId, questionId, variantEnId, "B");
        // 2nd: wrong (q2, A is correct, pick B)
        submitAnswer(sessionId, q2, v2, "B");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/attempts", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].question_id").value(questionId.toString()))
                .andExpect(jsonPath("$.data.items[0].selected_choice_key").value("B"))
                .andExpect(jsonPath("$.data.items[0].correct_choice_key").value("B"))
                .andExpect(jsonPath("$.data.items[0].is_correct").value(true))
                .andExpect(jsonPath("$.data.items[0].stem")
                        .value("What does a stop sign look like?"))
                .andExpect(jsonPath("$.data.items[0].choices", hasSize(3)))
                .andExpect(jsonPath("$.data.items[0].explanation")
                        .value("A stop sign is a red octagon."))
                .andExpect(jsonPath("$.data.items[1].question_id").value(q2.toString()))
                .andExpect(jsonPath("$.data.items[1].selected_choice_key").value("B"))
                .andExpect(jsonPath("$.data.items[1].correct_choice_key").value("A"))
                .andExpect(jsonPath("$.data.items[1].is_correct").value(false));
    }

    @Test
    void getAttempts_languageRespected_returnsZhStem() throws Exception {
        String sessionId = startSessionAndGetId("zh");
        submitAnswer(sessionId, questionId, variantEnId, "B");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/attempts", sessionId)
                        .param("language", "zh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].stem").value("停车标志是什么样子的？"))
                .andExpect(jsonPath("$.data.items[0].explanation").value("停车标志是红色八角形。"));
    }

    @Test
    void getAttempts_unknownSession_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/practice/sessions/{id}/attempts", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void getAttempts_ownedSession_differentUser_returns403() throws Exception {
        Long owner = fixtures.insertUser("attempts_owner@example.com");
        Long stranger = fixtures.insertUser("attempts_stranger@example.com");
        String sessionId = startSessionAsUser(owner, "en");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/attempts", sessionId)
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void getAttempts_fullSession_passExpiredMidSession_returns403() throws Exception {
        Long uid = fixtures.insertUser("attempts_full_expire@example.com");
        Long passId = fixtures.insertAccessPass(uid, "active",
                java.time.OffsetDateTime.now().minusDays(1),
                java.time.OffsetDateTime.now().plusDays(30), 3, 0);
        String sessionId = startFullSessionAsUser(uid, "en");

        fixtures.expireAccessPass(passId);

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/attempts", sessionId)
                        .header("Authorization", "Bearer " + uid))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void getAttempts_pathIdNotNumeric_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/practice/sessions/{id}/attempts", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ID_FORMAT"));
    }

    @Test
    void getAttempts_noLanguageParam_fallsBackToSessionLanguage() throws Exception {
        // PracticeService.listAttempts: when the client omits ?language=, the
        // resolver falls back to the session's original language. Drives the
        // null/blank branch that an explicit language= can't reach.
        String sessionId = startSessionAndGetId("zh");
        submitAnswer(sessionId, questionId, variantEnId, "B");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/attempts", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].language").value("zh"))
                .andExpect(jsonPath("$.data.items[0].stem").value("停车标志是什么样子的？"));
    }

    @Test
    void getAttempts_blankLanguageParam_fallsBackToSessionLanguage() throws Exception {
        // The other half of the fallback condition — empty-string language
        // is treated the same as missing.
        String sessionId = startSessionAndGetId("zh");
        submitAnswer(sessionId, questionId, variantEnId, "B");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/attempts", sessionId)
                        .param("language", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].language").value("zh"));
    }

    @Test
    void getAttempts_explanationNull_returnsEmptyString() throws Exception {
        // Mirror PracticeAttemptDetail's explanation-null guard, which the
        // happy-path tests can't reach (variants are always inserted with a
        // non-null explanation).
        Long t2 = fixtures.insertTopic("ATTEMPTS_NULL_EXPL");
        Long q2 = fixtures.insertQuestion(t2, "B");
        Long v2 = fixtures.insertVariantReturningId(q2, "en", "Q2 stem en?",
                "[{\"key\":\"A\",\"text\":\"a\"},{\"key\":\"B\",\"text\":\"b\"}]",
                null);

        String sessionId = startSessionAndGetId("en");
        submitAnswer(sessionId, q2, v2, "B");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/attempts", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].explanation").value(""));
    }

    // ---------------------------------------------------------------
    // entry_type=full access control
    // ---------------------------------------------------------------

    @Test
    void startSession_fullType_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"full","language":"en"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void startSession_fullType_noPass_returns403() throws Exception {
        Long uid = fixtures.insertUser("nopass_full@example.com");

        mockMvc.perform(post("/api/v1/practice/sessions")
                        .header("Authorization", "Bearer " + uid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"full","language":"en"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void startSession_fullType_withActivePass_createsSession() throws Exception {
        Long uid = fixtures.insertUser("full_pass@example.com");
        fixtures.insertAccessPass(uid, "active",
                java.time.OffsetDateTime.now().minusDays(1),
                java.time.OffsetDateTime.now().plusDays(30), 3, 0);

        mockMvc.perform(post("/api/v1/practice/sessions")
                        .header("Authorization", "Bearer " + uid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"full","language":"en"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.entry_type").value("full"))
                .andExpect(jsonPath("$.data.next_question.question_id").isString());
    }

    // ---------------------------------------------------------------
    // entry_type=full continuation gate — pass expiring mid-session must
    // block all subsequent ops (sec audit #2). Without this the pass check
    // ran only at session creation and an expired user could keep going.
    // ---------------------------------------------------------------

    @Test
    void nextQuestion_fullSession_passExpiredMidSession_returns403() throws Exception {
        Long uid = fixtures.insertUser("expire_next@example.com");
        Long passId = fixtures.insertAccessPass(uid, "active",
                java.time.OffsetDateTime.now().minusDays(1),
                java.time.OffsetDateTime.now().plusDays(30), 3, 0);
        String sessionId = startFullSessionAsUser(uid, "en");

        fixtures.expireAccessPass(passId);

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId)
                        .header("Authorization", "Bearer " + uid))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void submitAnswer_fullSession_passExpiredMidSession_returns403() throws Exception {
        Long uid = fixtures.insertUser("expire_submit@example.com");
        Long passId = fixtures.insertAccessPass(uid, "active",
                java.time.OffsetDateTime.now().minusDays(1),
                java.time.OffsetDateTime.now().plusDays(30), 3, 0);
        String sessionId = startFullSessionAsUser(uid, "en");

        fixtures.expireAccessPass(passId);

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .header("Authorization", "Bearer " + uid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId, variantEnId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void completeSession_fullSession_passExpiredMidSession_returns403() throws Exception {
        Long uid = fixtures.insertUser("expire_complete@example.com");
        Long passId = fixtures.insertAccessPass(uid, "active",
                java.time.OffsetDateTime.now().minusDays(1),
                java.time.OffsetDateTime.now().plusDays(30), 3, 0);
        String sessionId = startFullSessionAsUser(uid, "en");

        fixtures.expireAccessPass(passId);

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/complete", sessionId)
                        .header("Authorization", "Bearer " + uid))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void nextQuestion_freeTrialSession_unaffectedByExpiredPass() throws Exception {
        // Regression: the new continuation gate must apply only to entry_type=full.
        // Free-trial sessions (anonymous or owned) keep working without a pass.
        Long uid = fixtures.insertUser("free_unaffected@example.com");
        String sessionId = startSessionAsUser(uid, "en");

        // Even granting+expiring a pass shouldn't influence a free_trial session.
        Long passId = fixtures.insertAccessPass(uid, "active",
                java.time.OffsetDateTime.now().minusDays(1),
                java.time.OffsetDateTime.now().plusDays(30), 3, 0);
        fixtures.expireAccessPass(passId);

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId)
                        .header("Authorization", "Bearer " + uid))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------
    // Ownership / FORBIDDEN tests
    // ---------------------------------------------------------------

    @Test
    void nextQuestion_sameAuthenticatedUser_returns200() throws Exception {
        Long uid = fixtures.insertUser("puser@example.com");
        String sessionId = startSessionAsUser(uid, "en");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId)
                        .header("Authorization", "Bearer " + uid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.question_id").isString());
    }

    @Test
    void nextQuestion_differentUser_returns403() throws Exception {
        Long userA = fixtures.insertUser("pa@example.com");
        Long userB = fixtures.insertUser("pb@example.com");
        String sessionId = startSessionAsUser(userA, "en");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId)
                        .header("Authorization", "Bearer " + userB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    // ---------------------------------------------------------------
    // T1.2 — Personalized question selection
    // (docs/review-and-readiness-engine.md "按用户薄弱点投放")
    //
    // Selection priority (highest first):
    //   1. Topics the user has an active MistakeRecord in (current cycle)
    //   2. Key topics (topics.is_key_topic=true) the user has not yet
    //      covered in the current learning cycle
    //   3. Everything else
    // Recency penalty: questions whose topic was the topic of one of the
    // user's last 2 answers in the session are deprioritized.
    // ---------------------------------------------------------------

    @Test
    void nextQuestion_userHasActiveMistakeInTopicA_returnsQuestionFromTopicA() throws Exception {
        // Setup: two non-key topics with one question each. User has an active
        // MistakeRecord in topic A. The default ID-asc tiebreaker would return
        // topic B's question (lower id, inserted earlier in setUp) but the
        // active-mistake topic must win.
        fixtures.truncateAll();
        Long topicA = fixtures.insertTopic("MISTAKE_TOPIC_A");
        Long topicB = fixtures.insertTopic("PLAIN_TOPIC_B");
        // Insert topicB's question FIRST so its id is lower than topicA's —
        // proves the personalization re-ranking beat pure ORDER BY id.
        Long qB = fixtures.insertQuestion(topicB, "A");
        fixtures.insertEnVariant(qB, "B-stem", "B-expl");
        Long qA = fixtures.insertQuestion(topicA, "A");
        fixtures.insertEnVariant(qA, "A-stem", "A-expl");

        Long uid = fixtures.insertUser("mistake_topicA@example.com");
        // User has an active mistake in topic A (from a prior, hypothetical
        // attempt — TestFixtures.insertMistakeRecord defaults learning_cycle=0
        // which matches a new user's reset_count).
        fixtures.insertMistakeRecord(uid, qA, topicA, 3, "practice");

        String sessionId = startSessionAsUser(uid, "en");

        // The first next_question is the one the session was created with.
        // Fetch it explicitly via /next-question for clarity.
        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId)
                        .header("Authorization", "Bearer " + uid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.question_id").value(qA.toString()));
    }

    @Test
    void nextQuestion_noMistakes_userMissingKeyTopicB_returnsQuestionFromTopicB() throws Exception {
        // Setup: non-key topic A and key topic B. No mistake records. User has
        // covered topic A (via a prior attempt in another session in the same
        // learning cycle). Even though qA's id is lower, the missing-key-topic
        // priority must pull qB to the front.
        fixtures.truncateAll();
        Long topicA = fixtures.insertTopic("PLAIN_A", "A", "A", false, 0);
        Long topicB = fixtures.insertTopic("KEY_B",   "B", "B", true,  0);
        Long qA = fixtures.insertQuestion(topicA, "A");
        fixtures.insertEnVariant(qA, "A-stem", "A-expl");
        Long qB = fixtures.insertQuestion(topicB, "A");
        fixtures.insertEnVariant(qB, "B-stem", "B-expl");

        Long uid = fixtures.insertUser("missing_keyB@example.com");

        // Simulate: user already answered something from topic A in a prior
        // completed session in the current learning cycle. Now topic B is
        // uncovered and topic A is covered.
        Long priorSession = fixtures.insertPracticeSession(uid, 0);
        Long vA = fixtures.insertVariantReturningId(qA, "zh", "A-zh", "[]", "expl");
        fixtures.insertPracticeAttempt(uid, priorSession, qA, vA, "A", true);

        String sessionId = startSessionAsUser(uid, "en");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId)
                        .header("Authorization", "Bearer " + uid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.question_id").value(qB.toString()));
    }

    @Test
    void nextQuestion_recentAnswerFromTopicA_returnsQuestionFromOtherTopic() throws Exception {
        // Setup: two non-key topics, multiple questions each. No mistake
        // records, no prior coverage. After the user answers a question from
        // topic A in this session, the *next* question should NOT be from
        // topic A (recency penalty) — even though topic A has the lowest-id
        // remaining question.
        fixtures.truncateAll();
        Long topicA = fixtures.insertTopic("RECENT_A");
        Long topicB = fixtures.insertTopic("RECENT_B");
        // Insert all topic-A questions first so they have the lowest ids; pure
        // ORDER BY id would serve a second topic-A question right after the
        // first answer.
        Long qA1 = fixtures.insertQuestion(topicA, "B");
        Long vA1 = fixtures.insertVariantReturningId(qA1, "en", "A1?",
                "[{\"key\":\"A\",\"text\":\"a\"},{\"key\":\"B\",\"text\":\"b\"}]", "x");
        Long qA2 = fixtures.insertQuestion(topicA, "B");
        fixtures.insertEnVariant(qA2, "A2?", "x");
        Long qB1 = fixtures.insertQuestion(topicB, "B");
        fixtures.insertEnVariant(qB1, "B1?", "x");

        String sessionId = startSessionAndGetId("en");
        // Answer qA1 — the most recent answer's topic is now A.
        submitAnswer(sessionId, qA1, vA1, "B");

        // The next question must be from topic B (recency penalty pushes the
        // remaining topic-A question down).
        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.question_id").value(qB1.toString()));
    }

    @Test
    void nextQuestion_freeTrialSession_onlyDrawsFromFreeTrialPool() throws Exception {
        // Regression: personalization must NOT widen the pool. A user with an
        // active mistake on a paid-only question in a free_trial session must
        // still only see free-trial questions, because allow_in_free_trial
        // restricts the pool BEFORE ranking.
        fixtures.truncateAll();
        Long topicA = fixtures.insertTopic("FT_TOPIC_A");
        Long topicB = fixtures.insertTopic("FT_TOPIC_B");

        // Paid-only question in topic A — eligible for personalization but
        // must be excluded from a free_trial session's pool.
        Long qPaid = fixtures.insertPaidOnlyQuestion(topicA, "A");
        fixtures.insertEnVariant(qPaid, "Paid stem", "x");
        // Free-trial question in topic B — should be the answer.
        Long qFree = fixtures.insertQuestion(topicB, "A");
        fixtures.insertEnVariant(qFree, "Free stem", "x");

        Long uid = fixtures.insertUser("ft_mistake@example.com");
        // Active mistake on the paid-only question — would win ranking if not
        // filtered out by the free-trial pool gate.
        fixtures.insertMistakeRecord(uid, qPaid, topicA, 5, "practice");

        String sessionId = startSessionAsUser(uid, "en");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId)
                        .header("Authorization", "Bearer " + uid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.question_id").value(qFree.toString()));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String startSessionAndGetId(String language) throws Exception {
        var result = mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"%s"}
                                """.formatted(language)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String key = "\"session_id\":\"";
        int start = body.indexOf(key) + key.length();
        int end   = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    private String startSessionAsUser(Long userId, String language) throws Exception {
        var result = mockMvc.perform(post("/api/v1/practice/sessions")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"%s"}
                                """.formatted(language)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String key = "\"session_id\":\"";
        int start = body.indexOf(key) + key.length();
        int end   = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    private String startFullSessionAsUser(Long userId, String language) throws Exception {
        var result = mockMvc.perform(post("/api/v1/practice/sessions")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"full","language":"%s"}
                                """.formatted(language)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String key = "\"session_id\":\"";
        int start = body.indexOf(key) + key.length();
        int end   = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    private void submitAnswer(String sessionId, Long qId, Long vId, String choice)
            throws Exception {
        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"question_id":"%s","variant_id":"%s","selected_choice_key":"%s"}
                        """.formatted(qId, vId, choice)));
    }

    // ===== /api/v1/practice/sessions/history =====

    @Test
    void getHistory_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/practice/sessions/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void getHistory_noSessions_returnsEmptyList() throws Exception {
        Long userId = fixtures.insertUser("hist@test.com");
        mockMvc.perform(get("/api/v1/practice/sessions/history")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessions").isArray())
                .andExpect(jsonPath("$.data.sessions", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.data.total_in_db").value(0));
    }

    @Test
    void getHistory_returnsSessionsWithStatsNewestFirst() throws Exception {
        Long userId = fixtures.insertUser("hist@test.com");
        Long topicId = fixtures.insertTopic("LANES", "Lane", "车道", false, 10);
        Long q1 = fixtures.insertQuestion(topicId, "A");
        Long v1 = fixtures.insertEnVariantReturningId(q1, "s1", "e1");
        Long q2 = fixtures.insertQuestion(topicId, "A");
        Long v2 = fixtures.insertEnVariantReturningId(q2, "s2", "e2");

        Long oldSession = fixtures.insertPracticeSession(userId, 0);
        fixtures.insertPracticeAttempt(userId, oldSession, q1, v1, "A", true);
        fixtures.insertPracticeAttempt(userId, oldSession, q2, v2, "B", false);

        Long newSession = fixtures.insertPracticeSession(userId, 0);
        fixtures.insertPracticeAttempt(userId, newSession, q1, v1, "A", true);

        mockMvc.perform(get("/api/v1/practice/sessions/history")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_in_db").value(2))
                .andExpect(jsonPath("$.data.sessions", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.sessions[0].session_id").value(newSession.toString()))
                .andExpect(jsonPath("$.data.sessions[0].answered_count").value(1))
                .andExpect(jsonPath("$.data.sessions[0].correct_count").value(1))
                .andExpect(jsonPath("$.data.sessions[0].accuracy_percent").value(100))
                .andExpect(jsonPath("$.data.sessions[1].session_id").value(oldSession.toString()))
                .andExpect(jsonPath("$.data.sessions[1].answered_count").value(2))
                .andExpect(jsonPath("$.data.sessions[1].correct_count").value(1))
                .andExpect(jsonPath("$.data.sessions[1].accuracy_percent").value(50));
    }

    @Test
    void getHistory_clampsLimitToServerMax() throws Exception {
        Long userId = fixtures.insertUser("hist@test.com");
        // limit=999 should be capped at server-side max of 50.
        mockMvc.perform(get("/api/v1/practice/sessions/history?limit=999")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk());
    }

    // ===== /api/v1/practice/sessions/stats =====

    @Test
    void getStats_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/practice/sessions/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStats_emptyAccount_returnsZeros() throws Exception {
        Long userId = fixtures.insertUser("stats@test.com");
        mockMvc.perform(get("/api/v1/practice/sessions/stats")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_sessions").value(0))
                .andExpect(jsonPath("$.data.total_questions_answered").value(0))
                .andExpect(jsonPath("$.data.total_correct").value(0))
                .andExpect(jsonPath("$.data.overall_accuracy_percent").value(0))
                .andExpect(jsonPath("$.data.active_mistakes_count").value(0))
                .andExpect(jsonPath("$.data.active_mistakes_topic_count").value(0));
    }

    @Test
    void getStats_withAttemptsAndMistakes_aggregatesCorrectly() throws Exception {
        Long userId = fixtures.insertUser("stats@test.com");
        Long topicId = fixtures.insertTopic("LANES", "Lane", "车道", false, 10);
        Long q1 = fixtures.insertQuestion(topicId, "A");
        Long v1 = fixtures.insertEnVariantReturningId(q1, "s1", "e1");
        Long q2 = fixtures.insertQuestion(topicId, "A");
        Long v2 = fixtures.insertEnVariantReturningId(q2, "s2", "e2");

        Long sessionId = fixtures.insertPracticeSession(userId, 0);
        fixtures.insertPracticeAttempt(userId, sessionId, q1, v1, "A", true);
        fixtures.insertPracticeAttempt(userId, sessionId, q2, v2, "B", false);

        // Active mistake (is_active default = TRUE, learning_cycle default = 0)
        fixtures.insertMistakeRecord(userId, q2, topicId, 1, "practice");

        mockMvc.perform(get("/api/v1/practice/sessions/stats")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_sessions").value(1))
                .andExpect(jsonPath("$.data.total_questions_answered").value(2))
                .andExpect(jsonPath("$.data.total_correct").value(1))
                .andExpect(jsonPath("$.data.overall_accuracy_percent").value(50))
                .andExpect(jsonPath("$.data.active_mistakes_count").value(1))
                .andExpect(jsonPath("$.data.active_mistakes_topic_count").value(1));
    }
}
