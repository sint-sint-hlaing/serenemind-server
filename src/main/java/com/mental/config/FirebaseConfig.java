package com.mental.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = Logger.getLogger(FirebaseConfig.class.getName());

    @PostConstruct
    public void initialize() {
        String keyPath = "src/main/resources/serviceAccountKey.json";
        File file = new File(keyPath);
        if (!file.exists()) {
            logger.severe("Firebase serviceAccountKey.json not found at " + keyPath + ". Firebase operations will not work!");
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(file)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            logger.info("Firebase has been initialized successfully.");
        } catch (IOException e) {
            logger.severe("Failed to initialize Firebase: " + e.getMessage());
        }
    }
}
