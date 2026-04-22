package com.dmvmotor.api.authaccess.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
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

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .build();
        return FirebaseApp.initializeApp(options);
    }
}
