package com.mental.controller;

import com.mental.dto.*;
import com.mental.security.UserPrincipal;
import com.mental.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {

        ProfileResponse response = userProfileService.getProfile(userPrincipal.getEmail());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateProfileRequest request) {

        // Update လုပ်တဲ့နေရာမှာလည်း userPrincipal.getUsername() ကိုပဲ သုံးပါမယ်
        ProfileResponse response = userProfileService.updateProfile(userPrincipal.getEmail(), request);
        return ResponseEntity.ok(response);
    }
    @PostMapping(value = "/me/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageResponse uploadProfileImage(
            @RequestParam MultipartFile image) {

        return userProfileService.uploadProfileImage(image);
    }

    @DeleteMapping("/me/profile-image")
    public MessageResponse removeProfileImage() {

        return userProfileService.removeProfileImage();
    }

    @PutMapping("/me/avatar")
    public MessageResponse changeAvatar(
            @RequestBody SelectAvatarRequest request) {

        return userProfileService.changeAvatar(request);
    }

}