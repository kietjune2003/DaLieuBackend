package com.example.AuthService.service;

import com.example.AuthService.dto.response.UserProfileResponse;
import com.example.AuthService.dto.response.UserResponseDTO;
import com.example.AuthService.dto.request.UserUpdateRequestDTO;

import java.util.List;

public interface UserService {
    UserProfileResponse getUserProfileByEmail(String email);


    List<UserResponseDTO> getAllUsers(
            int page,
            int size,
            String keyword,
            Long roleId,
            Boolean enabled
    );

    UserResponseDTO getUserById(Long id);
    UserResponseDTO updateUser(Long id, UserUpdateRequestDTO request);
    void deleteUser(Long id);
    UserResponseDTO createUser(UserUpdateRequestDTO request);

    UserProfileResponse updateMyProfile(String email, UserUpdateRequestDTO request);
}
