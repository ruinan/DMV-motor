package com.dmvmotor.api.authaccess.application;

import com.dmvmotor.api.authaccess.domain.AccessPass;
import com.dmvmotor.api.authaccess.infrastructure.AccessRepository;
import com.dmvmotor.api.content.application.ExamContext;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class AccessService {

    private final AccessRepository accessRepo;
    private final ExamContext      examContext;

    public AccessService(AccessRepository accessRepo, ExamContext examContext) {
        this.accessRepo  = accessRepo;
        this.examContext = examContext;
    }

    /** Access for the user's CURRENT exam — subscriptions are per exam (V32). */
    public AccessInfo getAccess(Long userId) {
        return getAccess(userId, examContext.resolveExamId(userId));
    }

    public AccessInfo getAccess(Long userId, Long examId) {
        OffsetDateTime now = OffsetDateTime.now();
        Optional<AccessPass> passOpt = accessRepo.findRelevantPassByUserId(userId, examId, now);

        if (passOpt.isEmpty()) {
            return new AccessInfo("free_trial", false, 0, false, false, null, null);
        }

        AccessPass pass = passOpt.get();
        boolean hasActive = pass.isActive(now);
        int mockRemaining = hasActive ? pass.mockRemaining() : 0;
        boolean canUseMock = hasActive && mockRemaining > 0;
        Long activePassId = hasActive ? pass.id() : null;

        String state = hasActive ? "active" : "expired";
        return new AccessInfo(state, hasActive, mockRemaining, hasActive, canUseMock,
                pass.expiresAt(), activePassId);
    }

    /**
     * Snapshot of a user's access state. {@code activePassId} is the id of
     * the row that drives {@code hasActivePass=true}; null otherwise. Use it
     * for row-targeted updates such as decrementing mock-exam quota — the
     * earlier "match all status='active' rows" pattern double-counted when
     * a user owned more than one active pass.
     */
    public record AccessInfo(
            String         state,
            boolean        hasActivePass,
            int            mockRemaining,
            boolean        canUseReview,
            boolean        canUseMockExam,
            OffsetDateTime expiresAt,
            Long           activePassId
    ) {}
}
