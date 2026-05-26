package com.dmvmotor.api.practice.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.common.Ids;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.practice.application.PracticeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/practice/sessions")
public class PracticeSessionController {

    private final PracticeService practiceService;

    public PracticeSessionController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> startSession(@CurrentUser Long userId,
                                        @Valid @RequestBody StartRequest req) {
        var result = practiceService.startSession(userId, req.entryType(),
                req.language() != null ? req.language() : "en");
        return ApiResponse.ok(Map.of(
                "session_id",    String.valueOf(result.sessionId()),
                "entry_type",    result.entryType(),
                "status",        result.status(),
                "language",      result.language(),
                "next_question", toQuestionDto(result.nextQuestion())
        ));
    }

    @GetMapping("/{id}/next-question")
    public ApiResponse<?> nextQuestion(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam(required = false) String language
    ) {
        QuestionDetail q = practiceService.getNextQuestion(id, userId, language);
        int answered = practiceService.getSessionStatus(id, userId).answeredCount();
        return ApiResponse.ok(Map.of(
                "question_id", String.valueOf(q.questionId()),
                "variant_id",  String.valueOf(q.variantId()),
                "stem",        q.stem(),
                "choices",     q.choices(),
                "progress",    Map.of("answered_count", answered)
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getSession(@CurrentUser Long userId, @PathVariable Long id) {
        var status = practiceService.getSessionStatus(id, userId);
        return ApiResponse.ok(Map.of(
                "session_id",    String.valueOf(status.sessionId()),
                "status",        status.status(),
                "answered_count", status.answeredCount(),
                "total_count",   status.totalCount()
        ));
    }

    @PostMapping("/{id}/answers")
    public ApiResponse<?> submitAnswer(@CurrentUser Long userId,
                                        @PathVariable Long id,
                                        @Valid @RequestBody AnswerRequest req) {
        var result = practiceService.submitAnswer(id, userId,
                Ids.parse(req.questionId(), "question_id"),
                Ids.parse(req.variantId(), "variant_id"),
                req.selectedChoiceKey());
        return ApiResponse.ok(Map.of(
                "question_id",        String.valueOf(result.questionId()),
                "is_correct",         result.isCorrect(),
                "correct_choice_key", result.correctChoiceKey(),
                "explanation",        result.explanation() != null ? result.explanation() : "",
                "progress",           Map.of("answered_count", result.answeredCount())
        ));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<?> completeSession(@CurrentUser Long userId, @PathVariable Long id) {
        var result = practiceService.completeSession(id, userId);
        return ApiResponse.ok(Map.of(
                "session_id", String.valueOf(result.sessionId()),
                "status",     result.status()
        ));
    }

    @GetMapping("/{id}/attempts")
    public ApiResponse<?> listAttempts(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam(required = false) String language
    ) {
        var attempts = practiceService.listAttempts(id, userId, language);
        var items = attempts.stream().map(a -> Map.ofEntries(
                Map.entry("question_id",         String.valueOf(a.questionId())),
                Map.entry("variant_id",          String.valueOf(a.variantId())),
                Map.entry("topic_id",            String.valueOf(a.topicId())),
                Map.entry("language",            a.language()),
                Map.entry("stem",                a.stem()),
                Map.entry("choices",             a.choices()),
                Map.entry("correct_choice_key",  a.correctChoiceKey()),
                Map.entry("selected_choice_key", a.selectedChoiceKey()),
                Map.entry("explanation",         a.explanation() != null ? a.explanation() : ""),
                Map.entry("is_correct",          a.isCorrect()),
                Map.entry("submitted_at",        a.submittedAt().toString())
        )).toList();
        return ApiResponse.okWithMeta(Map.of("items", items),
                Map.of("total", items.size()));
    }

    @GetMapping("/history")
    public ApiResponse<?> listHistory(
            @CurrentUser Long userId,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        if (userId == null) {
            throw new com.dmvmotor.api.common.BusinessException("UNAUTHORIZED",
                    "Authentication required", HttpStatus.UNAUTHORIZED);
        }
        var result = practiceService.listHistory(userId, limit);
        var sessions = result.sessions().stream().map(r -> {
            int accuracy = r.answeredCount() == 0
                    ? 0
                    : (int) Math.round(r.correctCount() * 100.0 / r.answeredCount());
            return Map.ofEntries(
                    Map.entry("session_id",       String.valueOf(r.id())),
                    Map.entry("entry_type",       r.entryType()),
                    Map.entry("language",         r.languageCode()),
                    Map.entry("status",           r.status()),
                    Map.entry("started_at",       r.startedAt().toString()),
                    Map.entry("completed_at",     r.completedAt() != null ? r.completedAt().toString() : ""),
                    Map.entry("answered_count",   r.answeredCount()),
                    Map.entry("correct_count",    r.correctCount()),
                    Map.entry("accuracy_percent", accuracy)
            );
        }).toList();
        return ApiResponse.ok(Map.of(
                "sessions",    sessions,
                "total_in_db", result.totalInDb()
        ));
    }

    @GetMapping("/stats")
    public ApiResponse<?> getStats(@CurrentUser Long userId) {
        if (userId == null) {
            throw new com.dmvmotor.api.common.BusinessException("UNAUTHORIZED",
                    "Authentication required", HttpStatus.UNAUTHORIZED);
        }
        var s = practiceService.getStats(userId);
        return ApiResponse.ok(Map.of(
                "total_sessions",             s.totalSessions(),
                "total_questions_answered",   s.totalQuestionsAnswered(),
                "total_correct",              s.totalCorrect(),
                "overall_accuracy_percent",   s.overallAccuracyPercent(),
                "active_mistakes_count",      s.activeMistakesCount(),
                "active_mistakes_topic_count", s.activeMistakesTopicCount()
        ));
    }

    // ---------------------------------------------------------------
    // DTOs
    // ---------------------------------------------------------------

    record StartRequest(
            @NotBlank(message = "must not be blank") String entry_type,
            String language
    ) {
        String entryType() { return entry_type; }
    }

    record AnswerRequest(
            @NotBlank String question_id,
            @NotBlank String variant_id,
            @NotBlank String selected_choice_key
    ) {
        String questionId()      { return question_id; }
        String variantId()       { return variant_id; }
        String selectedChoiceKey() { return selected_choice_key; }
    }

    private Map<String, Object> toQuestionDto(QuestionDetail q) {
        return Map.of(
                "question_id", String.valueOf(q.questionId()),
                "variant_id",  String.valueOf(q.variantId()),
                "stem",        q.stem(),
                "choices",     q.choices()
        );
    }
}
