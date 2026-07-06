package com.mental.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class JournalResponse {
    private Long id;
    private String title;
    private String content; // Decrypt လုပ်ပြီးသား Rich Text ကို ပြန်ပေးမည်
    private LocalDateTime createdAt;
}
