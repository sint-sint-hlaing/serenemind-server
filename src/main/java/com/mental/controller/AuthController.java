package com.mental.controller;

import com.mental.dto.*;
import com.mental.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @GetMapping("/data")
    public String test() {
        return "Data";

    }

    @PostMapping("/register")
    public AuthResponse register(
            @RequestBody RegisterRequest req) {

        return authService.register(req);

    }


    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody LoginRequest req) {

        return authService.login(req);

    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @RequestBody RefreshRequest request
    ) {


        return authService.refresh(request);

    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody LogoutRequest request
    ) {


        authService.logout(request);


        return ResponseEntity.ok(
                "Logout success"
        );

    }


}