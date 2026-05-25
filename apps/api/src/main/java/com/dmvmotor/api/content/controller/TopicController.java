package com.dmvmotor.api.content.controller;

import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.content.application.ContentService;
import com.dmvmotor.api.content.domain.Topic;
import com.dmvmotor.api.progressreadiness.application.MasteryViewService;
import com.dmvmotor.api.progressreadiness.application.MasteryViewService.TopicMasteryView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/topics")
public class TopicController {

    private final ContentService contentService;
    private final MasteryViewService masteryViewService;

    public TopicController(ContentService contentService, MasteryViewService masteryViewService) {
        this.contentService = contentService;
        this.masteryViewService = masteryViewService;
    }

    @GetMapping
    public ApiResponse<?> listTopics() {
        List<TopicDto> items = contentService.listTopics().stream()
                .map(TopicDto::from)
                .toList();
        return ApiResponse.ok(Map.of("items", items));
    }

    @GetMapping("/mastery")
    public ApiResponse<?> getMastery(@CurrentUser Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required", HttpStatus.UNAUTHORIZED);
        }
        TopicMasteryView view = masteryViewService.build(userId);
        return ApiResponse.ok(Map.of(
                "topics", view.topics().stream().map(TopicMasteryDto::from).toList(),
                "summary", Map.of(
                        "total_sub_topics", view.totalSubTopics(),
                        "mastered_sub_topics", view.masteredSubTopics())));
    }

    record TopicMasteryDto(
            String topicId,
            String code,
            String nameEn,
            String nameZh,
            boolean isMastered,
            List<SubTopicMasteryDto> subTopics
    ) {
        static TopicMasteryDto from(MasteryViewService.TopicView t) {
            return new TopicMasteryDto(
                    String.valueOf(t.topicId()),
                    t.code(),
                    t.nameEn(),
                    t.nameZh(),
                    t.isMastered(),
                    t.subTopics().stream().map(SubTopicMasteryDto::from).toList());
        }
    }

    record SubTopicMasteryDto(
            String subTopicId,
            String code,
            String nameEn,
            String nameZh,
            boolean isMastered,
            int attemptedCount,
            int correctCount,
            int bankSize
    ) {
        static SubTopicMasteryDto from(MasteryViewService.SubTopicView s) {
            return new SubTopicMasteryDto(
                    String.valueOf(s.subTopicId()),
                    s.code(),
                    s.nameEn(),
                    s.nameZh(),
                    s.isMastered(),
                    s.attemptedCount(),
                    s.correctCount(),
                    s.bankSize());
        }
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
