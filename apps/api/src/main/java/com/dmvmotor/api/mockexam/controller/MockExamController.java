package com.dmvmotor.api.mockexam.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.common.Ids;
import com.dmvmotor.api.content.domain.Choice;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.mockexam.application.MockExamService;
import com.dmvmotor.api.mockexam.application.MockExamService.*;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.AnswerDetail;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.WeakTopicRow;
import com.dmvmotor.api.mockexam.infrastructure.MockHistoryDao.AttemptHistoryRow;
import com.dmvmotor.api.mockexam.infrastructure.MockHistoryDao.AttemptStats;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        return ApiResponse.ok(MockAccessDto.from(mockExamService.checkAccess(userId)));
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
        return ApiResponse.ok(StartAttemptDto.from(result));
    }

    @GetMapping("/attempts/{id}")
    public ApiResponse<?> getAttempt(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam(required = false) String language
    ) {
        requireAuth(userId);
        return ApiResponse.ok(
                AttemptDetailDto.from(mockExamService.getAttemptDetail(id, userId, language)));
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
        // Clock ran out: the service already finalized the attempt as a timeout
        // (committed). Signal the client to refetch into the result view.
        if (result.expired()) {
            throw new BusinessException("MOCK_EXPIRED",
                    "Time is up — the exam has ended", HttpStatus.CONFLICT);
        }
        return ApiResponse.ok(SaveAnswerDto.from(result));
    }

    @PostMapping("/attempts/{id}/submit")
    public ApiResponse<?> submitAttempt(
            @CurrentUser Long userId,
            @PathVariable Long id
    ) {
        requireAuth(userId);
        return ApiResponse.ok(SubmitDto.from(mockExamService.submitAttempt(id, userId)));
    }

    @PostMapping("/attempts/{id}/exit")
    public ApiResponse<?> exitAttempt(
            @CurrentUser Long userId,
            @PathVariable Long id
    ) {
        requireAuth(userId);
        return ApiResponse.ok(ExitDto.from(mockExamService.exitAttempt(id, userId)));
    }

    @GetMapping("/attempts/history")
    public ApiResponse<?> listAttemptHistory(
            @CurrentUser Long userId,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        requireAuth(userId);
        var result = mockExamService.listHistory(userId, limit);
        var attempts = result.attempts().stream().map(AttemptHistoryItemDto::from).toList();
        return ApiResponse.ok(new AttemptHistoryDto(attempts, result.totalInDb()));
    }

    @GetMapping("/attempts/stats")
    public ApiResponse<?> getAttemptStats(@CurrentUser Long userId) {
        requireAuth(userId);
        return ApiResponse.ok(AttemptStatsDto.from(mockExamService.getStats(userId)));
    }

    private void requireAuth(Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    // ---------------------------------------------------------------
    // Request DTOs
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

    // ---------------------------------------------------------------
    // Response DTOs — explicit records (snake_case via Jackson) so the wire
    // contract is type-checked instead of stringly-keyed inline maps. Each
    // `from` factory owns the id-stringification and null-defaulting (-1 for
    // "no score", 0 for counts, "" for absent strings) the old Map.of did.
    // ---------------------------------------------------------------

    record QuestionDto(String questionId, String variantId, String stem, List<Choice> choices) {
        static QuestionDto from(QuestionDetail q) {
            return new QuestionDto(String.valueOf(q.questionId()), String.valueOf(q.variantId()),
                    q.stem(), q.choices());
        }
    }

    record MockAccessDto(boolean allowed, int mockRemaining, String reason) {
        static MockAccessDto from(MockAccessResult r) {
            return new MockAccessDto(r.allowed(), r.mockRemaining(),
                    r.reason() != null ? r.reason() : "");
        }
    }

    record StartAttemptDto(String mockAttemptId, String status, int mockRemainingAfterStart,
                           List<QuestionDto> questions) {
        static StartAttemptDto from(StartAttemptResult r) {
            return new StartAttemptDto(String.valueOf(r.attemptId()), r.status(),
                    r.mockRemainingAfterStart(),
                    r.questions().stream().map(QuestionDto::from).toList());
        }
    }

    record SavedAnswerDto(String questionId, String selectedChoiceKey, String correctChoiceKey,
                          boolean isCorrect, String explanation) {
        static SavedAnswerDto from(AnswerDetail a) {
            return new SavedAnswerDto(String.valueOf(a.questionId()), a.selectedKey(),
                    a.correctKey() != null ? a.correctKey() : "",
                    a.isCorrect() != null ? a.isCorrect() : false,
                    a.explanation() != null ? a.explanation() : "");
        }
    }

    record AttemptDetailDto(String mockAttemptId, String mockExamId, String status, String language,
                            int scorePercent, int correctCount, int wrongCount,
                            int timeLimitSeconds, String startedAt, int timeUsedSeconds,
                            List<QuestionDto> questions, List<SavedAnswerDto> savedAnswers) {
        static AttemptDetailDto from(AttemptDetailResult r) {
            return new AttemptDetailDto(
                    String.valueOf(r.attemptId()), String.valueOf(r.mockExamId()),
                    r.status(), r.language(),
                    r.scorePercent() == null ? -1 : r.scorePercent(),
                    r.correctCount() == null ? 0 : r.correctCount(),
                    r.wrongCount() == null ? 0 : r.wrongCount(),
                    r.timeLimitSeconds(), r.startedAt().toString(),
                    r.timeUsedSeconds() == null ? -1 : r.timeUsedSeconds(),
                    r.questions().stream().map(QuestionDto::from).toList(),
                    r.savedAnswers().stream().map(SavedAnswerDto::from).toList());
        }
    }

    record SaveAnswerDto(boolean saved, int answeredCount, boolean isCorrect,
                         String correctChoiceKey, int wrongCount, int maxAllowedWrong,
                         boolean shouldTerminate) {
        static SaveAnswerDto from(SaveAnswerResult r) {
            return new SaveAnswerDto(r.saved(), r.answeredCount(), r.isCorrect(),
                    r.correctChoiceKey(), r.wrongCountSoFar(), r.maxAllowedWrong(),
                    r.shouldTerminate());
        }
    }

    record WeakTopicDto(String topicId, String label) {
        static WeakTopicDto from(WeakTopicRow t) {
            return new WeakTopicDto(String.valueOf(t.topicId()), t.label());
        }
    }

    record SubmitDto(String mockAttemptId, String status, int scorePercent, int correctCount,
                     int wrongCount, List<WeakTopicDto> weakTopics, Map<String, String> nextAction) {
        static SubmitDto from(SubmitResult r) {
            return new SubmitDto(String.valueOf(r.attemptId()), r.status(), r.scorePercent(),
                    r.correctCount(), r.wrongCount(),
                    r.weakTopics().stream().map(WeakTopicDto::from).toList(),
                    r.nextAction());
        }
    }

    record ExitDto(String mockAttemptId, String status, boolean quotaConsumed, int answeredCount) {
        static ExitDto from(ExitResult r) {
            return new ExitDto(String.valueOf(r.attemptId()), r.status(),
                    r.quotaConsumed(), r.answeredCount());
        }
    }

    record AttemptHistoryDto(List<AttemptHistoryItemDto> attempts, int totalInDb) {}

    record AttemptHistoryItemDto(String attemptId, String mockExamId, String mockExamCode,
                                 String status, int scorePercent, int correctCount,
                                 int answeredCount, String startedAt, String submittedAt) {
        static AttemptHistoryItemDto from(AttemptHistoryRow r) {
            return new AttemptHistoryItemDto(
                    String.valueOf(r.id()), String.valueOf(r.mockExamId()), r.mockExamCode(),
                    r.status(),
                    r.scorePercent() == null ? -1 : r.scorePercent(),
                    r.correctCount() == null ? 0 : r.correctCount(),
                    r.answeredCount(), r.startedAt().toString(),
                    r.submittedAt() != null ? r.submittedAt().toString() : "");
        }
    }

    record AttemptStatsDto(int totalAttempts, int submittedCount, int exitedCount,
                           @JsonProperty("recent_3_avg_score_percent") int recent3AvgScorePercent,
                           int bestScorePercent, int latestScorePercent) {
        static AttemptStatsDto from(AttemptStats s) {
            return new AttemptStatsDto(s.totalAttempts(), s.submittedCount(), s.exitedCount(),
                    s.recent3AvgScorePercent() == null ? -1 : s.recent3AvgScorePercent(),
                    s.bestScorePercent() == null ? -1 : s.bestScorePercent(),
                    s.latestScorePercent() == null ? -1 : s.latestScorePercent());
        }
    }
}
