package com.tracker.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;

/**
 * Initializes the FirebaseApp singleton once at startup, from a service
 * account JSON file path (never the file contents itself, and never
 * hardcoded - see FIREBASE_CREDENTIALS_PATH in application.yml).
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials-path}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FileInputStream serviceAccount = new FileInputStream(credentialsPath);

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            }
        } catch (Exception e) {
            // Don't crash the whole app if Firebase creds are missing/bad
            // during local dev - FCM push will just silently no-op instead.
            log.error("Firebase initialization failed - FCM push will be unavailable: {}", e.getMessage());
        }
    }
}