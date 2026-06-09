package com.dmvmotor.api.practice.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.common.Ids;
import com.dmvmotor.api.content.domain.Choice;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.practice.application.PracticeService;
import com.dmvmotor.api.practice.domain.AnswerResult;
import com.dmvmotor.api.practice.infrastructure.PracticeHistoryDao;
import com.dmvmotor.api.practice.infrastructure.PracticeSessionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
                req.language() != null ? req.language() : "en",
                req.topicFilter(), req.examId(), req.mode());
        return ApiResponse.ok(StartSessionDto.from(result));
    }

    @GetMapping("/{id}/next-question")
    public ApiResponse<?> nextQuestion(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam(required = false) String language
    ) {
        QuestionDetail q = practiceService.getNextQuestion(id, userId, language);
        int answered = practiceService.getSessionStatus(id, userId).answeredCount();
        return ApiResponse.ok(NextQuestionDto.from(q, answered));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getSession(@CurrentUser Long userId, @PathVariable Long id) {
        var status = practiceService.getSessionStatus(id, userId);
        return ApiResponse.ok(SessionStatusDto.from(status));
    }

    @PostMapping("/{id}/answers")
    public ApiResponse<?> submitAnswer(@CurrentUser Long userId,
                                        @PathVariable Long id,
                                        @Valid @RequestBody AnswerRequest req) {
        var result = practiceService.submitAnswer(id, userId,
                Ids.parse(req.questionId(), "question_id"),
                Ids.parse(req.variantId(), "variant_id"),
                req.selectedChoiceKey());
        return ApiResponse.ok(SubmitAnswerDto.from(result));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<?> completeSession(@CurrentUser Long userId, @PathVariable Long id) {
        var result = practiceService.completeSession(id, userId);
        return ApiResponse.ok(CompleteSessionDto.from(result));
    }

    @GetMapping("/{id}/attempts")
    public ApiResponse<?> listAttempts(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam(required = false) String language
    ) {
        var items = practiceService.listAttempts(id, userId, language).stream()
                .map(AttemptItemDto::from).toList();
        return ApiResponse.okWithMeta(new AttemptsDto(items),
                Map.of("total", items.size()));
    }

    @GetMapping("/history")
    public ApiResponse<?> listHistory(
            @CurrentUser Long userId,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        requireAuth(userId);
        var result = practiceService.listHistory(userId, limit);
        var sessions = result.sessions().stream().map(HistoryItemDto::from).toList();
        return ApiResponse.ok(new HistoryDto(sessions, result.totalInDb()));
    }

    @GetMapping("/stats")
    public ApiResponse<?> getStats(@CurrentUser Long userId) {
        requireAuth(userId);
        return ApiResponse.ok(StatsDto.from(practiceService.getStats(userId)));
    }

    private void requireAuth(Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED",
                    "Authentication required", HttpStatus.UNAUTHORIZED);
        }
    }

    // ---------------------------------------------------------------
    // Request DTOs
    // ---------------------------------------------------------------

    record StartRequest(
            @NotBlank(message = "must not be blank") String entry_type,
            String language,
            List<Long> topic_filter,
            // Anonymous visitors name the exam to practice (landing-page "choose
            // then practice"). Ignored for signed-in users — they always get
            // their server-side current exam. See ExamContext.resolveExamId.
            Long exam_id,
            // Practice mode (bug4): random / weak_points / review_learned.
            // Backend downgrades non-paid sessions to random regardless.
            String mode
    ) {
        String entryType() { return entry_type; }
        List<Long> topicFilter() { return topic_filter; }
        Long examId() { return exam_id; }
        // mode() is the record's own component accessor (auto-generated, public).
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

    // ---------------------------------------------------------------
    // Response DTOs — explicit records (snake_case via Jackson) so the wire
    // contract is type-checked instead of stringly-keyed inline maps. Each
    // `from` factory owns the id-stringification and null-defaulting that the
    // old Map.of ternaries did.
    // ---------------------------------------------------------------

    record QuestionDto(String questionId, String variantId, String stem, List<Choice> choices) {
        static QuestionDto from(QuestionDetail q) {
            return new QuestionDto(String.valueOf(q.questionId()), String.valueOf(q.variantId()),
                    q.stem(), q.choices());
        }
    }

    record ProgressDto(int answeredCount) {}

    record StartSessionDto(String sessionId, String entryType, String status,
                           String language, String selectionMode, QuestionDto nextQuestion) {
        static StartSessionDto from(PracticeService.StartSessionResult r) {
            return new StartSessionDto(String.valueOf(r.sessionId()), r.entryType(),
                    r.status(), r.language(), r.selectionMode(), QuestionDto.from(r.nextQuestion()));
        }
    }

    record NextQuestionDto(String questionId, String variantId, String stem,
                           List<Choice> choices, ProgressDto progress) {
        static NextQuestionDto from(QuestionDetail q, int answered) {
            return new NextQuestionDto(String.valueOf(q.questionId()),
                    String.valueOf(q.variantId()), q.stem(), q.choices(),
                    new ProgressDto(answered));
        }
    }

    record SessionStatusDto(String sessionId, String status, int answeredCount, int totalCount) {
        static SessionStatusDto from(PracticeService.SessionStatus s) {
            return new SessionStatusDto(String.valueOf(s.sessionId()), s.status(),
                    s.answeredCount(), s.totalCount());
        }
    }

    record SubmitAnswerDto(String questionId, boolean isCorrect, String correctChoiceKey,
                           String explanation, ProgressDto progress) {
        static SubmitAnswerDto from(AnswerResult r) {
            return new SubmitAnswerDto(String.valueOf(r.questionId()), r.isCorrect(),
                    r.correctChoiceKey(), r.explanation() != null ? r.explanation() : "",
                    new ProgressDto(r.answeredCount()));
        }
    }

    record CompleteSessionDto(String sessionId, String status) {
        static CompleteSessionDto from(PracticeService.CompletedSession r) {
            return new CompleteSessionDto(String.valueOf(r.sessionId()), r.status());
        }
    }

    record AttemptsDto(List<AttemptItemDto> items) {}

    record AttemptItemDto(String questionId, String variantId, String topicId, String language,
                          String stem, List<Choice> choices, String correctChoiceKey,
                          String selectedChoiceKey, String explanation, boolean isCorrect,
                          String submittedAt) {
        static AttemptItemDto from(PracticeSessionRepository.AttemptDetail a) {
            return new AttemptItemDto(
                    String.valueOf(a.questionId()), String.valueOf(a.variantId()),
                    String.valueOf(a.topicId()), a.language(), a.stem(), a.choices(),
                    a.correctChoiceKey(), a.selectedChoiceKey(),
                    a.explanation() != null ? a.explanation() : "",
                    a.isCorrect(), a.submittedAt().toString());
        }
    }

    record HistoryDto(List<HistoryItemDto> sessions, int totalInDb) {}

    record HistoryItemDto(String sessionId, String entryType, String language, String status,
                          String startedAt, String completedAt, int answeredCount,
                          int correctCount, int accuracyPercent) {
        static HistoryItemDto from(PracticeHistoryDao.SessionHistoryRow r) {
            int accuracy = r.answeredCount() == 0 ? 0
                    : (int) Math.round(r.correctCount() * 100.0 / r.answeredCount());
            return new HistoryItemDto(
                    String.valueOf(r.id()), r.entryType(), r.languageCode(), r.status(),
                    r.startedAt().toString(),
                    r.completedAt() != null ? r.completedAt().toString() : "",
                    r.answeredCount(), r.correctCount(), accuracy);
        }
    }

    record StatsDto(int totalSessions, int totalQuestionsAnswered, int totalCorrect,
                    int overallAccuracyPercent, int activeMistakesCount,
                    int activeMistakesTopicCount) {
        static StatsDto from(PracticeService.PracticeStats s) {
            return new StatsDto(s.totalSessions(), s.totalQuestionsAnswered(), s.totalCorrect(),
                    s.overallAccuracyPercent(), s.activeMistakesCount(),
                    s.activeMistakesTopicCount());
        }
    }
}
