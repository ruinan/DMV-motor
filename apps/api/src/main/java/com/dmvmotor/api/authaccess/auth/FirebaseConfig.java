package com.dmvmotor.api.authaccess.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Initializes the {@link FirebaseApp} singleton used by the Admin SDK. Gated on
 * {@code app.auth.firebase.enabled=true} so that dev/test runs (stub verifier)
 * never reach for Application Default Credentials.
 *
 * <p>On Cloud Run, {@code GoogleCredentials.getApplicationDefault()} reads the
 * runtime metadata server — no service-account JSON key is shipped in the image.
 * Locally, point {@code GOOGLE_APPLICATION_CREDENTIALS} at a downloaded key, or
 * simply leave Firebase disabled and rely on the stub verifier.
 */
@Configuration
@ConditionalOnProperty(name = "app.auth.firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    private final String projectId;

    public FirebaseConfig(@Value("${app.auth.firebase.project-id:}") String projectId) {
        this.projectId = projectId;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault());
        // ComputeEngineCredentials on Cloud Run carries no project_id, and
        // GOOGLE_CLOUD_PROJECT is not auto-injected — verifyIdToken() needs an
        // explicit projectId or it throws IllegalStateException at first call.
        if (projectId != null && !projectId.isBlank()) {
            builder.setProjectId(projectId);
        }
        return FirebaseApp.initializeApp(builder.build());
    }
}
