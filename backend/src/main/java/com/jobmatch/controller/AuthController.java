package com.jobmatch.controller;

import com.jobmatch.dto.AuthDtos.AuthResponse;
import com.jobmatch.dto.AuthDtos.LoginRequest;
import com.jobmatch.dto.AuthDtos.RegisterRequest;
import com.jobmatch.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
