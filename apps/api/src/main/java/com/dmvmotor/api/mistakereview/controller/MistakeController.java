package com.dmvmotor.api.mistakereview.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.mistakereview.application.MistakeService;
import com.dmvmotor.api.mistakereview.application.MistakeService.MistakeListResult;
import com.dmvmotor.api.mistakereview.domain.MistakeRecord;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mistakes")
public class MistakeController {

    private final MistakeService mistakeService;

    public MistakeController(MistakeService mistakeService) {
        this.mistakeService = mistakeService;
    }

    @GetMapping
    public ApiResponse<?> listMistakes(
            @CurrentUser Long userId,
            @RequestParam(value = "topic_id", required = false) Long topicId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize
    ) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }

        MistakeListResult result = mistakeService.listMistakes(userId, topicId, page, pageSize);

        List<Map<String, Object>> items = result.items().stream()
                .map(this::toItemDto)
                .toList();

        return ApiResponse.okWithMeta(
                Map.of("items", items),
                Map.of(
                        "page",     result.page(),
                        "pageSize", result.pageSize(),
                        "total",    result.total()
                )
        );
    }

    private Map<String, Object> toItemDto(MistakeRecord r) {
        return Map.of(
                "mistakeId",   String.valueOf(r.id()),
                "questionId",  String.valueOf(r.questionId()),
                "topicId",     String.valueOf(r.topicId()),
                "wrongCount",  r.wrongCount(),
                "lastWrongAt", r.lastWrongAt() != null ? r.lastWrongAt().toString() : "",
                "source",      r.lastEntrySource() != null ? r.lastEntrySource() : ""
        );
    }
}
