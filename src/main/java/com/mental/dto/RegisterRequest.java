package com.mental.dto;

import lombok.Data;

import java.util.Set;

public record RegisterRequest(
        String username,
        String email,
        String password
){}
