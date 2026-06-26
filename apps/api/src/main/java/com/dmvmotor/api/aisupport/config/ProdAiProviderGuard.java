package com.dmvmotor.api.aisupport.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fail-fast in production if the AI provider isn't the real one. The stub
 * provider is the default ({@code @ConditionalOnProperty(matchIfMissing=true)}),
 * so a dropped or mistyped {@code APP_AI_PROVIDER} env would otherwise let prod
 * boot happily and silently serve canned {@code stub:} explanations. Under the
 * {@code prod} profile we abort startup unless a real provider (deepseek) is
 * configured WITH a key.
 *
 * <p>No-op outside prod — dev/test deliberately run the stub to avoid LLM cost.
 */
@Component
@Profile("prod")
public class ProdAiProviderGuard implements InitializingBean {

    private final AiProperties props;

    public ProdAiProviderGuard(AiProperties props) {
        this.props = props;
    }

    @Override
    public void afterPropertiesSet() {
        validate(props);
    }

    /** Throws {@link IllegalStateException} (aborting startup) unless a real,
     *  keyed provider is configured. Package-private + static so it's unit-tested
     *  without booting a prod application context. */
    static void validate(AiProperties props) {
        String provider = props.provider() == null ? "" : props.provider().trim().toLowerCase();
        if (!"deepseek".equals(provider)) {
            throw new IllegalStateException(
                    "Refusing to start in prod with app.ai.provider='" + props.provider()
                    + "' — a real AI provider (deepseek) is required. Check the APP_AI_PROVIDER env.");
        }
        if (!StringUtils.hasText(props.deepseek().apiKey())) {
            throw new IllegalStateException(
                    "Refusing to start in prod: app.ai.provider=deepseek but no API key is set "
                    + "(app.ai.deepseek.api-key / DEEPSEEK_API_KEY).");
        }
    }
}
