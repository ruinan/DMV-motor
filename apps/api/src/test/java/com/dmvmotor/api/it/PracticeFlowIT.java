package com.dmvmotor.api.it;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * E2E practice session flow against seed data (V2 + V10 real questions).
 * Creates a unique test user per run and cleans up after.
 *
 * NOTE on field names: controllers respond with snake_case (Jackson
 * property-naming-strategy is SNAKE_CASE in application.yml), so RestAssured
 * JSON paths use snake_case here even though Java fields use camelCase.
 */
class PracticeFlowIT extends E2ETestBase {

    private Long userId;

    @BeforeEach
    void createUser() {
        userId = createTestUser("it-practice-" + System.currentTimeMillis() + "@test.com");
    }

    @AfterEach
    void cleanup() {
        cleanupTestUser(userId);
    }

    @Test
    void anonymousPractice_freeTrial_completesFullFlow() {
        // 1. Start anonymous session
        String sessionBody = given()
                .contentType("application/json")
                .accept("application/json")
                .body("""
                        {"entry_type":"free_trial","language":"en"}
                        """)
                .post("/api/v1/practice/sessions")
                .then()
                .statusCode(201)
                .body("data.status", equalTo("in_progress"))
                .body("data.next_question.question_id", notNullValue())
                .body("data.next_question.choices", hasSize(greaterThan(0)))
                .extract().asString();

        String sessionId = extractField(sessionBody, "session_id");
        String questionId = extractNestedField(sessionBody, "next_question", "question_id");
        String variantId  = extractNestedField(sessionBody, "next_question", "variant_id");

        // 2. Get next question
        given().accept("application/json")
                .get("/api/v1/practice/sessions/" + sessionId + "/next-question")
                .then()
                .statusCode(200)
                .body("data.question_id", equalTo(questionId))
                .body("data.progress.answered_count", equalTo(0));

        // 3. Submit an answer (pick first choice key — we don't know correct, just testing flow)
        given()
                .contentType("application/json")
                .accept("application/json")
                .body("""
                        {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                        """.formatted(questionId, variantId))
                .post("/api/v1/practice/sessions/" + sessionId + "/answers")
                .then()
                .statusCode(200)
                .body("data.question_id", equalTo(questionId))
                .body("data.is_correct", notNullValue())
                .body("data.correct_choice_key", notNullValue())
                .body("data.progress.answered_count", equalTo(1));

        // 4. Get session status
        given().accept("application/json")
                .get("/api/v1/practice/sessions/" + sessionId)
                .then()
                .statusCode(200)
                .body("data.status", equalTo("in_progress"))
                .body("data.answered_count", equalTo(1))
                .body("data.total_count", greaterThanOrEqualTo(1));

        // 5. Complete session
        given().accept("application/json")
                .post("/api/v1/practice/sessions/" + sessionId + "/complete")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("completed"));
    }

    @Test
    void userPractice_wrongAnswer_recordsMistake_visibleInMistakeList() {
        // entry_type=full requires an active access pass; fresh test users default to free_trial.
        grantActiveAccessPass(userId);

        // 1. Start session
        String sessionBody = given()
                .contentType("application/json")
                .accept("application/json")
                .body("""
                        {"entry_type":"full","language":"en"}
                        """)
                .header("Authorization", "Bearer " + userId)
                .post("/api/v1/practice/sessions")
                .then()
                .statusCode(201)
                .extract().asString();

        String sessionId  = extractField(sessionBody, "session_id");
        String questionId = extractNestedField(sessionBody, "next_question", "question_id");
        String variantId  = extractNestedField(sessionBody, "next_question", "variant_id");

        // Fetch the correct answer so we can intentionally answer WRONG
        String questionBody = given().accept("application/json")
                .queryParam("language", "en")
                .get("/api/v1/questions/" + questionId)
                .then().extract().asString();
        String correctKey = extractField(questionBody, "correct_choice_key");
        String wrongKey   = "A".equals(correctKey) ? "B" : "A";

        // 2. Submit wrong answer
        given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "Bearer " + userId)
                .body("""
                        {"question_id":"%s","variant_id":"%s","selected_choice_key":"%s"}
                        """.formatted(questionId, variantId, wrongKey))
                .post("/api/v1/practice/sessions/" + sessionId + "/answers")
                .then()
                .statusCode(200)
                .body("data.is_correct", is(false));

        // 3. Mistake should appear in list
        given().accept("application/json")
                .header("Authorization", "Bearer " + userId)
                .get("/api/v1/mistakes")
                .then()
                .statusCode(200)
                .body("data.items", hasSize(1))
                .body("data.items[0].question_id", equalTo(questionId))
                .body("meta.total", equalTo(1));
    }

    @Test
    void accountFlow_getMe_updateLanguage() {
        // GET /me
        asUser(userId)
                .get("/api/v1/me")
                .then()
                .statusCode(200)
                .body("data.user_id", equalTo(String.valueOf(userId)))
                .body("data.language", equalTo("en"))
                .body("data.access.state", equalTo("free_trial"));

        // PUT /me/language
        asUser(userId)
                .body("""
                        {"language":"zh"}
                        """)
                .put("/api/v1/me/language")
                .then()
                .statusCode(200)
                .body("data.language", equalTo("zh"));

        // Verify change persisted
        asUser(userId)
                .get("/api/v1/me")
                .then()
                .body("data.language", equalTo("zh"));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Inserts an active access pass for the given user (expires in 30 days, 5 mock attempts). */
    private void grantActiveAccessPass(Long userId) {
        jdbc.update("""
                INSERT INTO access_passes
                    (user_id, status, starts_at, expires_at, mock_exam_total_count, mock_exam_used_count)
                VALUES
                    (?, 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days', 5, 0)
                """, userId);
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return "";
        start += key.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "";
    }

    private String extractNestedField(String json, String parent, String field) {
        int parentStart = json.indexOf("\"" + parent + "\"");
        if (parentStart < 0) return "";
        String sub = json.substring(parentStart);
        return extractField(sub, field);
    }
}
