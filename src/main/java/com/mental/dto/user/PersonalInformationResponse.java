package com.mental.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalInformationResponse {

    // Basic Information
    private String fullname;
    private String email;
    private String username;
    private LocalDate birthday;

    // Account Status
    private String accountStatus;
    private String role;
    private LocalDateTime memberSince;
}