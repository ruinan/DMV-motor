package com.dmvmotor.api.mockexam.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.mockexam.application.MockExamService;
import com.dmvmotor.api.mockexam.application.MockExamService.*;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.WeakTopicRow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mock-exams")
public class MockExamController {

    private final MockExamService mockExamService;

    public MockExamController(MockExamService mockExamService) {
        this.mockExamService = mockExamService;
    }

    @GetMapping("/access")
    public ApiResponse<?> getMockAccess(@CurrentUser Long userId) {
        requireAuth(userId);
        MockAccessResult result = mockExamService.checkAccess(userId);
        return ApiResponse.ok(Map.of(
                "allowed",        result.allowed(),
                "mock_remaining", result.mockRemaining(),
                "reason",         result.reason() != null ? result.reason() : ""
        ));
    }

    @PostMapping("/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> startAttempt(
            @CurrentUser Long userId,
            @Valid @RequestBody StartRequest req
    ) {
        requireAuth(userId);
        StartAttemptResult result = mockExamService.startAttempt(
                userId, req.language() != null ? req.language() : "en");
        return ApiResponse.ok(Map.of(
                "mock_attempt_id",             String.valueOf(result.attemptId()),
                "status",                      result.status(),
                "mock_remaining_after_start",  result.mockRemainingAfterStart(),
                "questions", result.questions().stream().map(this::toQuestionDto).toList()
        ));
    }

    @PostMapping("/attempts/{id}/answers")
    public ApiResponse<?> saveAnswer(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @Valid @RequestBody AnswerRequest req
    ) {
        requireAuth(userId);
        SaveAnswerResult result = mockExamService.saveAnswer(
                id, userId,
                Long.parseLong(req.questionId()),
                Long.parseLong(req.variantId()),
                req.selectedKey());
        return ApiResponse.ok(Map.of(
                "saved",          result.saved(),
                "answered_count", result.answeredCount()
        ));
    }

    @PostMapping("/attempts/{id}/submit")
    public ApiResponse<?> submitAttempt(
            @CurrentUser Long userId,
            @PathVariable Long id
    ) {
        requireAuth(userId);
        SubmitResult result = mockExamService.submitAttempt(id, userId);
        return ApiResponse.ok(Map.of(
                "mock_attempt_id", String.valueOf(result.attemptId()),
                "status",          result.status(),
                "score_percent",   result.scorePercent(),
                "correct_count",   result.correctCount(),
                "wrong_count",     result.wrongCount(),
                "weak_topics",     result.weakTopics().stream()
                        .map(t -> Map.of(
                                "topic_id", String.valueOf(t.topicId()),
                                "label",    t.label()))
                        .toList(),
                "next_action",     result.nextAction()
        ));
    }

    @PostMapping("/attempts/{id}/exit")
    public ApiResponse<?> exitAttempt(
            @CurrentUser Long userId,
            @PathVariable Long id
    ) {
        requireAuth(userId);
        ExitResult result = mockExamService.exitAttempt(id, userId);
        return ApiResponse.ok(Map.of(
                "mock_attempt_id", String.valueOf(result.attemptId()),
                "status",          result.status(),
                "quota_consumed",  result.quotaConsumed(),
                "answered_count",  result.answeredCount()
        ));
    }

    // ---------------------------------------------------------------
    // DTOs
    // ---------------------------------------------------------------

    record StartRequest(String language) {}

    record AnswerRequest(
            @NotBlank String question_id,
            @NotBlank String variant_id,
            @NotBlank String selected_choice_key
    ) {
        String questionId()  { return question_id; }
        String variantId()   { return variant_id; }
        String selectedKey() { return selected_choice_key; }
    }

    private Map<String, Object> toQuestionDto(QuestionDetail q) {
        return Map.of(
                "question_id", String.valueOf(q.questionId()),
                "variant_id",  String.valueOf(q.variantId()),
                "stem",        q.stem(),
                "choices",     q.choices()
        );
    }

    private void requireAuth(Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
    }
}
