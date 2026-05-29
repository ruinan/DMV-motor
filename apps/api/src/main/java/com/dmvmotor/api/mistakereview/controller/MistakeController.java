package com.dmvmotor.api.mistakereview.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.content.domain.QuestionDetail;
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
                        "page",      result.page(),
                        "page_size", result.pageSize(),
                        "total",     result.total()
                )
        );
    }

    /**
     * Review detail for one of the user's mistakes — the question plus its
     * correct answer + explanation, so the Mistakes page can be a real review
     * surface (not just a list of ids). Gated to the caller's active mistakes.
     */
    @GetMapping("/{questionId}/review")
    public ApiResponse<?> reviewMistake(
            @CurrentUser Long userId,
            @PathVariable Long questionId,
            @RequestParam(value = "language", required = false) String language
    ) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
        QuestionDetail q = mistakeService.getReview(userId, questionId, language);
        return ApiResponse.ok(Map.of(
                "question_id",        String.valueOf(q.questionId()),
                "variant_id",         String.valueOf(q.variantId()),
                "stem",               q.stem(),
                "choices",            q.choices(),
                "correct_choice_key", q.correctChoiceKey(),
                "explanation",        q.explanation() != null ? q.explanation() : ""
        ));
    }

    private Map<String, Object> toItemDto(MistakeRecord r) {
        return Map.of(
                "mistake_id",   String.valueOf(r.id()),
                "question_id",  String.valueOf(r.questionId()),
                "topic_id",     String.valueOf(r.topicId()),
                "wrong_count",  r.wrongCount(),
                "last_wrong_at", r.lastWrongAt().toString(),
                "source",       r.lastEntrySource()
        );
    }
}
