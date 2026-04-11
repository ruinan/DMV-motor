package com.dmvmotor.api.content.application;

import com.dmvmotor.api.common.ResourceNotFoundException;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.content.domain.Topic;
import com.dmvmotor.api.content.infrastructure.QuestionRepository;
import com.dmvmotor.api.content.infrastructure.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;

    public ContentService(TopicRepository topicRepository, QuestionRepository questionRepository) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
    }

    public List<Topic> listTopics() {
        return topicRepository.findAllOrderBySortOrder();
    }

    public QuestionDetail getQuestion(Long questionId, String language) {
        return questionRepository.findByIdAndLanguage(questionId, language)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question not found: id=" + questionId + ", language=" + language));
    }
}
