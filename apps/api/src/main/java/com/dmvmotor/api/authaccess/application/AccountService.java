package com.dmvmotor.api.authaccess.application;

import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.authaccess.infrastructure.UserRepository.UserRow;
import com.dmvmotor.api.common.ResourceNotFoundException;
import com.dmvmotor.api.mistakereview.review.infrastructure.ReviewRepository;
import com.dmvmotor.api.practice.infrastructure.MistakeRepository;
import com.dmvmotor.api.practice.infrastructure.PracticeSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final UserRepository            userRepo;
    private final AccessService             accessService;
    private final PracticeSessionRepository practiceSessionRepo;
    private final MistakeRepository         mistakeRepo;
    private final ReviewRepository          reviewRepo;

    public AccountService(UserRepository userRepo,
                          AccessService accessService,
                          PracticeSessionRepository practiceSessionRepo,
                          MistakeRepository mistakeRepo,
                          ReviewRepository reviewRepo) {
        this.userRepo            = userRepo;
        this.accessService       = accessService;
        this.practiceSessionRepo = practiceSessionRepo;
        this.mistakeRepo         = mistakeRepo;
        this.reviewRepo          = reviewRepo;
    }

    public MeResult getMe(Long userId) {
        UserRow user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        AccessService.AccessInfo access = accessService.getAccess(userId);
        boolean hasInProgressPractice = practiceSessionRepo.existsInProgressByUserId(userId);
        boolean hasInProgressReview   = reviewRepo.findActivePackId(userId).isPresent();

        return new MeResult(userId, user.email(), user.languagePreference(),
                access, hasInProgressPractice, hasInProgressReview);
    }

    public String updateLanguage(Long userId, String language) {
        userRepo.updateLanguage(userId, language);
        return language;
    }

    @Transactional
    public void resetLearning(Long userId) {
        practiceSessionRepo.deleteAllByUserId(userId);
        mistakeRepo.deleteAllByUserId(userId);
    }

    public record MeResult(
            Long   userId,
            String email,
            String language,
            AccessService.AccessInfo access,
            boolean hasInProgressPractice,
            boolean hasInProgressReview
    ) {}
}
