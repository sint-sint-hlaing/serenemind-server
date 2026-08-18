package com.mental.service;

import com.mental.dto.ProfileResponse;
import com.mental.dto.UpdateProfileRequest;
import com.mental.dto.UserDto;
import com.mental.dto.user.PersonalInformationResponse;
import com.mental.dto.user.UserActivityResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.UserMapper;
import com.mental.model.entity.User;
import com.mental.model.entity.UserProfile;
import com.mental.model.entity.enums.GoalStatus;
import com.mental.repository.*;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final JournalRepository journalRepository;
    private final UserGoalRepository userGoalRepository;
    private final PostRepository postRepository;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final MeditationSessionRepository meditationSessionRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserActivityResponse getUserActivity(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new UsernameNotFoundException("Authenticated user not found");
        }

        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userPrincipal.getEmail()));

        long journalCount = journalRepository.countByUserId(user.getId());
        long completedGoalsCount = userGoalRepository.countByUserIdAndStatus(user.getId(), GoalStatus.COMPLETED);
        long postCount = postRepository.countByUserId(user.getId());
        long completedMeditations = meditationSessionRepository.countByUserAndCompletedTrue(user);
        long favorites = favoriteRepository.countByUserId(user.getId());

        return UserActivityResponse.builder()
                .totalJournals(journalCount)
                .goalsCompleted(completedGoalsCount)
                .totalPosts(postCount)
                .completedMeditationsCount(completedMeditations)
                .favoriteMeditationsCount(favorites)
                .build();
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }
        if (request == null) {
            throw new IllegalArgumentException("Update profile request must not be null");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
            userRepository.save(user);
        }

        UserProfile profile = userProfileRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUser(user); // User Object နှင့် ချိတ်ဆက်ပေးပါ
                    return userProfileRepository.save(newProfile);
                });

        if (request.getFullname() != null && !request.getFullname().isBlank()) {
            profile.setFullname(request.getFullname());
        }

        if (request.getBirthday() != null) {
            profile.setBirthday(request.getBirthday());
        }

        profile.setBio(request.getBio());

        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            if (profile.getAvatar() != null && !profile.getAvatar().isBlank()) {
                try {
                    cloudinaryService.deleteImage(profile.getAvatar());
                } catch (Exception ignored) {
                    // Fail gracefully and continue with avatar upload
                }
            }

            String newImageUrl = cloudinaryService.uploadImage(request.getAvatar());
            if (newImageUrl == null || newImageUrl.isBlank()) {
                throw new IllegalStateException("Failed to upload avatar image. Please try again.");
            }
            profile.setAvatar(newImageUrl);
        }

        userProfileRepository.save(profile);

        return getProfile(email);
    }

    private UserProfile getUserProfile(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return userProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + email));
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUserRegistration() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toAdminDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonalInformationResponse getPersonalInformation(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        UserProfile profile = user.getProfile();

        return PersonalInformationResponse.builder()
                .fullname(profile != null ? profile.getFullname() : null)
                .email(user.getEmail())
                .username(user.getUsername())
                .birthday(profile != null ? profile.getBirthday() : null)
                .accountStatus("Active")
                .role(user.getRole() != null ? user.getRole().name() : "User")
                .memberSince(user.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElse(new UserProfile());

        return ProfileResponse.builder()
                .fullname(profile.getFullname())
                .email(user.getEmail())
                .avatar(profile.getAvatar())
                .bio(profile.getBio())
                .username(user.getUsername())
                .birthday(profile.getBirthday())
                .build();
    }
}