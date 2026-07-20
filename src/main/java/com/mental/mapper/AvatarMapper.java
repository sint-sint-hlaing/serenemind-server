package com.mental.mapper;

import com.mental.dto.AvatarResponse;
import com.mental.model.entity.Avatar;
import org.springframework.stereotype.Component;

@Component
public class AvatarMapper {


    public AvatarResponse toResponse(Avatar avatar) {

        if (avatar == null) {
            return null;
        }


        return new AvatarResponse(
                avatar.getId(),
                avatar.getName(),
                avatar.getImageUrl(),
                avatar.getIsActive(),
                avatar.getCreatedAt()
        );
    }
}