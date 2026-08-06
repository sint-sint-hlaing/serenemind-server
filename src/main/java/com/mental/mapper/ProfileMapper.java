package com.mental.mapper;

import com.mental.dto.ProfileResponse;
import com.mental.model.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProfileMapper {


    public ProfileResponse toResponse(
            UserProfile profile
    ) {

        return ProfileResponse.builder()

                .id(profile.getId())

                .fullname(profile.getFullname())


                .profileImageUrl(
                        profile.getProfileImageUrl()
                )

                .avatarId(
                        profile.getAvatar() != null
                                ? profile.getAvatar().getId()
                                : null
                )

                .avatarUrl(
                        profile.getAvatar() != null
                                ? profile.getAvatar().getImageUrl()
                                : null
                )

                .createdAt(LocalDateTime.from(profile.getCreatedAt()))

                .updatedAt(LocalDateTime.from(profile.getUpdatedAt()))

                .build();
    }
}