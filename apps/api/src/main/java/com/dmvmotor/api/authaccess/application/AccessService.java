package com.dmvmotor.api.authaccess.application;

import com.dmvmotor.api.authaccess.domain.AccessPass;
import com.dmvmotor.api.authaccess.infrastructure.AccessRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class AccessService {

    private final AccessRepository accessRepo;

    public AccessService(AccessRepository accessRepo) {
        this.accessRepo = accessRepo;
    }

    public AccessInfo getAccess(Long userId) {
        Optional<AccessPass> passOpt = accessRepo.findLatestPassByUserId(userId);

        if (passOpt.isEmpty()) {
            return new AccessInfo("free_trial", false, 0, false, false, null);
        }

        AccessPass pass = passOpt.get();
        boolean hasActive = pass.isActive();
        int mockRemaining = hasActive ? pass.mockRemaining() : 0;
        boolean canUseMock = hasActive && mockRemaining > 0;

        String state = hasActive ? "active" : "expired";
        return new AccessInfo(state, hasActive, mockRemaining, hasActive, canUseMock,
                pass.expiresAt());
    }

    public record AccessInfo(
            String         state,
            boolean        hasActivePass,
            int            mockRemaining,
            boolean        canUseReview,
            boolean        canUseMockExam,
            OffsetDateTime expiresAt
    ) {}
}
