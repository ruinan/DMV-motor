package com.dmvmotor.api.practice.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.CurrentUser;
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
    public ApiResponse<?> nextQuestion(@CurrentUser Long userId, @PathVariable Long id) {
        QuestionDetail q = practiceService.getNextQuestion(id, userId);
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
                Long.parseLong(req.questionId()),
                Long.parseLong(req.variantId()),
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
