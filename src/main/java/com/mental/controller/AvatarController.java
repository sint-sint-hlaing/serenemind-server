package com.mental.controller;

import com.mental.dto.AvatarResponse;
import com.mental.service.AvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/avatars")
@RequiredArgsConstructor
public class AvatarController {
    private final AvatarService avatarService;

    @GetMapping
    public List<AvatarResponse> getActiveAvatars() {
        return avatarService.getActiveAvatars();
    }

    @GetMapping("/{id}")
    public AvatarResponse getAvatar(
            @PathVariable Long id) {

        return avatarService.getAvatar(id);
    }
}
