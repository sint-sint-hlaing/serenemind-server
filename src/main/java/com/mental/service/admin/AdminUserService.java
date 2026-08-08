package com.mental.service.admin;

import com.mental.dto.UserDto;

import java.util.List;
public interface AdminUserService {

    List<UserDto> getUsers();

    UserDto blockUser(Long id);

    UserDto activateUser(Long id);
}
