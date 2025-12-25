package com.example.AuthService.controller;

import com.example.AuthService.dto.response.UserProfileResponse;
import com.example.AuthService.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public UserProfileResponse getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        return userService.getUserProfileByEmail(email);
    }

}
