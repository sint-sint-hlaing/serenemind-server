package com.mental.controller;


import com.mental.dto.ProfileResponse;
import com.mental.dto.UpdateProfileRequest;
import com.mental.dto.user.PersonalInformationResponse;
import com.mental.dto.user.UserActivityResponse;

import com.mental.dto.*;
import com.mental.security.UserPrincipal;
import com.mental.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @ModelAttribute UpdateProfileRequest request) {

        ProfileResponse response = userProfileService.updateProfile(userPrincipal.getEmail(), request);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/activity")
    public ResponseEntity<UserActivityResponse> getUserActivity(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        UserActivityResponse activity = userProfileService.getUserActivity(userPrincipal);
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/personal-info")
    public ResponseEntity<PersonalInformationResponse> getPersonalInformation(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        PersonalInformationResponse response = userProfileService.getPersonalInformation(userPrincipal.getEmail());
        return ResponseEntity.ok(response);
    }




}