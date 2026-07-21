package com.mental.dto.meditation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

@Builder
public record MeditationRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Category is required")
        String category,
        @Min(value = 1, message = "Duration must be at least 1 minute")
        String duration,
        @NotNull(message = "Difficulty is required") // 👈 ၁။ ဒီလိုင်း အသစ်ထည့်ပါ
        Integer difficulty,
        MultipartFile audioFile, // Audio File လက်ခံရန်
        MultipartFile imageFile
) {
}
