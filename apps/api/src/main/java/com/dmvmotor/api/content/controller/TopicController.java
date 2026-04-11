package com.dmvmotor.api.content.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.content.application.ContentService;
import com.dmvmotor.api.content.domain.Topic;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/topics")
public class TopicController {

    private final ContentService contentService;

    public TopicController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public ApiResponse<?> listTopics() {
        List<TopicDto> items = contentService.listTopics().stream()
                .map(TopicDto::from)
                .toList();
        return ApiResponse.ok(Map.of("items", items));
    }

    record TopicDto(
            String id,
            String parentTopicId,
            String code,
            String nameEn,
            String nameZh,
            boolean isKeyTopic,
            String riskLevel,
            int sortOrder
    ) {
        static TopicDto from(Topic t) {
            return new TopicDto(
                    String.valueOf(t.id()),
                    t.parentTopicId() != null ? String.valueOf(t.parentTopicId()) : null,
                    t.code(),
                    t.nameEn(),
                    t.nameZh(),
                    t.isKeyTopic(),
                    t.riskLevel(),
                    t.sortOrder()
            );
        }
    }
}
