package com.mental.mapper;

import com.mental.model.entity.UserGoal;
import org.springframework.stereotype.Component;

@Component
public class UserGoalMapper {

    // Return type ကို package အပြည့်အစုံဖြင့် ရေးပါ
    public com.mental.dto.goal.UserGoal toDto(UserGoal entity) {
        if(entity == null) {
            return null;
        }
        return com.mental.dto.goal.UserGoal.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .targetDate(entity.getTargetDate())
                .targetDays(entity.getTargetDays())
                .username(entity.getUser().getUsername())
                .progress(entity.getProgress())
                .email(entity.getUser().getEmail())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toLocalDate() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toLocalDate() : null)
                .completedAt(entity.getCompletedAt())
                .build();
    }

    public com.mental.model.entity.UserGoal toEntity(UserGoal dto, com.mental.model.entity.User user) {
        if (dto == null) {
            return null;
        }

        return com.mental.model.entity.UserGoal.builder()
                .id(dto.getId())
                .user(user)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .targetDays(dto.getTargetDays())
                .targetDate(dto.getTargetDate())
                .progress(dto.getProgress())
                .status(dto.getStatus())
                .completedAt(dto.getCompletedAt())
                .build();
    }
}