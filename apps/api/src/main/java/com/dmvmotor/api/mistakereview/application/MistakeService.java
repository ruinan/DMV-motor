package com.dmvmotor.api.mistakereview.application;

import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.common.ResourceNotFoundException;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.content.infrastructure.QuestionRepository;
import com.dmvmotor.api.mistakereview.domain.MistakeRecord;
import com.dmvmotor.api.mistakereview.infrastructure.MistakeListRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MistakeService {

    private final MistakeListRepository mistakeListRepo;
    private final QuestionRepository    questionRepo;
    private final UserRepository        userRepo;

    public MistakeService(MistakeListRepository mistakeListRepo,
                          QuestionRepository questionRepo,
                          UserRepository userRepo) {
        this.mistakeListRepo = mistakeListRepo;
        this.questionRepo    = questionRepo;
        this.userRepo        = userRepo;
    }

    /**
     * Full review detail for a single mistake: the question plus its correct
     * answer + explanation. Answers are normally hidden outside gameplay
     * endpoints; here they're safe to reveal because the gate proves the user
     * already got this question wrong (it's their active mistake).
     */
    public QuestionDetail getReview(Long userId, Long questionId, String language) {
        int cycle = userRepo.findById(userId).map(u -> u.resetCount()).orElse(0);
        if (!mistakeListRepo.existsActiveMistake(userId, questionId, cycle)) {
            throw new ResourceNotFoundException(
                    "No active mistake for question: " + questionId);
        }
        String lang = (language == null || language.isBlank()) ? "en" : language;
        return questionRepo.findByIdAndLanguage(questionId, lang)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question not found: " + questionId));
    }

    public MistakeListResult listMistakes(Long userId, Long topicId, int page, int pageSize) {
        int cycle = userRepo.findById(userId).map(u -> u.resetCount()).orElse(0);
        List<MistakeRecord> items = mistakeListRepo.findActiveMistakes(userId, topicId, page, pageSize, cycle);
        int total = mistakeListRepo.countActiveMistakes(userId, topicId, cycle);
        return new MistakeListResult(items, page, pageSize, total);
    }

    public record MistakeListResult(
            List<MistakeRecord> items,
            int page,
            int pageSize,
            int total
    ) {}
}
