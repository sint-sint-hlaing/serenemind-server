package com.mental.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProfileResponse {
    private String username;
    private String email;
    private String fullname;
    private String avatar;
    private LocalDate birthday;
    private int profileCompletionPercentage;
}
