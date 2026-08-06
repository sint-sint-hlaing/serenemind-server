package com.mental.service;

import com.mental.dto.AvatarResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.AvatarMapper;
import com.mental.model.entity.Avatar;
import com.mental.repository.AvatarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Read-only transactions for better performance
public class AvatarService {

    private final AvatarRepository avatarRepository;
    private final AvatarMapper avatarMapper;

    public List<AvatarResponse> getAllAvatars() {
        return avatarRepository.findAll()
                .stream()
                .map(avatarMapper::toResponse)
                .toList();
    }

    /**
     * Active ဖြစ်နေသော Avatar များသာ ယူရန် (Refactored from AdminAvatarService)
     */
    public List<AvatarResponse> getActiveAvatars() {
        return avatarRepository.findByIsActiveTrue()
                .stream()
                .map(avatarMapper::toResponse)
                .toList();
    }

    public AvatarResponse getAvatar(Long id) {
        Avatar avatar = avatarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avatar not found with id: " + id));
        return avatarMapper.toResponse(avatar);
    }
}