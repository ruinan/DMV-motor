package com.dmvmotor.api.mistakereview.application;

import com.dmvmotor.api.mistakereview.domain.MistakeRecord;
import com.dmvmotor.api.mistakereview.infrastructure.MistakeListRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MistakeService {

    private final MistakeListRepository mistakeListRepo;

    public MistakeService(MistakeListRepository mistakeListRepo) {
        this.mistakeListRepo = mistakeListRepo;
    }

    public MistakeListResult listMistakes(Long userId, Long topicId, int page, int pageSize) {
        List<MistakeRecord> items = mistakeListRepo.findActiveMistakes(userId, topicId, page, pageSize);
        int total = mistakeListRepo.countActiveMistakes(userId, topicId);
        return new MistakeListResult(items, page, pageSize, total);
    }

    public record MistakeListResult(
            List<MistakeRecord> items,
            int page,
            int pageSize,
            int total
    ) {}
}
