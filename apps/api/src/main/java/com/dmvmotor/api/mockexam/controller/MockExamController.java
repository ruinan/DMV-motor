package com.dmvmotor.api.mockexam.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.common.Ids;
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

    @GetMapping("/attempts/{id}")
    public ApiResponse<?> getAttempt(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam(required = false) String language
    ) {
        requireAuth(userId);
        var result = mockExamService.getAttemptDetail(id, userId, language);
        return ApiResponse.ok(Map.of(
                "mock_attempt_id", String.valueOf(result.attemptId()),
                "mock_exam_id",    String.valueOf(result.mockExamId()),
                "status",          result.status(),
                "language",        result.language(),
                "questions",       result.questions().stream().map(this::toQuestionDto).toList(),
                "saved_answers",   result.savedAnswers().stream()
                        .map(a -> Map.of(
                                "question_id",         String.valueOf(a.questionId()),
                                "selected_choice_key", a.selectedKey()))
                        .toList()
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
                Ids.parse(req.questionId(), "question_id"),
                Ids.parse(req.variantId(), "variant_id"),
                req.selectedKey());
        return ApiResponse.ok(Map.ofEntries(
                Map.entry("saved",              result.saved()),
                Map.entry("answered_count",     result.answeredCount()),
                Map.entry("is_correct",         result.isCorrect()),
                Map.entry("correct_choice_key", result.correctChoiceKey()),
                Map.entry("wrong_count",        result.wrongCountSoFar()),
                Map.entry("max_allowed_wrong",  result.maxAllowedWrong()),
                Map.entry("should_terminate",   result.shouldTerminate())
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

    @GetMapping("/attempts/history")
    public ApiResponse<?> listAttemptHistory(
            @CurrentUser Long userId,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        requireAuth(userId);
        var result = mockExamService.listHistory(userId, limit);
        var attempts = result.attempts().stream().map(r -> Map.ofEntries(
                Map.entry("attempt_id",      String.valueOf(r.id())),
                Map.entry("mock_exam_id",    String.valueOf(r.mockExamId())),
                Map.entry("mock_exam_code",  r.mockExamCode()),
                Map.entry("status",          r.status()),
                Map.entry("score_percent",   r.scorePercent() == null ? -1 : r.scorePercent()),
                Map.entry("correct_count",   r.correctCount() == null ? 0 : r.correctCount()),
                Map.entry("answered_count",  r.answeredCount()),
                Map.entry("started_at",      r.startedAt().toString()),
                Map.entry("submitted_at",    r.submittedAt() != null ? r.submittedAt().toString() : "")
        )).toList();
        return ApiResponse.ok(Map.of(
                "attempts",    attempts,
                "total_in_db", result.totalInDb()
        ));
    }

    @GetMapping("/attempts/stats")
    public ApiResponse<?> getAttemptStats(@CurrentUser Long userId) {
        requireAuth(userId);
        var s = mockExamService.getStats(userId);
        return ApiResponse.ok(Map.of(
                "total_attempts",               s.totalAttempts(),
                "submitted_count",              s.submittedCount(),
                "exited_count",                 s.exitedCount(),
                "recent_3_avg_score_percent",   s.recent3AvgScorePercent() == null ? -1 : s.recent3AvgScorePercent(),
                "best_score_percent",           s.bestScorePercent() == null ? -1 : s.bestScorePercent(),
                "latest_score_percent",         s.latestScorePercent() == null ? -1 : s.latestScorePercent()
        ));
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
