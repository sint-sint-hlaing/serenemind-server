package com.mental.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Cloudinary configuration bean.
 *
 * Credentials are injected from application.properties:
 *   cloudinary.cloud-name
 *   cloudinary.api-key
 *   cloudinary.api-secret
 *
 * ⚠️ SECURITY: Never hard-code credentials here.
 *    Use environment variables or a secrets manager in production.
 *    Example .env / system env variable names:
 *      CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET
 */
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(Map.of(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true          // always use HTTPS URLs
        ));
    }
}
