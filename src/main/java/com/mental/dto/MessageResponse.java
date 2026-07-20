package com.mental.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        //int status,

        boolean success,

        String message,
        LocalDateTime timestamp

) {

    public static MessageResponse success(String message) {
        return new MessageResponse(true, message, LocalDateTime.now());
    }

    public static MessageResponse error(String message) {
        return new MessageResponse(false, message, LocalDateTime.now());
    }
}
