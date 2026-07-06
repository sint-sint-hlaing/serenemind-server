package com.mental.service;

import com.mental.dto.ProfileResponse;
import com.mental.dto.UpdateProfileRequest;
import com.mental.model.entity.User;
import com.mental.model.entity.UserProfile;
import com.mental.repository.UserProfileRepository;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserProfile profile = user.getProfile();

        ProfileResponse response = new ProfileResponse();
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullname(profile.getFullname());
        response.setAvatar(profile.getAvatar());
        response.setBirthday(profile.getBirthday());

        // UI အလှဆင်ဖို့အတွက် Completion Percentage တွက်ချက်ခြင်း
        response.setProfileCompletionPercentage(calculateCompletion(profile));

        return response;
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));


        UserProfile profile = user.getProfile();

        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setProfile(profile);
        }

        profile.setFullname(request.getFullname());
        profile.setAvatar(request.getAvatar());
        profile.setBirthday(request.getBirthday());

        userProfileRepository.save(profile);

        return getProfile(email); // နောက်ဆုံး Update ဖြစ်သွားတဲ့ Data ကို Response အနေနဲ့ ပြန်ပေးခြင်း
    }

    private int calculateCompletion(UserProfile profile) {
        int percentage = 30; // Username နဲ့ Email ရှိရုံနဲ့ အနည်းဆုံး 30% ပေးထားမယ်
        if (profile.getFullname() != null && !profile.getFullname().isBlank()) percentage += 30;
        if (profile.getAvatar() != null && !profile.getAvatar().isBlank()) percentage += 20;
        if (profile.getBirthday() != null) percentage += 20;
        return percentage;
    }
}
