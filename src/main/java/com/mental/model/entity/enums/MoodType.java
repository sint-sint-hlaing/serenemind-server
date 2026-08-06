package com.mental.model.entity.enums;

import lombok.Getter;

@Getter
public enum MoodType {
    HAPPY("😊", 90, "Keep smiling today"),
    CALM("😌", 85, "Stay peaceful and relaxed"),
    NEUTRAL("🌱", 60, "Today is a fresh start"),
    SAD("💙", 40, "Take care of yourself"),
    ANXIOUS("🌿", 35, "Take a deep breath"),
    ANGRY("❤️", 20, "Relax and stay calm");

    private final String emoji;
    private final int percentage;
    private final String message;

    MoodType(String emoji, int percentage, String message) {
        this.emoji = emoji;
        this.percentage = percentage;
        this.message = message;
    }
}
