package com.dmvmotor.api.mockexam.application;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.authaccess.application.AccessService.AccessInfo;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.ResourceNotFoundException;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.content.infrastructure.QuestionRepository;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.AnswerRow;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.AttemptRow;
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

    public MockExamService(MockExamRepository mockExamRepo,
                           AccessService accessService,
                           QuestionRepository questionRepo) {
        this.mockExamRepo = mockExamRepo;
        this.accessService = accessService;
        this.questionRepo  = questionRepo;
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

        Long attemptId = mockExamRepo.createAttempt(userId, mockExamId, language);
        mockExamRepo.consumeMockQuota(userId);

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

    @Transactional
    public SaveAnswerResult saveAnswer(Long attemptId, Long userId,
                                       Long questionId, Long variantId, String selectedKey) {
        AttemptRow attempt = requireAttempt(attemptId, userId);
        boolean isNew = mockExamRepo.upsertAnswer(attemptId, questionId, variantId, selectedKey);
        int answeredCount = isNew ? attempt.answeredCount() + 1 : attempt.answeredCount();
        return new SaveAnswerResult(true, answeredCount);
    }

    @Transactional
    public SubmitResult submitAttempt(Long attemptId, Long userId) {
        AttemptRow attempt = requireAttempt(attemptId, userId);

        List<AnswerRow> answers = mockExamRepo.findAnswersByAttemptId(attemptId);
        int correctCount = 0;
        int wrongCount   = 0;

        for (AnswerRow answer : answers) {
            QuestionDetail q = questionRepo
                    .findByIdAndLanguage(answer.questionId(), attempt.language())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Question not found: " + answer.questionId()));
            boolean isCorrect = q.correctChoiceKey().equals(answer.selectedKey());
            mockExamRepo.markAnswerCorrectness(attemptId, answer.questionId(), isCorrect);
            if (isCorrect) correctCount++; else wrongCount++;
        }

        int total        = answers.size();
        int scorePercent = total == 0 ? 0 : (int) Math.round(100.0 * correctCount / total);
        mockExamRepo.scoreAttempt(attemptId, correctCount, wrongCount, scorePercent);

        List<MockExamRepository.WeakTopicRow> weakTopics =
                mockExamRepo.findWeakTopicsByAttemptId(attemptId);

        Map<String, String> nextAction = wrongCount > 0
                ? Map.of("type", "review", "label", "Review weak topics first")
                : Map.of("type", "practice", "label", "Keep practicing");

        return new SubmitResult(attemptId, "submitted", scorePercent,
                correctCount, wrongCount, weakTopics, nextAction);
    }

    @Transactional
    public ExitResult exitAttempt(Long attemptId, Long userId) {
        AttemptRow attempt = requireAttempt(attemptId, userId);
        mockExamRepo.updateAttemptStatus(attemptId, "ended_by_exit");
        return new ExitResult(attemptId, "ended_by_exit", true, attempt.answeredCount());
    }

    // ---------------------------------------------------------------
    // Result types
    // ---------------------------------------------------------------

    public record MockAccessResult(boolean allowed, int mockRemaining, String reason) {}

    public record StartAttemptResult(
            Long attemptId, String status,
            int mockRemainingAfterStart, List<QuestionDetail> questions) {}

    public record SaveAnswerResult(boolean saved, int answeredCount) {}

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
}
