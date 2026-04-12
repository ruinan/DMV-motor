package com.dmvmotor.api.it;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;

/**
 * Base class for E2E integration tests running against local Docker PostgreSQL.
 *
 * Uses the "it" Spring profile → application-it.yml → dmv_motor_it database.
 * Flyway auto-applies all migrations (V1–V5) including V2 seed data.
 *
 * Each subclass MUST call cleanupTestUser() in @AfterAll or @AfterEach
 * to remove test-specific users (cascade-deletes all related rows).
 *
 * Run: mvn verify   (Failsafe picks up *IT.java files)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
public abstract class E2ETestBase {

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port    = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = "";
    }

    // ---------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------

    /** Creates a test user and returns their id. */
    protected Long createTestUser(String email) {
        return jdbc.queryForObject(
                "INSERT INTO users (email, language_preference) VALUES (?, 'en') RETURNING id",
                Long.class, email);
    }

    /** Cascade-deletes the user and all their data (sessions, mistakes, attempts, etc.). */
    protected void cleanupTestUser(Long userId) {
        if (userId == null) return;
        // Cascade: access_passes, practice_sessions → practice_attempts,
        //          mistake_records, review_packs → review_tasks → review_task_questions,
        //          mock_attempts → mock_attempt_results
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
    }

    /** Authenticated request spec. Dev-mode auth: Bearer <userId>. */
    protected RequestSpecification asUser(Long userId) {
        return given()
                .header("Authorization", "Bearer " + userId)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    /** Anonymous request spec. */
    protected RequestSpecification anon() {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    protected Response post(RequestSpecification spec, String path, String body) {
        return spec.body(body).post(path);
    }

    protected Response get(RequestSpecification spec, String path) {
        return spec.get(path);
    }
}
