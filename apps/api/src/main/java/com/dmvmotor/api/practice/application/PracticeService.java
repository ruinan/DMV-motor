package com.dmvmotor.api.practice.application;

import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.ResourceNotFoundException;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.content.infrastructure.QuestionRepository;
import com.dmvmotor.api.practice.domain.AnswerResult;
import com.dmvmotor.api.practice.domain.PracticeSession;
import com.dmvmotor.api.practice.infrastructure.MistakeRepository;
import com.dmvmotor.api.practice.infrastructure.PracticeSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PracticeService {

    private final PracticeSessionRepository sessionRepo;
    private final QuestionRepository        questionRepo;
    private final MistakeRepository         mistakeRepo;

    public PracticeService(PracticeSessionRepository sessionRepo,
                           QuestionRepository questionRepo,
                           MistakeRepository mistakeRepo) {
        this.sessionRepo  = sessionRepo;
        this.questionRepo = questionRepo;
        this.mistakeRepo  = mistakeRepo;
    }

    @Transactional
    public StartSessionResult startSession(Long userId, String entryType, String language) {
        // Verify at least one question is available before creating session
        int total = sessionRepo.countTotal(language);
        if (total == 0) {
            throw new BusinessException("NO_QUESTIONS_AVAILABLE",
                    "No practice questions available for language: " + language,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Long sessionId = sessionRepo.create(userId, entryType, language);

        QuestionDetail first = sessionRepo
                .findNextUnansweredQuestion(sessionId, language)
                .orElseThrow(() -> new BusinessException("NO_QUESTIONS_AVAILABLE",
                        "No questions available", HttpStatus.UNPROCESSABLE_ENTITY));

        return new StartSessionResult(sessionId, entryType, "in_progress", language, first);
    }

    public QuestionDetail getNextQuestion(Long sessionId) {
        PracticeSession session = requireSession(sessionId);

        return sessionRepo.findNextUnansweredQuestion(sessionId, session.languageCode())
                .orElseThrow(() -> new BusinessException("SESSION_COMPLETED",
                        "No more questions in this session",
                        HttpStatus.NOT_FOUND));
    }

    public SessionStatus getSessionStatus(Long sessionId) {
        PracticeSession session = requireSession(sessionId);
        int answered = sessionRepo.countAnswered(sessionId);
        int total    = sessionRepo.countTotal(session.languageCode());
        return new SessionStatus(sessionId, session.status(), answered, total);
    }

    @Transactional
    public AnswerResult submitAnswer(Long sessionId, Long questionId,
                                     Long variantId, String selectedKey) {
        PracticeSession session = requireSession(sessionId);

        if (sessionRepo.hasAttempt(sessionId, questionId)) {
            throw new BusinessException("QUESTION_ALREADY_SUBMITTED",
                    "Question already answered in this session", HttpStatus.CONFLICT);
        }

        QuestionDetail question = questionRepo
                .findByIdAndLanguage(questionId, session.languageCode())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        boolean isCorrect = question.correctChoiceKey().equals(selectedKey);

        sessionRepo.saveAttempt(sessionId, session.userId(), questionId,
                variantId, selectedKey, isCorrect);

        if (!isCorrect && session.userId() != null) {
            mistakeRepo.upsertMistake(session.userId(), questionId,
                    question.topicId(), "practice");
        }

        int answeredCount = sessionRepo.countAnswered(sessionId);
        return new AnswerResult(questionId, isCorrect,
                question.correctChoiceKey(), question.explanation(), answeredCount);
    }

    @Transactional
    public CompletedSession completeSession(Long sessionId) {
        PracticeSession session = requireSession(sessionId);

        if (!session.isInProgress()) {
            throw new BusinessException("CONFLICT_STATE",
                    "Session is not in progress", HttpStatus.CONFLICT);
        }

        sessionRepo.updateStatus(sessionId, "completed");
        return new CompletedSession(sessionId, "completed");
    }

    private PracticeSession requireSession(Long sessionId) {
        return sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Practice session not found: " + sessionId));
    }

    // ---------------------------------------------------------------
    // Result types
    // ---------------------------------------------------------------

    public record StartSessionResult(
            Long sessionId, String entryType, String status,
            String language, QuestionDetail nextQuestion) {}

    public record SessionStatus(
            Long sessionId, String status, int answeredCount, int totalCount) {}

    public record CompletedSession(Long sessionId, String status) {}
}
