package com.dmvmotor.api.mistakereview.application;

import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.mistakereview.domain.MistakeRecord;
import com.dmvmotor.api.mistakereview.infrastructure.MistakeListRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MistakeService {

    private final MistakeListRepository mistakeListRepo;
    private final UserRepository        userRepo;

    public MistakeService(MistakeListRepository mistakeListRepo, UserRepository userRepo) {
        this.mistakeListRepo = mistakeListRepo;
        this.userRepo        = userRepo;
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
