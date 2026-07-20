package com.mental.service.admin.impl;

import com.mental.dto.UserDto;
import com.mental.model.entity.User;
import com.mental.repository.UserRepository;
import com.mental.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {


    private final UserRepository userRepository;


    @Override
    public List<UserDto> getUsers() {

        return userRepository.findAll()
                .stream()
                .map(user -> UserDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .isActive(user.isActive())
                        .build())
                .toList();

    }


    @Override
    public UserDto blockUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );


        user.setActive(false);

        userRepository.save(user);


        return mapToDto(user);
    }



    @Override
    public UserDto activateUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );


        user.setActive(true);

        userRepository.save(user);


        return mapToDto(user);
    }



    private UserDto mapToDto(User user){

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .isActive(user.isActive())
                .build();

    }
}