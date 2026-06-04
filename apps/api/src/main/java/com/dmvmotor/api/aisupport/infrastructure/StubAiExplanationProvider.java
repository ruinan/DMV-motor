package com.dmvmotor.api.aisupport.infrastructure;

import com.dmvmotor.api.aisupport.application.AiExplanationProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default AI provider — active when {@code app.ai.provider=stub} (or unset).
 * Returns a deterministic string keyed off question + language so cache-hit
 * tests can assert "the second call returned the same text". Tracks the
 * call count so tests can verify cache short-circuiting.
 *
 * <p>Phase B will add a DeepSeek-backed sibling guarded by
 * {@code app.ai.provider=deepseek}.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider",
        havingValue = "stub", matchIfMissing = true)
public class StubAiExplanationProvider implements AiExplanationProvider {

    private final AtomicInteger callCount = new AtomicInteger(0);

    @Override
    public Output explain(Input in) {
        callCount.incrementAndGet();
        // depth 0 keeps the original string (existing cache-hit tests assert it);
        // deep-dive layers append :depth=N (and :aspect=X when directed) so
        // tests can tell layers apart.
        String text = "stub:explanation:q=" + in.questionId() + ":lang=" + in.language()
                + (in.depth() > 0 ? ":depth=" + in.depth() : "")
                + (in.aspect() != null && !in.aspect().isBlank() ? ":aspect=" + in.aspect() : "")
                // The service always resolves a non-null exam label; echo it so
                // wiring tests can assert which exam reached the provider.
                + ":exam=" + in.examLabel();
        return new Output(text, 0, 0);
    }

    @Override
    public String modelName() {
        return "stub";
    }

    public int callCount() {
        return callCount.get();
    }

    public void resetCallCount() {
        callCount.set(0);
    }
}
