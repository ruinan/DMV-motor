package com.dmvmotor.api.aisupport.infrastructure;

import com.dmvmotor.api.aisupport.application.AiReviewPlanProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default review-plan provider — active when {@code app.ai.provider=stub}
 * (or unset). Returns a deterministic, content-aware string so the flow can
 * be exercised end-to-end without a DeepSeek call.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider",
        havingValue = "stub", matchIfMissing = true)
public class StubAiReviewPlanProvider implements AiReviewPlanProvider {

    @Override
    public Output generate(Input in) {
        StringBuilder sb = new StringBuilder();
        sb.append("stub:review-plan score=").append(in.scorePercent())
          .append("% passed=").append(in.passed())
          .append(" wrong=").append(in.wrongItems().size())
          // The service always resolves a non-null exam label; echo it.
          .append(" exam=").append(in.examLabel());
        if (!in.wrongItems().isEmpty()) {
            sb.append(" topics=");
            in.wrongItems().forEach(w -> sb.append(w.topicLabel()).append(","));
        }
        return new Output(sb.toString(), 0, 0);
    }

    @Override
    public String modelName() {
        return "stub";
    }
}
