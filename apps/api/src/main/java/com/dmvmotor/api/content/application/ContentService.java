package com.dmvmotor.api.content.application;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.common.ResourceNotFoundException;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.content.domain.Topic;
import com.dmvmotor.api.content.infrastructure.QuestionRepository;
import com.dmvmotor.api.content.infrastructure.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentService {

    private final TopicRepository    topicRepository;
    private final QuestionRepository questionRepository;
    private final AccessService      accessService;

    public ContentService(TopicRepository topicRepository,
                           QuestionRepository questionRepository,
                           AccessService accessService) {
        this.topicRepository    = topicRepository;
        this.questionRepository = questionRepository;
        this.accessService      = accessService;
    }

    public List<Topic> listTopics() {
        return topicRepository.findAllOrderBySortOrder();
    }

    /**
     * Public read of a question. The repository filters by status='active';
     * additionally, callers without an active access pass (anonymous,
     * free-trial, expired) only see the documented free-trial pool. The
     * controller layer further strips {@code correctChoiceKey} and
     * {@code explanation} from the wire response — answers are revealed
     * only through the gameplay endpoints (Practice / Review / Mock submit).
     */
    public QuestionDetail getQuestion(Long userId, Long questionId, String language) {
        boolean hasActivePass = userId != null
                && accessService.getAccess(userId).hasActivePass();

        var found = hasActivePass
                ? questionRepository.findByIdAndLanguage(questionId, language)
                : questionRepository.findFreeTrialActiveByIdAndLanguage(questionId, language);

        return found.orElseThrow(() -> new ResourceNotFoundException(
                "Question not found: id=" + questionId + ", language=" + language));
    }
}
