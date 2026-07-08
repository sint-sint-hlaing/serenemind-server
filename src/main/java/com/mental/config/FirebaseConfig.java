package com.mental.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.logging.Logger;

@Configuration
public class FirebaseConfig {

    private static final Logger logger =
            Logger.getLogger(FirebaseConfig.class.getName());

    @PostConstruct
    public void initialize() {

        try {

            if (FirebaseApp.getApps().isEmpty()) {

                InputStream serviceAccount =
                        new ClassPathResource("serviceAccountKey.json")
                                .getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);

                logger.info("Firebase initialized successfully.");
            }

        } catch (Exception e) {
            logger.severe("Firebase initialization failed: " + e.getMessage());
        }
    }
}