package com.dmvmotor.api.common.recaptcha;

import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Real reCAPTCHA Enterprise verifier — creates an assessment via the REST API,
 * authenticated with Application Default Credentials (the Cloud Run service
 * account in prod). Only a bean when {@code app.recaptcha.enabled=true}, so dev /
 * test never construct it. The single class that touches GCP — excluded from
 * coverage (mirrors {@code StripeGatewayImpl}); its logic is exercised in prod
 * smoke tests, while the verification GATE is fake-tested.
 *
 * <p>Fails closed: any error → an invalid, zero-score assessment, so the guard
 * rejects rather than letting a bad call through.
 */
@Component
@ConditionalOnProperty(name = "app.recaptcha.enabled", havingValue = "true")
public class RecaptchaEnterpriseVerifier implements RecaptchaVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(RecaptchaEnterpriseVerifier.class);
    private static final String SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    private final RecaptchaProperties props;
    private final RestClient http;

    public RecaptchaEnterpriseVerifier(RecaptchaProperties props) {
        this.props = props;
        this.http  = RestClient.builder()
                .baseUrl("https://recaptchaenterprise.googleapis.com/v1")
                .build();
    }

    @Override
    public Assessment verify(String token, String expectedAction) {
        try {
            Map<String, Object> body = Map.of("event", Map.of(
                    "token", token,
                    "siteKey", props.siteKey(),
                    "expectedAction", expectedAction));

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = http.post()
                    .uri("/projects/{p}/assessments", props.projectId())
                    .header("Authorization", "Bearer " + accessToken())
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (resp == null) return new Assessment(false, 0.0, "empty");

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenProps = (Map<String, Object>) resp.get("tokenProperties");
            boolean valid = tokenProps != null && Boolean.TRUE.equals(tokenProps.get("valid"));

            @SuppressWarnings("unchecked")
            Map<String, Object> risk = (Map<String, Object>) resp.get("riskAnalysis");
            double score = (risk != null && risk.get("score") instanceof Number n) ? n.doubleValue() : 0.0;

            String reason = valid ? "ok"
                    : String.valueOf(tokenProps != null ? tokenProps.get("invalidReason") : "unknown");
            return new Assessment(valid, score, reason);
        } catch (Exception e) {
            LOG.warn("reCAPTCHA assessment failed: {}", e.getMessage());
            return new Assessment(false, 0.0, "error"); // fail closed
        }
    }

    private String accessToken() throws Exception {
        GoogleCredentials creds = GoogleCredentials.getApplicationDefault().createScoped(SCOPE);
        creds.refreshIfExpired();
        return creds.getAccessToken().getTokenValue();
    }
}
