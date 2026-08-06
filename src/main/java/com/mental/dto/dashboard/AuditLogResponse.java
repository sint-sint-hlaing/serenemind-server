package com.mental.dto.dashboard;

import lombok.Builder;

import java.time.LocalDateTime;

public record AuditLogResponse(

        Long id,

        String username,

        String action,

        String description,

        LocalDateTime createdAt

){}
