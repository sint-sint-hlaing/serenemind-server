package com.mental.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProfileResponse {
    private String fullname;
    private String email;
    private String avatar;
    private String bio;
    private String username;
    private LocalDate birthday;
}
