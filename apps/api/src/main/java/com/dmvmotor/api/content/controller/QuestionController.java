package com.dmvmotor.api.content.controller;

import com.dmvmotor.api.common.ApiResponse;
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

    @GetMapping("/{id}")
    public ApiResponse<?> getQuestion(
            @PathVariable Long id,
            @RequestParam(defaultValue = "en") String language
    ) {
        QuestionDetail detail = contentService.getQuestion(id, language);
        return ApiResponse.ok(QuestionDto.from(detail));
    }

    record QuestionDto(
            String questionId,
            String topicId,
            String correctChoiceKey,
            String language,
            String stem,
            List<Choice> choices,
            String explanation
    ) {
        static QuestionDto from(QuestionDetail d) {
            return new QuestionDto(
                    String.valueOf(d.questionId()),
                    String.valueOf(d.topicId()),
                    d.correctChoiceKey(),
                    d.language(),
                    d.stem(),
                    d.choices(),
                    d.explanation()
            );
        }
    }
}
