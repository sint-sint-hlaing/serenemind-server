package com.mental.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private String fullname;
    private String email;
    private String avatar;
    private String bio;
    private String username;
    private LocalDate birthday;
}
