package com.mental.dto;

import lombok.Data;

public record AuthResponse(

        String accessToken,
        String refreshToken

){}
