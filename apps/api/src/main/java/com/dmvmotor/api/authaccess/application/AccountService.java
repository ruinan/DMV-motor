package com.dmvmotor.api.authaccess.application;

import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.authaccess.infrastructure.UserRepository.UserRow;
import com.dmvmotor.api.aisupport.infrastructure.AiExplanationRepository;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.ResourceNotFoundException;
import com.dmvmotor.api.content.domain.Exam;
import com.dmvmotor.api.content.infrastructure.ExamRepository;
import com.dmvmotor.api.mistakereview.review.infrastructure.ReviewRepository;
import com.dmvmotor.api.practice.infrastructure.PracticeSessionRepository;
import com.dmvmotor.api.practice.infrastructure.PracticeSessionRepository.InProgressSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final UserRepository            userRepo;
    private final AccessService             accessService;
    private final PracticeSessionRepository practiceSessionRepo;
    private final ReviewRepository          reviewRepo;
    private final ExamRepository            examRepo;
    private final AiExplanationRepository   aiExplanationRepo;

    public AccountService(UserRepository userRepo,
                          AccessService accessService,
                          PracticeSessionRepository practiceSessionRepo,
                          ReviewRepository reviewRepo,
                          ExamRepository examRepo,
                          AiExplanationRepository aiExplanationRepo) {
        this.userRepo            = userRepo;
        this.accessService       = accessService;
        this.practiceSessionRepo = practiceSessionRepo;
        this.reviewRepo          = reviewRepo;
        this.examRepo            = examRepo;
        this.aiExplanationRepo   = aiExplanationRepo;
    }

    public MeResult getMe(Long userId) {
        UserRow user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        int cycle = user.resetCount();
        AccessService.AccessInfo access = accessService.getAccess(userId);
        InProgressSession inProgress = practiceSessionRepo.findInProgressByUser(userId, cycle).orElse(null);
        boolean hasInProgressReview   = reviewRepo.findActivePackId(userId, cycle).isPresent();
        // Null until the user has picked an exam (onboarding); existing users
        // were backfilled to CA-M1 by V26.
        Exam currentExam = user.currentExamId() == null
                ? null
                : examRepo.findById(user.currentExamId()).orElse(null);

        return new MeResult(userId, user.email(), user.languagePreference(),
                access, inProgress, hasInProgressReview, currentExam);
    }

    public String updateLanguage(Long userId, String language) {
        userRepo.updateLanguage(userId, language);
        return language;
    }

    /** Set the user's current exam. Rejects unknown / inactive exams. */
    @Transactional
    public Exam updateExam(Long userId, Long examId) {
        Exam exam = examRepo.findById(examId)
                .filter(e -> "active".equals(e.status()))
                .orElseThrow(() -> new BusinessException("INVALID_EXAM",
                        "No such active exam: " + examId, HttpStatus.BAD_REQUEST));
        userRepo.updateCurrentExam(userId, examId);
        return exam;
    }

    @Transactional
    public void resetLearning(Long userId) {
        // Soft reset: increment cycle counter so all current-cycle data becomes invisible.
        // Historical data (practice sessions, mistakes, review packs) is preserved.
        userRepo.incrementResetCount(userId);
        // AI explanation cache is keyed by question (not cycle), so it would keep
        // serving old cached answers after a reset — drop it for a clean slate
        // (B22; the browser-side deep-dive threads are cleared client-side too).
        aiExplanationRepo.deleteForUser(userId);
    }

    public record MeResult(
            Long   userId,
            String email,
            String language,
            AccessService.AccessInfo access,
            InProgressSession inProgressPractice,
            boolean hasInProgressReview,
            Exam currentExam
    ) {
        public boolean hasInProgressPractice() {
            return inProgressPractice != null;
        }
    }
}
