package com.mental.dto;

import lombok.Data;

public record LoginRequest(
        String email,
        String password
){}
