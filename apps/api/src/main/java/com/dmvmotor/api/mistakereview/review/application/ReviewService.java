package com.dmvmotor.api.mistakereview.review.application;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.ResourceNotFoundException;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.content.infrastructure.QuestionRepository;
import com.dmvmotor.api.mistakereview.domain.MistakeRecord;
import com.dmvmotor.api.mistakereview.infrastructure.MistakeListRepository;
import com.dmvmotor.api.mistakereview.review.infrastructure.ReviewRepository;
import com.dmvmotor.api.mistakereview.review.infrastructure.ReviewRepository.TaskRow;
import com.dmvmotor.api.practice.infrastructure.MistakeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    private final ReviewRepository      reviewRepo;
    private final MistakeListRepository mistakeListRepo;
    private final MistakeRepository     mistakeRepo;
    private final QuestionRepository    questionRepo;
    private final AccessService         accessService;
    private final UserRepository        userRepo;

    public ReviewService(ReviewRepository reviewRepo,
                         MistakeListRepository mistakeListRepo,
                         MistakeRepository mistakeRepo,
                         QuestionRepository questionRepo,
                         AccessService accessService,
                         UserRepository userRepo) {
        this.reviewRepo      = reviewRepo;
        this.mistakeListRepo = mistakeListRepo;
        this.mistakeRepo     = mistakeRepo;
        this.questionRepo    = questionRepo;
        this.accessService   = accessService;
        this.userRepo        = userRepo;
    }

    @Transactional
    public ReviewPackResult getOrCreatePack(Long userId) {
        requireReviewAccess(userId);

        int cycle = cycleFor(userId);

        var existingPackId = reviewRepo.findActivePackId(userId, cycle);
        if (existingPackId.isPresent()) {
            return buildPackResult(existingPackId.get());
        }

        List<MistakeRecord> mistakes = mistakeListRepo
                .findActiveMistakes(userId, null, 1, Integer.MAX_VALUE, cycle);
        if (mistakes.isEmpty()) {
            throw new BusinessException("NO_MISTAKES_TO_REVIEW",
                    "No active mistakes to review", HttpStatus.NOT_FOUND);
        }

        Map<Long, List<Long>> byTopic      = new LinkedHashMap<>();
        Map<Long, Integer>    wrongByTopic = new LinkedHashMap<>();
        for (MistakeRecord m : mistakes) {
            byTopic.computeIfAbsent(m.topicId(), k -> new ArrayList<>()).add(m.questionId());
            wrongByTopic.merge(m.topicId(), m.wrongCount(), Integer::sum);
        }

        Long packId = reviewRepo.createPack(userId, cycle);
        for (var entry : byTopic.entrySet()) {
            Long       topicId     = entry.getKey();
            List<Long> questionIds = entry.getValue();
            int        priority    = wrongByTopic.getOrDefault(topicId, 0);
            Long taskId = reviewRepo.createTask(packId, userId, topicId,
                    questionIds.size(), priority);
            for (Long qId : questionIds) {
                reviewRepo.addTaskQuestion(taskId, qId);
            }
        }
        return buildPackResult(packId);
    }

    public TaskQuestionsResult getTaskQuestions(Long taskId, Long userId, String language) {
        requireReviewAccess(userId);
        TaskRow task = requireTask(taskId, userId);

        List<Long> questionIds = reviewRepo.findQuestionIdsByTaskId(taskId);
        List<QuestionDetail> questions = questionIds.stream()
                .map(qId -> questionRepo.findByIdAndLanguage(qId, language)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Question not found: " + qId)))
                .toList();

        return new TaskQuestionsResult(taskId, task.taskType(), task.topicId(), questions);
    }

    @Transactional
    public ReviewAnswerResult submitAnswer(Long taskId, Long userId,
                                           Long questionId, Long variantId,
                                           String selectedKey, String language) {
        requireReviewAccess(userId);
        TaskRow task = requireTask(taskId, userId);

        if (reviewRepo.hasAnswer(taskId, questionId)) {
            throw new BusinessException("QUESTION_ALREADY_SUBMITTED",
                    "Question already answered in this task", HttpStatus.CONFLICT);
        }

        UserRepository.UserRow user = userRepo.findById(userId).orElse(null);
        // Use the language the question was shown in; fall back to user preference.
        String resolvedLanguage = (language != null && !language.isBlank())
                ? language
                : (user != null ? user.languagePreference() : "en");
        int cycle = user != null ? user.resetCount() : 0;

        QuestionDetail question = questionRepo
                .findByIdAndLanguage(questionId, resolvedLanguage)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question not found: " + questionId));

        boolean isCorrect = question.correctChoiceKey().equals(selectedKey);

        reviewRepo.saveReviewAttempt(taskId, userId, questionId, variantId, selectedKey, isCorrect);
        reviewRepo.markQuestionAnswered(taskId, questionId, isCorrect);
        reviewRepo.incrementCompletedCount(taskId);

        // Wrong answer: update/create mistake record. Correct answer: do NOT deactivate yet.
        // MistakeRecord deactivation is evaluated at task completion (completeTask) after
        // reviewing all questions, per docs/review-and-readiness-engine.md.
        if (!isCorrect) {
            mistakeRepo.upsertMistake(userId, questionId, question.topicId(), "review", cycle);
        }

        if ("pending".equals(task.status())) {
            reviewRepo.updateTaskStatus(taskId, "in_progress");
        }

        int answeredCount = task.completedQuestionCount() + 1;
        return new ReviewAnswerResult(questionId, isCorrect, question.correctChoiceKey(),
                question.explanation(), answeredCount, task.targetQuestionCount());
    }

    @Transactional
    public CompleteTaskResult completeTask(Long taskId, Long userId) {
        requireReviewAccess(userId);
        TaskRow task = requireTask(taskId, userId);
        int cycle = cycleFor(userId);

        // Mastery evaluation: deactivate MistakeRecords for correctly-answered questions.
        // Simplified MVP: answered correctly in this review task → deactivate mistake.
        // TODO(MASTERY): replace with full mastery check (topic ≥80% rate, last 8 related
        // questions ≥6 correct, per docs/parameters.md mastery_threshold) once query
        // infrastructure for recent-attempt history is in place.
        List<Long> correctQIds = reviewRepo.findCorrectlyAnsweredQuestionIds(taskId);
        for (Long qId : correctQIds) {
            mistakeListRepo.setActive(userId, qId, false, cycle);
        }

        reviewRepo.updateTaskStatus(taskId, "completed");

        // Close the pack once all tasks are done
        int remaining = reviewRepo.countIncompleteTasks(task.reviewPackId());
        if (remaining == 0) {
            reviewRepo.updatePackStatus(task.reviewPackId(), "completed");
        }

        return new CompleteTaskResult(taskId, true);
    }

    // ---------------------------------------------------------------
    // Result types
    // ---------------------------------------------------------------

    public record ReviewPackResult(
            Long packId, String status,
            int targetQuestionCount, int completedQuestionCount,
            List<TaskSummary> tasks) {}

    public record TaskSummary(
            Long taskId, Long topicId, String type, String status, int priority,
            int targetQuestionCount, int completedQuestionCount) {}

    public record TaskQuestionsResult(
            Long taskId, String taskType, Long topicId, List<QuestionDetail> questions) {}

    public record ReviewAnswerResult(
            Long questionId, boolean isCorrect, String correctChoiceKey,
            String explanation, int answeredCount, int targetCount) {}

    public record CompleteTaskResult(Long taskId, boolean completed) {}

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private int cycleFor(Long userId) {
        return userRepo.findById(userId).map(u -> u.resetCount()).orElse(0);
    }

    private void requireReviewAccess(Long userId) {
        if (!accessService.getAccess(userId).canUseReview()) {
            throw new BusinessException("ACCESS_DENIED",
                    "Active access pass required to use review",
                    HttpStatus.FORBIDDEN);
        }
    }

    private TaskRow requireTask(Long taskId, Long userId) {
        TaskRow task = reviewRepo.findTaskById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review task not found: " + taskId));
        if (!task.userId().equals(userId)) {
            throw new BusinessException("FORBIDDEN",
                    "Task belongs to a different user", HttpStatus.FORBIDDEN);
        }
        return task;
    }

    private ReviewPackResult buildPackResult(Long packId) {
        List<TaskRow> tasks = reviewRepo.findTasksByPackId(packId);
        List<TaskSummary> summaries = tasks.stream()
                .map(t -> new TaskSummary(t.id(), t.topicId(), t.taskType(),
                        t.status(), t.priority(),
                        t.targetQuestionCount(), t.completedQuestionCount()))
                .toList();
        int targetTotal    = summaries.stream().mapToInt(TaskSummary::targetQuestionCount).sum();
        int completedTotal = summaries.stream().mapToInt(TaskSummary::completedQuestionCount).sum();
        return new ReviewPackResult(packId, "active", targetTotal, completedTotal, summaries);
    }
}
