package com.dmvmotor.api.mockexam.application;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.authaccess.application.AccessService.AccessInfo;
import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.ResourceNotFoundException;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.content.infrastructure.QuestionRepository;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.AnswerRow;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.AttemptRow;
import com.dmvmotor.api.practice.infrastructure.MistakeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MockExamService {

    private final MockExamRepository mockExamRepo;
    private final AccessService      accessService;
    private final QuestionRepository questionRepo;
    private final MistakeRepository  mistakeRepo;
    private final UserRepository     userRepo;
    private final ApplicationEventPublisher events;

    public MockExamService(MockExamRepository mockExamRepo,
                           AccessService accessService,
                           QuestionRepository questionRepo,
                           MistakeRepository mistakeRepo,
                           UserRepository userRepo,
                           ApplicationEventPublisher events) {
        this.mockExamRepo  = mockExamRepo;
        this.accessService = accessService;
        this.questionRepo  = questionRepo;
        this.mistakeRepo   = mistakeRepo;
        this.userRepo      = userRepo;
        this.events        = events;
    }

    public MockAccessResult checkAccess(Long userId) {
        AccessInfo info = accessService.getAccess(userId);
        boolean allowed = info.canUseMockExam();
        return new MockAccessResult(allowed, info.mockRemaining(),
                allowed ? null : "No active pass or quota exhausted");
    }

    @Transactional
    public StartAttemptResult startAttempt(Long userId, String language) {
        AccessInfo info = accessService.getAccess(userId);
        if (!info.canUseMockExam()) {
            throw new BusinessException("ACCESS_DENIED",
                    "No active access pass or mock quota exhausted",
                    HttpStatus.FORBIDDEN);
        }

        Long mockExamId = mockExamRepo.findActiveMockExamId()
                .orElseThrow(() -> new BusinessException("NO_MOCK_EXAM_AVAILABLE",
                        "No active mock exam template found",
                        HttpStatus.UNPROCESSABLE_ENTITY));

        int cycle = userRepo.findById(userId).map(u -> u.resetCount()).orElse(0);
        Long attemptId = mockExamRepo.createAttempt(userId, mockExamId, language, cycle);
        // canUseMockExam=true ⇒ activePassId is non-null, so quota decrement
        // targets the specific row that's currently in window.
        mockExamRepo.consumeMockQuotaForPass(info.activePassId());

        List<Long> questionIds = mockExamRepo.findQuestionIdsByMockExamId(mockExamId);
        List<QuestionDetail> questions = questionIds.stream()
                .map(qId -> questionRepo.findByIdAndLanguage(qId, language)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Question not found: " + qId)))
                .toList();

        int mockRemainingAfterStart = info.mockRemaining() - 1;
        return new StartAttemptResult(attemptId, "in_progress",
                mockRemainingAfterStart, questions);
    }

    /**
     * Pass threshold = 85% per docs/parameters.md. Anything strictly above the
     * resulting wrong cap → exam auto-terminates with status='ended_by_failure'.
     * 30-question exam → ceil(30 × 0.15) = 5 wrongs allowed; on the 6th, fail.
     * Kept in service so unit-test overrides land without an env round-trip.
     */
    private static final double PASS_THRESHOLD = 0.85;

    private int maxAllowedWrong(int totalQuestions) {
        return (int) Math.ceil(totalQuestions * (1 - PASS_THRESHOLD));
    }

    private static int scorePercent(int correctCount, int totalQuestions) {
        if (totalQuestions == 0) return 0;
        return (int) Math.round(100.0 * correctCount / totalQuestions);
    }

    @Transactional
    public SaveAnswerResult saveAnswer(Long attemptId, Long userId,
                                       Long questionId, Long variantId, String selectedKey) {
        AttemptRow attempt = requireAttempt(attemptId, userId);

        if (!"in_progress".equals(attempt.status())) {
            throw new BusinessException("MOCK_ALREADY_ENDED",
                    "Mock attempt is no longer accepting answers", HttpStatus.CONFLICT);
        }

        if (!mockExamRepo.existsInMockExam(attempt.mockExamId(), questionId)) {
            throw new BusinessException("QUESTION_NOT_IN_MOCK_EXAM",
                    "Question is not part of this mock exam",
                    HttpStatus.BAD_REQUEST);
        }

        // Compute correctness inline — new UX shows right/wrong before
        // advancing, so the answer row carries is_correct from save-time
        // instead of waiting until submit().
        QuestionDetail question = questionRepo
                .findByIdAndLanguage(questionId, attempt.language())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question not found: " + questionId));
        boolean isCorrect = question.correctChoiceKey().equals(selectedKey);

        boolean isNew = mockExamRepo.upsertAnswer(attemptId, questionId, variantId, selectedKey, isCorrect);
        int answeredCount = isNew ? attempt.answeredCount() + 1 : attempt.answeredCount();

        // Mistake-record upsert happens on every wrong answer regardless of
        // pass/fail — keeps practice personalization fresh in real time.
        if (!isCorrect) {
            int cycle = userRepo.findById(userId).map(u -> u.resetCount()).orElse(0);
            mistakeRepo.upsertMistake(userId, questionId, question.topicId(),
                    "mock_exam", cycle);
        }

        int wrongCount = mockExamRepo.countWrongAnswers(attemptId);
        int totalQuestions = mockExamRepo.findMockExamQuestionCount(attempt.mockExamId());
        int maxWrong = maxAllowedWrong(totalQuestions);
        boolean shouldTerminate = wrongCount > maxWrong;

        if (shouldTerminate) {
            int correctCount = mockExamRepo.countCorrectAnswers(attemptId);
            mockExamRepo.finalizeAttempt(attemptId, "ended_by_failure",
                    scorePercent(correctCount, totalQuestions), correctCount, wrongCount);
            events.publishEvent(new MockAttemptCompletedEvent(attemptId, userId));
        }

        return new SaveAnswerResult(
                true, answeredCount, isCorrect,
                question.correctChoiceKey(), wrongCount, maxWrong, shouldTerminate);
    }

    @Transactional
    public SubmitResult submitAttempt(Long attemptId, Long userId) {
        AttemptRow attempt = requireAttempt(attemptId, userId);

        if (!"in_progress".equals(attempt.status())) {
            throw new BusinessException("MOCK_ALREADY_ENDED",
                    "Mock exam already submitted or exited", HttpStatus.CONFLICT);
        }

        // Correctness is already persisted on each row by saveAnswer (new mock
        // UX scores inline). Submit just aggregates and finalizes — no more
        // per-answer recompute. Mistake upserts also happened at save time.
        int correctCount  = mockExamRepo.countCorrectAnswers(attemptId);
        int wrongCount    = mockExamRepo.countWrongAnswers(attemptId);
        int total         = mockExamRepo.findMockExamQuestionCount(attempt.mockExamId());
        mockExamRepo.finalizeAttempt(attemptId, "submitted",
                scorePercent(correctCount, total), correctCount, wrongCount);
        events.publishEvent(new MockAttemptCompletedEvent(attemptId, userId));

        List<MockExamRepository.WeakTopicRow> weakTopics =
                mockExamRepo.findWeakTopicsByAttemptId(attemptId);

        Map<String, String> nextAction = wrongCount > 0
                ? Map.of("type", "review", "label", "Review weak topics first")
                : Map.of("type", "practice", "label", "Keep practicing");

        return new SubmitResult(attemptId, "submitted", scorePercent(correctCount, total),
                correctCount, wrongCount, weakTopics, nextAction);
    }

    @Transactional
    public ExitResult exitAttempt(Long attemptId, Long userId) {
        AttemptRow attempt = requireAttempt(attemptId, userId);
        if (!"in_progress".equals(attempt.status())) {
            throw new BusinessException("MOCK_ALREADY_ENDED",
                    "Mock exam already submitted or exited", HttpStatus.CONFLICT);
        }
        mockExamRepo.updateAttemptStatus(attemptId, "ended_by_exit");
        events.publishEvent(new MockAttemptCompletedEvent(attemptId, userId));
        return new ExitResult(attemptId, "ended_by_exit", true, attempt.answeredCount());
    }

    // ---------------------------------------------------------------
    // Result types
    // ---------------------------------------------------------------

    public record MockAccessResult(boolean allowed, int mockRemaining, String reason) {}

    public record StartAttemptResult(
            Long attemptId, String status,
            int mockRemainingAfterStart, List<QuestionDetail> questions) {}

    public record SaveAnswerResult(
            boolean saved,
            int     answeredCount,
            boolean isCorrect,
            String  correctChoiceKey,
            int     wrongCountSoFar,
            int     maxAllowedWrong,
            boolean shouldTerminate
    ) {}

    public record SubmitResult(
            Long attemptId, String status,
            int scorePercent, int correctCount, int wrongCount,
            List<MockExamRepository.WeakTopicRow> weakTopics,
            Map<String, String> nextAction) {}

    public record ExitResult(
            Long attemptId, String status,
            boolean quotaConsumed, int answeredCount) {}

    private AttemptRow requireAttempt(Long attemptId, Long userId) {
        AttemptRow attempt = mockExamRepo.findAttemptById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mock attempt not found: " + attemptId));
        if (!attempt.userId().equals(userId)) {
            throw new BusinessException("FORBIDDEN",
                    "Attempt belongs to a different user", HttpStatus.FORBIDDEN);
        }
        return attempt;
    }

    // ===== Study Hub history + stats =====

    private static final int MAX_HISTORY_LIMIT = 50;

    public AttemptHistoryResult listHistory(Long userId, int requestedLimit) {
        int limit = Math.min(Math.max(requestedLimit, 1), MAX_HISTORY_LIMIT);
        var rows = mockExamRepo.findRecentByUser(userId, limit);
        int totalInDb = mockExamRepo.countAttemptsByUser(userId);
        return new AttemptHistoryResult(rows, totalInDb);
    }

    public MockExamRepository.AttemptStats getStats(Long userId) {
        return mockExamRepo.aggregateStats(userId);
    }

    public record AttemptHistoryResult(
            List<MockExamRepository.AttemptHistoryRow> attempts,
            int                                        totalInDb
    ) {}

    /**
     * Returns enough state for a mock-exam attempt to be resumed on a fresh
     * client (refresh / new tab / different device). The original startAttempt
     * response was stashed in sessionStorage; this endpoint replaces that
     * dependency by reading the same data from the persisted attempt.
     */
    public AttemptDetailResult getAttemptDetail(Long attemptId, Long userId, String language) {
        AttemptRow attempt = requireAttempt(attemptId, userId);
        String effectiveLang = language != null && !language.isBlank()
                ? language
                : attempt.language();

        List<Long> questionIds = mockExamRepo.findQuestionIdsByMockExamId(attempt.mockExamId());
        List<QuestionDetail> questions = questionIds.stream()
                .map(qId -> questionRepo.findByIdAndLanguage(qId, effectiveLang)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Question not found: " + qId)))
                .toList();

        List<AnswerRow> savedAnswers = mockExamRepo.findAnswersByAttemptId(attemptId);
        return new AttemptDetailResult(
                attempt.id(),
                attempt.mockExamId(),
                attempt.status(),
                effectiveLang,
                questions,
                savedAnswers,
                attempt.scorePercent(),
                attempt.correctCount(),
                attempt.wrongCount());
    }

    public record AttemptDetailResult(
            Long                 attemptId,
            Long                 mockExamId,
            String               status,
            String               language,
            List<QuestionDetail> questions,
            List<AnswerRow>      savedAnswers,
            Integer              scorePercent,
            Integer              correctCount,
            Integer              wrongCount
    ) {}
}
