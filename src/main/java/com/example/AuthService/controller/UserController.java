package com.example.AuthService.controller;

import com.example.AuthService.dto.response.UserProfileResponse;
import com.example.AuthService.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public UserProfileResponse getUserProfile(
            @RequestParam String email
    ) {
        return userService.getUserProfileByEmail(email);
    }
}
