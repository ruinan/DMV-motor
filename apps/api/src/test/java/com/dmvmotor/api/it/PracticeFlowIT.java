package com.dmvmotor.api.it;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * E2E practice session flow against seed data (46 real questions from V2).
 * Creates a unique test user per run and cleans up after.
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
                .body("data.nextQuestion.questionId", notNullValue())
                .body("data.nextQuestion.choices", hasSize(greaterThan(0)))
                .extract().asString();

        String sessionId = extractField(sessionBody, "sessionId");
        String questionId = extractNestedField(sessionBody, "nextQuestion", "questionId");
        String variantId  = extractNestedField(sessionBody, "nextQuestion", "variantId");

        // 2. Get next question
        given().accept("application/json")
                .get("/api/v1/practice/sessions/" + sessionId + "/next-question")
                .then()
                .statusCode(200)
                .body("data.questionId", equalTo(questionId))
                .body("data.progress.answeredCount", equalTo(0));

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
                .body("data.questionId", equalTo(questionId))
                .body("data.isCorrect", notNullValue())
                .body("data.correctChoiceKey", notNullValue())
                .body("data.progress.answeredCount", equalTo(1));

        // 4. Get session status
        given().accept("application/json")
                .get("/api/v1/practice/sessions/" + sessionId)
                .then()
                .statusCode(200)
                .body("data.status", equalTo("in_progress"))
                .body("data.answeredCount", equalTo(1))
                .body("data.totalCount", greaterThanOrEqualTo(1));

        // 5. Complete session
        given().accept("application/json")
                .post("/api/v1/practice/sessions/" + sessionId + "/complete")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("completed"));
    }

    @Test
    void userPractice_wrongAnswer_recordsMistake_visibleInMistakeList() {
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

        String sessionId  = extractField(sessionBody, "sessionId");
        String questionId = extractNestedField(sessionBody, "nextQuestion", "questionId");
        String variantId  = extractNestedField(sessionBody, "nextQuestion", "variantId");

        // Fetch the correct answer so we can intentionally answer WRONG
        String questionBody = given().accept("application/json")
                .queryParam("language", "en")
                .get("/api/v1/questions/" + questionId)
                .then().extract().asString();
        String correctKey = extractField(questionBody, "correctChoiceKey");
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
                .body("data.isCorrect", is(false));

        // 3. Mistake should appear in list
        given().accept("application/json")
                .header("Authorization", "Bearer " + userId)
                .get("/api/v1/mistakes")
                .then()
                .statusCode(200)
                .body("data.items", hasSize(1))
                .body("data.items[0].questionId", equalTo(questionId))
                .body("meta.total", equalTo(1));
    }

    @Test
    void accountFlow_getMe_updateLanguage() {
        // GET /me
        asUser(userId)
                .get("/api/v1/me")
                .then()
                .statusCode(200)
                .body("data.userId", equalTo(String.valueOf(userId)))
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
