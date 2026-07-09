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
}