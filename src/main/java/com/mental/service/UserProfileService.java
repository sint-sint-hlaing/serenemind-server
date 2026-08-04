package com.mental.service;

import com.mental.dto.user.PersonalInformationResponse;
import com.mental.dto.ProfileResponse;
import com.mental.dto.UpdateProfileRequest;
import com.mental.dto.user.UserActivityResponse;
import com.mental.model.entity.User;
import com.mental.model.entity.UserProfile;
import com.mental.model.entity.enums.GoalStatus;
import com.mental.repository.*;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final JournalRepository journalRepository;
    private final UserGoalRepository userGoalRepository;
    private final PostRepository postRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional(readOnly = true)
    public UserActivityResponse getUserActivity(UserPrincipal userPrincipal) {
        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        long journalCount = journalRepository.countByUserId(user.getId());
        long completedGoalsCount = userGoalRepository.countByUserIdAndStatus(user.getId(), GoalStatus.COMPLETED);
        long postCount = postRepository.countByUserId(user.getId());

        return UserActivityResponse.builder()
                .totalJournals(journalCount)
                .goalsCompleted(completedGoalsCount)
                .totalPosts(postCount)
                .build();
    }

    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserProfile profile = user.getProfile();

        ProfileResponse response = new ProfileResponse();
        response.setEmail(user.getEmail());
        response.setFullname(profile.getFullname());
        response.setAvatar(profile != null ? profile.getAvatar() : null);
        response.setBio(profile != null ? profile.getBio() : "Be kind to your mind.");
        response.setUsername(user.getUsername());
        response.setBirthday(profile.getBirthday());

        return response;
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 1. Update Username if provided and changed
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
            userRepository.save(user);
        }

        UserProfile profile = user.getProfile();

        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setProfile(profile);
        }

        profile.setFullname(request.getFullname());
        profile.setBirthday(request.getBirthday());
        profile.setBio(request.getBio());


        // 2. Handle Cloudinary Image Upload
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            // Delete old avatar if present
            if (profile.getAvatar() != null && !profile.getAvatar().isBlank()) {
                cloudinaryService.deleteImage(profile.getAvatar());
            }

            // Upload new image
            String newImageUrl = cloudinaryService.uploadImage(request.getAvatar());
            profile.setAvatar(newImageUrl);
        }

        userProfileRepository.save(profile);

        return getProfile(email);
    }

    private int calculateCompletion(UserProfile profile) {
        int percentage = 30; // Username နဲ့ Email ရှိရုံနဲ့ အနည်းဆုံး 30% ပေးထားမယ်
        if (profile.getFullname() != null && !profile.getFullname().isBlank()) percentage += 30;
        if (profile.getAvatar() != null && !profile.getAvatar().isBlank()) percentage += 20;
        if (profile.getBirthday() != null) percentage += 20;
        return percentage;
    }

    @Transactional(readOnly = true)
    public PersonalInformationResponse getPersonalInformation(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        UserProfile profile = user.getProfile();

        return PersonalInformationResponse.builder()
                .fullname(profile != null ? profile.getFullname() : null)
                .email(user.getEmail())
                .username(user.getUsername())
                .birthday(profile != null ? profile.getBirthday() : null)
                .accountStatus("Active") // Or pull dynamically from user entity if available: user.getStatus().name()
                .role(user.getRole() != null ? user.getRole().name() : "User")
                .memberSince(user.getCreatedAt())
                .build();
    }


}
