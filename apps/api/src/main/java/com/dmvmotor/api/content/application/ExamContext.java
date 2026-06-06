package com.dmvmotor.api.content.application;

import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.content.infrastructure.ExamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Resolves which exam a request is scoped to — the user's chosen
 * {@code current_exam_id}, or the default active exam when the user hasn't
 * picked one yet (onboarding not done) or the caller is anonymous.
 *
 * <p>Centralized so practice, mock, recommendations, topics and the mastery
 * donut all answer "which exam?" the same way. With a single seeded exam this
 * always resolves to CA-M1; the indirection is what makes the system
 * multi-exam-ready without each call site re-deriving the rule.
 */
@Component
public class ExamContext {

    private final UserRepository userRepo;
    private final ExamRepository examRepo;

    public ExamContext(UserRepository userRepo, ExamRepository examRepo) {
        this.userRepo = userRepo;
        this.examRepo = examRepo;
    }

    /**
     * The exam id to scope this request to. Authenticated users with a chosen
     * exam get theirs; everyone else (anonymous, or signed in but pre-onboarding)
     * falls back to the default active exam.
     */
    public Long resolveExamId(Long userId) {
        if (userId != null) {
            Long current = userRepo.findById(userId)
                    .map(UserRepository.UserRow::currentExamId)
                    .orElse(null);
            if (current != null) return current;
        }
        return defaultExamId();
    }

    /**
     * Exam scope when the caller may name the exam they want — the landing-page
     * "choose, then practice" flow for anonymous visitors. Signed-in users ALWAYS
     * use their server-side current exam (the requested id is ignored; switching
     * is a {@code /me/exam} action, not a per-request override — so this can't be
     * used to bypass a user's chosen scope). Anonymous callers get the requested
     * exam if it's a real active one, else the default.
     */
    public Long resolveExamId(Long userId, Long requestedExamId) {
        if (userId != null) return resolveExamId(userId);
        if (requestedExamId != null
                && examRepo.findById(requestedExamId)
                        .map(e -> "active".equals(e.status()))
                        .orElse(false)) {
            return requestedExamId;
        }
        return defaultExamId();
    }

    public Long defaultExamId() {
        return examRepo.findDefaultActiveId()
                .orElseThrow(() -> new BusinessException("NO_EXAM_CONFIGURED",
                        "No active exam is configured", HttpStatus.UNPROCESSABLE_ENTITY));
    }
}
