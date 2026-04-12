package com.dmvmotor.api.mistakereview.review.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.content.domain.Choice;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.mistakereview.review.application.ReviewService;
import com.dmvmotor.api.mistakereview.review.application.ReviewService.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/pack")
    public ApiResponse<?> getReviewPack(@CurrentUser Long userId) {
        requireAuth(userId);
        ReviewPackResult pack = reviewService.getOrCreatePack(userId);
        return ApiResponse.ok(Map.of(
                "reviewPackId", String.valueOf(pack.packId()),
                "status",       pack.status(),
                "tasks",        pack.tasks().stream().map(this::toTaskDto).toList()
        ));
    }

    @GetMapping("/tasks/{id}/questions")
    public ApiResponse<?> getTaskQuestions(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "en") String language
    ) {
        requireAuth(userId);
        TaskQuestionsResult result = reviewService.getTaskQuestions(id, language);
        return ApiResponse.ok(Map.of(
                "reviewTaskId", String.valueOf(result.taskId()),
                "taskType",     result.taskType(),
                "topicId",      String.valueOf(result.topicId()),
                "questions",    result.questions().stream().map(this::toQuestionDto).toList()
        ));
    }

    @PostMapping("/tasks/{id}/answers")
    public ApiResponse<?> submitAnswer(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @Valid @RequestBody AnswerRequest req
    ) {
        requireAuth(userId);
        ReviewAnswerResult result = reviewService.submitAnswer(
                id, userId,
                Long.parseLong(req.questionId()),
                Long.parseLong(req.variantId()),
                req.selectedChoiceKey());
        return ApiResponse.ok(Map.of(
                "questionId",       String.valueOf(result.questionId()),
                "isCorrect",        result.isCorrect(),
                "correctChoiceKey", result.correctChoiceKey(),
                "explanation",      result.explanation() != null ? result.explanation() : "",
                "taskProgress",     Map.of(
                        "answeredCount", result.answeredCount(),
                        "targetCount",  result.targetCount()
                )
        ));
    }

    @PostMapping("/tasks/{id}/complete")
    public ApiResponse<?> completeTask(
            @CurrentUser Long userId,
            @PathVariable Long id
    ) {
        requireAuth(userId);
        CompleteTaskResult result = reviewService.completeTask(id);
        return ApiResponse.ok(Map.of(
                "reviewTaskId", String.valueOf(result.taskId()),
                "completed",    result.completed()
        ));
    }

    // ---------------------------------------------------------------
    // DTOs
    // ---------------------------------------------------------------

    record AnswerRequest(
            @NotBlank String question_id,
            @NotBlank String variant_id,
            @NotBlank String selected_choice_key
    ) {
        String questionId()       { return question_id; }
        String variantId()        { return variant_id; }
        String selectedChoiceKey(){ return selected_choice_key; }
    }

    private Map<String, Object> toTaskDto(TaskSummary t) {
        return Map.of(
                "reviewTaskId", String.valueOf(t.taskId()),
                "topicId",      String.valueOf(t.topicId()),
                "type",         t.type(),
                "status",       t.status(),
                "priority",     t.priority()
        );
    }

    private Map<String, Object> toQuestionDto(QuestionDetail q) {
        return Map.of(
                "questionId", String.valueOf(q.questionId()),
                "variantId",  String.valueOf(q.variantId()),
                "stem",       q.stem(),
                "choices",    q.choices()
        );
    }

    private void requireAuth(Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
    }
}
