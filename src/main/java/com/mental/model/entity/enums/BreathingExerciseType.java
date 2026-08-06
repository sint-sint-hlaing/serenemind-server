package com.mental.model.entity.enums;

public enum BreathingExerciseType {
    BOX_BREATHING("Box Breathing", "Inhale, hold, exhale, hold - each for 4 seconds"),
    DEEP_BREATHING("Deep Breathing", "Slow, deep breaths focusing on diaphragm"),
    FOUR_SEVEN_EIGHT("4-7-8 Breathing", "Inhale 4s, hold 7s, exhale 8s"),
    ALTERNATE_NOSTRIL("Alternate Nostril", "Alternating breathing through nostrils");

    private final String displayName;
    private final String description;

    BreathingExerciseType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    // Getters
}