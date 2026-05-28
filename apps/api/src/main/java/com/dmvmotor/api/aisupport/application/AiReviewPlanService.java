package com.dmvmotor.api.aisupport.application;

import com.dmvmotor.api.aisupport.config.AiProperties;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.AttemptRow;
import com.dmvmotor.api.mockexam.infrastructure.MockExamRepository.WrongAnswerDetail;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Generates (and caches) an AI review plan for a completed mock-exam attempt.
 *
 * <p>Guards, in order:
 * <ol>
 *   <li>AI feature flag — {@code app.ai.enabled=false} → AI_UNAVAILABLE.</li>
 *   <li>Ownership — the attempt must belong to the caller (else 403/404 via
 *       the same not-found-for-cross-user semantics as the rest of mockexam).</li>
 *   <li>Completion — only terminal attempts (submitted / ended_by_failure /
 *       ended_by_exit) get a plan; an in-progress exam has nothing to review.</li>
 *   <li>Cache — one plan per attempt, persisted on the attempt row. A second
 *       request returns the saved plan with no DeepSeek cost.</li>
 * </ol>
 */
@Service
public class AiReviewPlanService {

    private static final double PASS_THRESHOLD = 0.85;

    private final MockExamRepository  mockExamRepo;
    private final AiReviewPlanProvider provider;
    private final AiProperties        props;

    public AiReviewPlanService(MockExamRepository mockExamRepo,
                                AiReviewPlanProvider provider,
                                AiProperties props) {
        this.mockExamRepo = mockExamRepo;
        this.provider     = provider;
        this.props        = props;
    }

    public Result generate(Long attemptId, Long userId, String language) {
        if (!props.enabled()) {
            throw new BusinessException("AI_UNAVAILABLE",
                    "AI review plans are currently turned off",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        AttemptRow attempt = mockExamRepo.findAttemptById(attemptId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND",
                        "Mock attempt not found: " + attemptId, HttpStatus.NOT_FOUND));
        if (!attempt.userId().equals(userId)) {
            throw new BusinessException("FORBIDDEN",
                    "Attempt belongs to a different user", HttpStatus.FORBIDDEN);
        }
        if ("in_progress".equals(attempt.status())) {
            throw new BusinessException("MOCK_NOT_COMPLETED",
                    "Finish the exam before requesting a review plan",
                    HttpStatus.CONFLICT);
        }

        // Cache hit — return the persisted plan, no provider call.
        Optional<String> cached = mockExamRepo.findReviewPlan(attemptId);
        if (cached.isPresent()) {
            return new Result(cached.get(), true);
        }

        String lang = language != null && !language.isBlank()
                ? language : attempt.language();

        int correctCount = mockExamRepo.countCorrectAnswers(attemptId);
        int total = mockExamRepo.findMockExamQuestionCount(attempt.mockExamId());
        int scorePercent = total == 0 ? 0 : (int) Math.round(100.0 * correctCount / total);
        boolean passed = scorePercent >= (int) Math.round(PASS_THRESHOLD * 100);

        List<WrongAnswerDetail> wrong = mockExamRepo.findWrongAnswerDetails(attemptId, lang);
        List<AiReviewPlanProvider.WrongItem> wrongItems = wrong.stream()
                .map(w -> new AiReviewPlanProvider.WrongItem(
                        w.stem(), w.topicLabel(), w.subTopicLabel(),
                        w.selectedChoiceKey(), w.correctChoiceKey()))
                .toList();

        AiReviewPlanProvider.Output out = provider.generate(
                new AiReviewPlanProvider.Input(
                        scorePercent, correctCount, total, passed, wrongItems, lang));

        mockExamRepo.saveReviewPlan(attemptId, out.text(), provider.modelName());
        return new Result(out.text(), false);
    }

    public record Result(String plan, boolean cached) {}
}
