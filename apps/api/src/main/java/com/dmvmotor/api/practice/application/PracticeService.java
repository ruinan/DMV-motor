package com.dmvmotor.api.practice.application;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.ResourceNotFoundException;
import com.dmvmotor.api.content.application.ExamContext;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.content.infrastructure.QuestionRepository;
import com.dmvmotor.api.mistakereview.infrastructure.MistakeListRepository;
import com.dmvmotor.api.mistakereview.review.domain.MasteryEvaluator;
import com.dmvmotor.api.mistakereview.review.infrastructure.PracticeHistoryRepository;
import com.dmvmotor.api.practice.domain.AnswerResult;
import com.dmvmotor.api.practice.domain.PracticeSession;
import com.dmvmotor.api.mistakereview.infrastructure.MistakeRepository;
import com.dmvmotor.api.practice.infrastructure.PracticeHistoryDao;
import com.dmvmotor.api.practice.infrastructure.PracticeHistoryDao.AttemptTotals;
import com.dmvmotor.api.practice.infrastructure.PracticeHistoryDao.SessionHistoryRow;
import com.dmvmotor.api.practice.infrastructure.PracticeQuestionSelector;
import com.dmvmotor.api.practice.infrastructure.PracticeSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PracticeService {

    private static final int MAX_HISTORY_LIMIT = 50;

    private final PracticeSessionRepository sessionRepo;
    private final PracticeQuestionSelector  questionSelector;
    private final PracticeHistoryDao        historyDao;
    private final QuestionRepository        questionRepo;
    private final MistakeRepository         mistakeRepo;
    private final MistakeListRepository     mistakeListRepo;
    private final MasteryEvaluator          masteryEvaluator;
    private final PracticeHistoryRepository practiceHistoryRepo;
    private final AccessService             accessService;
    private final UserRepository            userRepo;
    private final ExamContext               examContext;

    public PracticeService(PracticeSessionRepository sessionRepo,
                           PracticeQuestionSelector questionSelector,
                           PracticeHistoryDao historyDao,
                           QuestionRepository questionRepo,
                           MistakeRepository mistakeRepo,
                           MistakeListRepository mistakeListRepo,
                           MasteryEvaluator masteryEvaluator,
                           PracticeHistoryRepository practiceHistoryRepo,
                           AccessService accessService,
                           UserRepository userRepo,
                           ExamContext examContext) {
        this.sessionRepo          = sessionRepo;
        this.questionSelector     = questionSelector;
        this.historyDao           = historyDao;
        this.questionRepo         = questionRepo;
        this.mistakeRepo          = mistakeRepo;
        this.mistakeListRepo      = mistakeListRepo;
        this.masteryEvaluator     = masteryEvaluator;
        this.practiceHistoryRepo  = practiceHistoryRepo;
        this.accessService        = accessService;
        this.userRepo             = userRepo;
        this.examContext          = examContext;
    }

    public SessionHistoryResult listHistory(Long userId, int requestedLimit) {
        int limit = Math.min(Math.max(requestedLimit, 1), MAX_HISTORY_LIMIT);
        // History is scoped to the exam the user is currently preparing for —
        // switching exam shows that exam's sessions only.
        Long examId = examContext.resolveExamId(userId);
        int cycle = userRepo.findById(userId).map(UserRepository.UserRow::resetCount).orElse(0);
        List<SessionHistoryRow> rows = historyDao.findRecentByUserWithStats(userId, examId, cycle, limit);
        int totalInDb = historyDao.countByUser(userId, examId, cycle);
        return new SessionHistoryResult(rows, totalInDb);
    }

    public PracticeStats getStats(Long userId) {
        Long examId = examContext.resolveExamId(userId);
        int cycle = userRepo.findById(userId).map(UserRepository.UserRow::resetCount).orElse(0);
        int totalSessions = historyDao.countByUser(userId, examId, cycle);
        AttemptTotals totals = historyDao.attemptTotals(userId, examId, cycle);
        int activeMistakes = mistakeListRepo.countActive(userId, examId, cycle);
        int activeMistakeTopics = mistakeListRepo.countDistinctActiveTopics(userId, examId, cycle);
        int accuracy = totals.answered() == 0
                ? 0
                : (int) Math.round(totals.correct() * 100.0 / totals.answered());
        return new PracticeStats(totalSessions, totals.answered(), totals.correct(),
                accuracy, activeMistakes, activeMistakeTopics);
    }

    public record SessionHistoryResult(List<SessionHistoryRow> sessions, int totalInDb) {}

    public record PracticeStats(
            int totalSessions,
            int totalQuestionsAnswered,
            int totalCorrect,
            int overallAccuracyPercent,
            int activeMistakesCount,
            int activeMistakesTopicCount
    ) {}

    private static final int MAX_TOPIC_FILTER = 8;

    @Transactional
    public StartSessionResult startSession(Long userId, String entryType, String language) {
        return startSession(userId, entryType, language, null, null);
    }

    @Transactional
    public StartSessionResult startSession(Long userId, String entryType, String language,
                                           List<Long> topicFilter) {
        return startSession(userId, entryType, language, topicFilter, null);
    }

    @Transactional
    public StartSessionResult startSession(Long userId, String entryType, String language,
                                           List<Long> topicFilter, Long requestedExamId) {
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

        // Cap the topic filter server-side (decision #5) so a crafted request
        // can't pass an unbounded IN-list.
        List<Long> filter = topicFilter == null ? List.of()
                : topicFilter.stream().limit(MAX_TOPIC_FILTER).toList();

        // Determine learning cycle (anonymous sessions always use cycle 0)
        int cycle = 0;
        if (userId != null) {
            cycle = userRepo.findById(userId).map(u -> u.resetCount()).orElse(0);
        }

        // Snapshot the exam at start. Signed-in users use their current exam;
        // anonymous visitors pick on the landing page and pass requestedExamId
        // (validated server-side, default fallback). Snapshotting means a later
        // exam switch never rewires the in-flight pool — like language/cycle.
        Long examId = examContext.resolveExamId(userId, requestedExamId);

        int total = sessionRepo.countTotal(language, entryType, examId);
        if (total == 0) {
            throw new BusinessException("NO_QUESTIONS_AVAILABLE",
                    "No practice questions available for language: " + language,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Long sessionId = sessionRepo.create(userId, entryType, language, cycle, examId, filter);

        QuestionDetail first = questionSelector
                .findNextUnansweredQuestion(sessionId, language, entryType, userId, cycle, examId, filter)
                .orElseThrow(() -> new BusinessException("NO_QUESTIONS_AVAILABLE",
                        "No questions available for the selected topics",
                        HttpStatus.UNPROCESSABLE_ENTITY));

        return new StartSessionResult(sessionId, entryType, "in_progress", language, first);
    }

    public QuestionDetail getNextQuestion(Long sessionId, Long requestUserId) {
        return getNextQuestion(sessionId, requestUserId, null);
    }

    public QuestionDetail getNextQuestion(Long sessionId, Long requestUserId, String overrideLanguage) {
        PracticeSession session = requireSession(sessionId, requestUserId);
        // The session was created in a specific learning cycle; even if the
        // user's reset_count has since incremented, personalization stays
        // anchored to the session's original cycle so a mid-session reset
        // does not silently rewire the question stream.
        int cycle = 0;
        if (session.userId() != null) {
            cycle = userRepo.findById(session.userId()).map(u -> u.resetCount()).orElse(0);
        }

        // overrideLanguage lets the caller (typically the frontend after a
        // mid-session locale toggle) flip the displayed variant without
        // mutating the session's stored language_code.
        String effectiveLang = overrideLanguage != null && !overrideLanguage.isBlank()
                ? overrideLanguage
                : session.languageCode();

        // A session is a fixed 10-question round (both free and paid). Once the
        // user has answered that many, the session is done — surfaced as the
        // same SESSION_COMPLETED the frontend already treats as "finished".
        if (sessionRepo.countAnswered(sessionId)
                >= PracticeSessionRepository.capFor(session.entryType())) {
            throw new BusinessException("SESSION_COMPLETED",
                    "No more questions in this session", HttpStatus.NOT_FOUND);
        }

        return questionSelector.findNextUnansweredQuestion(
                        sessionId, effectiveLang, session.entryType(),
                        session.userId(), cycle, session.examId(), session.topicFilter())
                .orElseThrow(() -> new BusinessException("SESSION_COMPLETED",
                        "No more questions in this session",
                        HttpStatus.NOT_FOUND));
    }

    public SessionStatus getSessionStatus(Long sessionId, Long requestUserId) {
        PracticeSession session = requireSession(sessionId, requestUserId);
        int answered = sessionRepo.countAnswered(sessionId);
        // A topic-scoped session ("Practice these") shows its filtered pool size
        // — the same filter findNextUnansweredQuestion serves from — not the
        // whole bank capped, which would over-report the total.
        int total    = Math.min(PracticeSessionRepository.capFor(session.entryType()),
                sessionRepo.countTotal(session.languageCode(), session.entryType(),
                        session.examId(), session.topicFilter()));
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

        if (!sessionRepo.existsInSessionPool(questionId, session.entryType(), session.examId())) {
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

        if (session.userId() != null) {
            int cycle = userRepo.findById(session.userId()).map(u -> u.resetCount()).orElse(0);
            if (!isCorrect) {
                mistakeRepo.upsertMistake(session.userId(), questionId,
                        question.topicId(), "practice", cycle);
            } else {
                // A correct answer can complete topic mastery → clear that
                // topic's accumulated mistakes. Without this the live practice
                // flow never resolved mistakes (deactivateForTopic was only
                // wired into the deprecated /review module), so active mistakes
                // piled up forever and the next-step recommendation never moved.
                resolveTopicMistakesIfMastered(session.userId(), question.topicId(), cycle);
            }
        }

        int answeredCount = sessionRepo.countAnswered(sessionId);
        return new AnswerResult(questionId, isCorrect,
                question.correctChoiceKey(), question.explanation(), answeredCount);
    }

    /**
     * Topic-level mastery gate (docs/parameters.md §6): once the user's history
     * for {@code topicId} in {@code cycle} passes both gates (overall correctness
     * ≥ threshold, ≥K of last N correct), every active mistake in that topic
     * clears in one shot — the user doesn't have to re-answer each accumulated
     * mistake question individually. Mirrors what {@code ReviewService.completeTask}
     * did, but driven from the path users actually use. Idempotent + cheap
     * (scoped to a single topic).
     */
    private void resolveTopicMistakesIfMastered(Long userId, Long topicId, int cycle) {
        var stats  = practiceHistoryRepo.topicStats(userId, topicId, cycle);
        var recent = practiceHistoryRepo.lastNAttemptsForTopic(userId, topicId, cycle,
                masteryEvaluator.recentWindow());
        if (masteryEvaluator.isMastered(stats, recent)) {
            mistakeListRepo.deactivateForTopic(userId, topicId, cycle);
        }
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

    /**
     * Read-only history of submitted answers in the session. Round 4 #2:
     * users wanted to revisit past questions to see why an answer was wrong;
     * the next-question pool excludes already-answered ones, so we surface
     * them via this dedicated endpoint instead of letting the user re-pick
     * (which would hit hasAttempt → 409). The display language is independent
     * of the session's original language so a user can flip to translation.
     */
    public java.util.List<com.dmvmotor.api.practice.infrastructure.PracticeSessionRepository.AttemptDetail>
    listAttempts(Long sessionId, Long requestUserId, String language) {
        PracticeSession session = requireSession(sessionId, requestUserId);
        String lang = (language == null || language.isBlank()) ? session.languageCode() : language;
        return sessionRepo.findAttemptsBySessionId(session.id(), lang);
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
        // entry_type=full requires an active access pass for the entire
        // lifetime of the session, not just at creation. There's no
        // background job flipping expired rows to status='expired', so a
        // pass whose window elapsed mid-session must be re-checked here
        // to keep paid content paywalled.
        if ("full".equals(session.entryType()) && session.userId() != null) {
            if (!accessService.getAccess(session.userId()).hasActivePass()) {
                throw new BusinessException("ACCESS_DENIED",
                        "Active access pass required for full practice",
                        HttpStatus.FORBIDDEN);
            }
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
