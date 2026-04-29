package com.dmvmotor.api.practice.application;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
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
    private final AccessService             accessService;
    private final UserRepository            userRepo;

    public PracticeService(PracticeSessionRepository sessionRepo,
                           QuestionRepository questionRepo,
                           MistakeRepository mistakeRepo,
                           AccessService accessService,
                           UserRepository userRepo) {
        this.sessionRepo   = sessionRepo;
        this.questionRepo  = questionRepo;
        this.mistakeRepo   = mistakeRepo;
        this.accessService = accessService;
        this.userRepo      = userRepo;
    }

    @Transactional
    public StartSessionResult startSession(Long userId, String entryType, String language) {
        // entry_type=full requires an active access pass
        if ("full".equals(entryType)) {
            if (userId == null) {
                throw new BusinessException("UNAUTHORIZED",
                        "Authentication required for full practice", HttpStatus.UNAUTHORIZED);
            }
            if (!accessService.getAccess(userId).hasActivePass()) {
                throw new BusinessException("ACCESS_DENIED",
                        "Active access pass required for full practice", HttpStatus.FORBIDDEN);
            }
        }

        // Determine learning cycle (anonymous sessions always use cycle 0)
        int cycle = 0;
        if (userId != null) {
            cycle = userRepo.findById(userId).map(u -> u.resetCount()).orElse(0);
        }

        int total = sessionRepo.countTotal(language, entryType);
        if (total == 0) {
            throw new BusinessException("NO_QUESTIONS_AVAILABLE",
                    "No practice questions available for language: " + language,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Long sessionId = sessionRepo.create(userId, entryType, language, cycle);

        QuestionDetail first = sessionRepo
                .findNextUnansweredQuestion(sessionId, language, entryType)
                .orElseThrow(() -> new BusinessException("NO_QUESTIONS_AVAILABLE",
                        "No questions available", HttpStatus.UNPROCESSABLE_ENTITY));

        return new StartSessionResult(sessionId, entryType, "in_progress", language, first);
    }

    public QuestionDetail getNextQuestion(Long sessionId, Long requestUserId) {
        PracticeSession session = requireSession(sessionId, requestUserId);

        return sessionRepo.findNextUnansweredQuestion(
                        sessionId, session.languageCode(), session.entryType())
                .orElseThrow(() -> new BusinessException("SESSION_COMPLETED",
                        "No more questions in this session",
                        HttpStatus.NOT_FOUND));
    }

    public SessionStatus getSessionStatus(Long sessionId, Long requestUserId) {
        PracticeSession session = requireSession(sessionId, requestUserId);
        int answered = sessionRepo.countAnswered(sessionId);
        int total    = sessionRepo.countTotal(session.languageCode(), session.entryType());
        return new SessionStatus(sessionId, session.status(), answered, total);
    }

    @Transactional
    public AnswerResult submitAnswer(Long sessionId, Long requestUserId, Long questionId,
                                     Long variantId, String selectedKey) {
        PracticeSession session = requireSession(sessionId, requestUserId);

        if (!session.isInProgress()) {
            throw new BusinessException("CONFLICT_STATE",
                    "Session is not in progress", HttpStatus.CONFLICT);
        }

        if (sessionRepo.hasAttempt(sessionId, questionId)) {
            throw new BusinessException("QUESTION_ALREADY_SUBMITTED",
                    "Question already answered in this session", HttpStatus.CONFLICT);
        }

        if (!sessionRepo.existsInSessionPool(questionId, session.entryType())) {
            throw new BusinessException("QUESTION_NOT_IN_SESSION",
                    "Question is not part of this session's pool",
                    HttpStatus.BAD_REQUEST);
        }

        QuestionDetail question = questionRepo
                .findByIdAndLanguage(questionId, session.languageCode())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        boolean isCorrect = question.correctChoiceKey().equals(selectedKey);

        sessionRepo.saveAttempt(sessionId, session.userId(), questionId,
                variantId, selectedKey, isCorrect);

        if (!isCorrect && session.userId() != null) {
            int cycle = userRepo.findById(session.userId()).map(u -> u.resetCount()).orElse(0);
            mistakeRepo.upsertMistake(session.userId(), questionId,
                    question.topicId(), "practice", cycle);
        }

        int answeredCount = sessionRepo.countAnswered(sessionId);
        return new AnswerResult(questionId, isCorrect,
                question.correctChoiceKey(), question.explanation(), answeredCount);
    }

    @Transactional
    public CompletedSession completeSession(Long sessionId, Long requestUserId) {
        PracticeSession session = requireSession(sessionId, requestUserId);

        if (!session.isInProgress()) {
            throw new BusinessException("CONFLICT_STATE",
                    "Session is not in progress", HttpStatus.CONFLICT);
        }

        sessionRepo.updateStatus(sessionId, "completed");
        return new CompletedSession(sessionId, "completed");
    }

    private PracticeSession requireSession(Long sessionId, Long requestUserId) {
        PracticeSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Practice session not found: " + sessionId));
        // Owned sessions: requestUserId must match. Anonymous sessions (userId=null) are open.
        if (session.userId() != null && !session.userId().equals(requestUserId)) {
            throw new BusinessException("FORBIDDEN",
                    "Session belongs to a different user", HttpStatus.FORBIDDEN);
        }
        return session;
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
