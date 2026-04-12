package com.dmvmotor.api.it;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * E2E tests for content APIs against seed data (V2 migration).
 * No user required — public endpoints.
 */
class ContentIT extends E2ETestBase {

    @Test
    void listTopics_seedData_returns8Topics() {
        given().accept("application/json")
                .get("/api/v1/topics")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.items", hasSize(greaterThanOrEqualTo(8)));
    }

    @Test
    void getQuestion_seedDataQuestion1_returnsDetails() {
        // First fetch topics to get a valid topic, then a question
        // Use question id=1 which exists after V2 seed
        given().accept("application/json")
                .queryParam("language", "en")
                .get("/api/v1/questions/1")
                .then()
                .statusCode(200)
                .body("data.questionId", equalTo("1"))
                .body("data.stem", notNullValue())
                .body("data.choices", hasSize(greaterThan(0)))
                .body("data.language", equalTo("en"));
    }

    @Test
    void getQuestion_chineseVariant_returnsZhStem() {
        given().accept("application/json")
                .queryParam("language", "zh")
                .get("/api/v1/questions/1")
                .then()
                .statusCode(200)
                .body("data.language", equalTo("zh"))
                .body("data.stem", notNullValue());
    }

    @Test
    void getQuestion_unknownId_returns404() {
        given().accept("application/json")
                .get("/api/v1/questions/999999")
                .then()
                .statusCode(404)
                .body("error.code", equalTo("RESOURCE_NOT_FOUND"));
    }
}
