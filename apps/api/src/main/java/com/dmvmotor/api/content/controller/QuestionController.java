package com.dmvmotor.api.content.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.content.application.ContentService;
import com.dmvmotor.api.content.domain.Choice;
import com.dmvmotor.api.content.domain.QuestionDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final ContentService contentService;

    public QuestionController(ContentService contentService) {
        this.contentService = contentService;
    }

    /**
     * Public-read of a question. Hard security contract:
     *   - Response NEVER carries {@code correct_choice_key} or
     *     {@code explanation}; answers flow only through gameplay endpoints.
     *   - Anonymous / free-trial / expired callers are filtered to the
     *     {@code allow_in_free_trial=true} pool by ContentService — paid bank
     *     IDs return 404 (not 403, to avoid leaking existence).
     *   - Inactive / draft questions are filtered out for everyone (404).
     */
    @GetMapping("/{id}")
    public ApiResponse<?> getQuestion(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "en") String language
    ) {
        QuestionDetail detail = contentService.getQuestion(userId, id, language);
        return ApiResponse.ok(PublicQuestionDto.from(detail));
    }

    record PublicQuestionDto(
            String questionId,
            String variantId,
            String topicId,
            String language,
            String stem,
            List<Choice> choices
    ) {
        static PublicQuestionDto from(QuestionDetail d) {
            return new PublicQuestionDto(
                    String.valueOf(d.questionId()),
                    String.valueOf(d.variantId()),
                    String.valueOf(d.topicId()),
                    d.language(),
                    d.stem(),
                    d.choices()
            );
        }
    }
}
