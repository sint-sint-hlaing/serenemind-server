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

    // application.properties ထဲက keys တွေကို ဖတ်ပြီးတာနဲ့ Cloudinary Object ကို အလိုအလျောက် တည်ဆောက်ပေးခြင်း
    @PostConstruct
    public void init() {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    /**
     * Frontend မှ ပေးပို့လိုက်သော ပုံကို Cloudinary သို့ တင်ပေးပြီး Secure URL (https://...) ပြန်ပေးမည့် Method
     */
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null; // ပုံမပါလာပါက null သာ ပြန်မည်
        }
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.emptyMap()
            );

            // Cloudinary မှ ပေးသော secure_url ကို ဆွဲထုတ်ပြီး String အနေဖြင့် ပြန်ပေးခြင်း
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Cloudinary သို့ ပုံတင်ခြင်း မအောင်မြင်ပါ- ", e);
        }
    }


    /**
     * Delete an image from Cloudinary using its secure URL.
     */
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

            String sub = url.substring(uploadIndex + 8); // strip up to "/upload/"

            // If it starts with a version path (e.g. v1571218039/), skip it
            if (sub.startsWith("v") && sub.indexOf('/') != -1 && sub.substring(1, sub.indexOf('/')).matches("\\d+")) {
                sub = sub.substring(sub.indexOf('/') + 1);
            }

            // Strip file extension
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
            // RuntimeException မှာ message နဲ့ cause (e) ကိုသာ ထည့်ပေးရပါမယ်
            throw new RuntimeException("Cloudinary သို့ ဖိုင်တင်ခြင်း မအောင်မြင်ပါ: " + e.getMessage(), e);
        }
    }
}
