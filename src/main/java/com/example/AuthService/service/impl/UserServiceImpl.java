package com.example.AuthService.service.impl;

import com.example.AuthService.dto.response.UserProfileResponse;
import com.example.AuthService.dto.response.UserResponseDTO;
import com.example.AuthService.dto.request.UserUpdateRequestDTO;
import com.example.AuthService.entity.Country;
import com.example.AuthService.entity.Role;
import com.example.AuthService.entity.User;
import com.example.AuthService.repository.CountryRepository;
import com.example.AuthService.repository.RoleRepository;
import com.example.AuthService.repository.UserRepository;
import com.example.AuthService.service.UserService;
import com.example.AuthService.spec.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CountryRepository countryRepository;
    private final PasswordEncoder passwordEncoder;
    @Override
    public UserProfileResponse getUserProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserProfileResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .phoneNumber(user.getPhoneNumber())
                .photoUrl(user.getPhotoUrl())
                .facebookAccountId(user.getFacebookAccountId())
                .googleAccountId(user.getGoogleAccountId())
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .countryName(user.getCountry() != null ? user.getCountry().getName() : null)
                .build();
    }
    @Override
    public List<UserResponseDTO> getAllUsers(
            int page,
            int size,
            String keyword,
            Long roleId,
            Boolean enabled
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<User> spec = UserSpecification.filter(
                keyword,
                roleId,
                enabled
        );

        Page<User> users = userRepository.findAll(spec, pageable);

        return users.stream()
                .map(this::toDTO)
                .toList();
    }


    @Override
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDTO(user);
    }

    @Override
    public UserResponseDTO updateUser(Long id, UserUpdateRequestDTO request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            boolean exists = userRepository.existsByEmail(request.getEmail());
            if (exists) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getPhotoUrl() != null) {
            user.setPhotoUrl(request.getPhotoUrl());
        }

        // ⭐ nếu admin gửi password → đổi password
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            user.setRole(role);
        }

        if (request.getCountryId() != null) {
            Country country = countryRepository.findById(request.getCountryId())
                    .orElseThrow(() -> new RuntimeException("Country not found"));
            user.setCountry(country);
        }

        if (request.getAccountNonLocked() != null) {
            user.setAccountNonLocked(request.getAccountNonLocked());
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        userRepository.save(user);
        return toDTO(user);
    }


    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }

    @Override
    public UserResponseDTO createUser(UserUpdateRequestDTO request) {

        if (request.getEmail() == null || request.getPassword() == null) {
            throw new RuntimeException("Email and password are required");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User with email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());

        // ⭐ encode password
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setGender(request.getGender());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPhotoUrl(request.getPhotoUrl());

        user.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        user.setAccountNonLocked(request.getAccountNonLocked() != null ? request.getAccountNonLocked() : true);
        user.setAccountNonExpired(true);
        user.setCredentialsNonExpired(true);

        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            user.setRole(role);
        }

        if (request.getCountryId() != null) {
            Country country = countryRepository.findById(request.getCountryId())
                    .orElseThrow(() -> new RuntimeException("Country not found"));
            user.setCountry(country);
        }

        userRepository.save(user);
        return toDTO(user);
    }


    private UserResponseDTO toDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .phoneNumber(user.getPhoneNumber())
                .photoUrl(user.getPhotoUrl())
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .countryName(user.getCountry() != null ? user.getCountry().getName() : null)
                .accountNonExpired(user.isAccountNonExpired())
                .accountNonLocked(user.isAccountNonLocked())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .enabled(user.isEnabled())
                .build();
    }
    private UserProfileResponse toProfileDTO(User user) {
        return UserProfileResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .phoneNumber(user.getPhoneNumber())
                .photoUrl(user.getPhotoUrl())
                .facebookAccountId(user.getFacebookAccountId())
                .googleAccountId(user.getGoogleAccountId())
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .countryName(user.getCountry() != null ? user.getCountry().getName() : null)
                .build();
    }

    @Override
    public UserProfileResponse updateMyProfile(String email, UserUpdateRequestDTO request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(request.getName());
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth()); // ✅ THÊM
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPhotoUrl(request.getPhotoUrl());


        // user CHỈ đổi password CỦA MÌNH
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);
        return toProfileDTO(user);
    }

}

