package com.mental.service;

import com.mental.dto.*;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.ProfileMapper;
import com.mental.mapper.UserMapper;
import com.mental.model.entity.Avatar;
import com.mental.model.entity.User;
import com.mental.model.entity.UserProfile;
import com.mental.repository.AvatarRepository;
import com.mental.repository.UserProfileRepository;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {


    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final AvatarRepository avatarRepository;
    private final ProfileMapper profileMapper;
    private final ImageService imageService;
    private final UserMapper userMapper;



    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {

        UserProfile profile = getUserProfile(email);

        return profileMapper.toResponse(profile);
    }



    public ProfileResponse updateProfile(
            String email,
            UpdateProfileRequest request
    ) {

        UserProfile profile = getUserProfile(email);


        if(request.getFullname() != null &&
                !request.getFullname().isBlank()) {

            profile.setFullname(
                    request.getFullname()
            );
        }


        if(request.getBirthday() != null) {

            profile.setBirthday(
                    request.getBirthday()
            );
        }


        userProfileRepository.save(profile);


        return profileMapper.toResponse(profile);
    }




    public ImageResponse uploadProfileImage(
            MultipartFile image
    ) {

        UserProfile profile = getCurrentProfile();


        String imageUrl = imageService.upload(image);


        profile.setProfileImageUrl(imageUrl);


        return new ImageResponse(imageUrl);
    }





    public MessageResponse removeProfileImage() {

        UserProfile profile = getCurrentProfile();


        profile.setProfileImageUrl(null);


        return MessageResponse.success(
                "Profile image removed successfully"
        );
    }




    public MessageResponse changeAvatar(
            SelectAvatarRequest request
    ) {

        UserProfile profile = getCurrentProfile();



        Avatar avatar = avatarRepository.findById(
                        request.avatarId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Avatar not found"
                        )
                );



        if(Boolean.FALSE.equals(avatar.getIsActive())) {

            throw new IllegalStateException(
                    "Avatar is not available"
            );
        }



        profile.setAvatar(avatar);



        return MessageResponse.success(
                "Avatar changed successfully"
        );
    }




    private UserProfile getCurrentProfile() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();


        return getUserProfile(email);
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
    }
