package com.dmvmotor.api.aisupport.config;

import com.dmvmotor.api.aisupport.config.AiProperties.Deepseek;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prod fail-fast guard: a dropped {@code APP_AI_PROVIDER} (→ default stub) or a
 * missing key must abort startup rather than silently serve stub explanations.
 * Calls the package-private {@code validate} directly — no prod context needed.
 */
class ProdAiProviderGuardTest {

    private static AiProperties props(String provider, String apiKey) {
        return new AiProperties(true, provider, 3, 0, 10, 50, 10,
                new Deepseek(apiKey, "https://api.deepseek.com", "deepseek-chat", 400, 30));
    }

    @Test
    void stubProvider_abortsStartup() {
        assertThatThrownBy(() -> ProdAiProviderGuard.validate(props("stub", "key")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deepseek");
    }

    @Test
    void missingProvider_abortsStartup() {
        assertThatThrownBy(() -> ProdAiProviderGuard.validate(props(null, "key")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deepseekWithoutKey_abortsStartup() {
        assertThatThrownBy(() -> ProdAiProviderGuard.validate(props("deepseek", "  ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("key");
    }

    @Test
    void deepseekWithKey_startsCleanly() {
        assertThatCode(() -> ProdAiProviderGuard.validate(props("deepseek", "sk-real-key")))
                .doesNotThrowAnyException();
    }

    @Test
    void providerMatchIsCaseAndSpaceInsensitive() {
        assertThatCode(() -> ProdAiProviderGuard.validate(props("  DeepSeek ", "sk-real-key")))
                .doesNotThrowAnyException();
    }
}
