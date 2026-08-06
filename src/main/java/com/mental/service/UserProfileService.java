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

import com.mental.dto.*;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.UserMapper;
import com.mental.model.entity.Avatar;
import com.mental.model.entity.User;
import com.mental.model.entity.UserProfile;
import com.mental.repository.AvatarRepository;
import com.mental.repository.UserProfileRepository;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final UserMapper userMapper;

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


    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));


        // 1. Update Username if provided and changed
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
            userRepository.save(user);
        }


        UserProfile profile = getUserProfile(email);


        if (request.getFullname() != null &&
                !request.getFullname().isBlank()) {

            profile.setFullname(
                    request.getFullname()
            );
        }


        if (request.getBirthday() != null) {

            profile.setBirthday(
                    request.getBirthday()
            );
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



    private UserProfile getUserProfile(
            String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );


        return userProfileRepository.findByUser(user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Profile not found"
                        )
                );
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


    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // သင့် database ဆက်စပ်ပုံအပေါ်မူတည်ပြီး profile ကို ဆွဲထုတ်ပါ
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElse(new UserProfile()); // မရှိသေးလျှင် အလွတ် object တစ်ခုပေးရန်

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

