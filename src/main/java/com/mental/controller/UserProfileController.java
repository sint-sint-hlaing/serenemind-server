package com.mental.controller;

import com.mental.dto.ProfileResponse;
import com.mental.dto.UpdateProfileRequest;
import com.mental.security.UserPrincipal;
import com.mental.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}