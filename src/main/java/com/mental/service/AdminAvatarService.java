package com.mental.service;

import com.mental.dto.AvatarResponse;
import com.mental.dto.MessageResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.AvatarMapper;
import com.mental.model.entity.Avatar;
import com.mental.repository.AvatarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminAvatarService {

    private final AvatarMapper avatarMapper;
    private final AvatarRepository avatarRepository;
    private final CloudinaryService cloudinaryService;
    public AvatarResponse createAvatar(String name, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Avatar image is required");
        }

        String imageUrl = cloudinaryService.uploadImage(image);

        Avatar avatar = new Avatar();
        avatar.setName(name);
        avatar.setImageUrl(imageUrl);
        avatar.setIsActive(true);

        avatarRepository.save(avatar);
        return avatarMapper.toResponse(avatar);
    }

    public AvatarResponse updateAvatar(Long id, String name, MultipartFile image) {
        Avatar avatar = avatarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avatar not found"));

        if (name != null && !name.isBlank()) {
            avatar.setName(name);
        }

        if (image != null && !image.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(image);
            avatar.setImageUrl(imageUrl);
        }

        return avatarMapper.toResponse(avatar);
    }

    public MessageResponse deleteAvatar(Long id) {
        Avatar avatar = avatarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avatar not found"));

        // Soft delete implementation
        avatar.setIsActive(false);
        avatarRepository.save(avatar);

        return MessageResponse.success("Avatar deleted successfully");
    }
}