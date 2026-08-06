package com.mental.service;

import org.springframework.stereotype.Component;

@Component
public class BreathingConfig {

    // Configuration values can be loaded from application.yml
    private final int defaultDurationMinutes = 5;
    private final int maxDurationMinutes = 30;
    private final int minDurationMinutes = 1;

    public int getDefaultDurationMinutes() {
        return defaultDurationMinutes;
    }

    public int getMaxDurationMinutes() {
        return maxDurationMinutes;
    }

    public int getMinDurationMinutes() {
        return minDurationMinutes;
    }
}