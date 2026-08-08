package com.mental.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

   @PostConstruct
    public void init() {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }


    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.emptyMap()
            );

            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Cloudinary သို့ ပုံတင်ခြင်း မအောင်မြင်ပါ- ", e);
        }
    }



    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        try {
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image from Cloudinary", e);
        }
    }

    private String extractPublicId(String url) {
        try {
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) return null;

            String sub = url.substring(uploadIndex + 8);


            if (sub.startsWith("v") && sub.indexOf('/') != -1 && sub.substring(1, sub.indexOf('/')).matches("\\d+")) {
                sub = sub.substring(sub.indexOf('/') + 1);
            }


            int dotIndex = sub.lastIndexOf('.');
            if (dotIndex != -1) {
                sub = sub.substring(0, dotIndex);
            }
            return sub;
        } catch (Exception e) {
            return null;
        }
    }

    public String storeFile(MultipartFile multipartFile, String audios) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }
        try {
            Map<?, ?> uploadRequest = cloudinary.uploader().upload(
                    multipartFile.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "auto",
                            "folder", audios
                    )
            );
            return uploadRequest.get("secure_url").toString();

        } catch (Exception e) {

            throw new RuntimeException("Cloudinary သို့ ဖိုင်တင်ခြင်း မအောင်မြင်ပါ: " + e.getMessage(), e);
        }
    }
}
