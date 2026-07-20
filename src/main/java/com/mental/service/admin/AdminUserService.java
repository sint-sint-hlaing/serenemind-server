package com.mental.service.admin;

import com.mental.dto.UserDto;

import java.util.List;
public interface AdminUserService {
    // User Management

    List<UserDto> getUsers();

    UserDto blockUser(Long id);

    UserDto activateUser(Long id);
}
