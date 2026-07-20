package com.mental.mapper;

import com.mental.dto.UserDto;
import com.mental.model.entity.User;
import com.mental.model.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class UserMapper {

    public UserDto toAdminDto(User user) {
        UserProfile profile = user.getUserProfile();

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullname(profile != null ? profile.getFullname() : "N/A")
                .birthday(profile != null ? profile.getBirthday() : null)
                .avatarUrl(profile != null ? profile.getProfileImageUrl() : null)
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt() ).build();
    }
}