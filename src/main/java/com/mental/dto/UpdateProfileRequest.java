package com.mental.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateProfileRequest {
    @NotBlank(message = "Full name cannot be empty")
    private String fullname;

    private String avatar;
    private LocalDate birthday;
}
