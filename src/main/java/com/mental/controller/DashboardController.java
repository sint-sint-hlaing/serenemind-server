package com.mental.controller;

import com.mental.security.UserPrincipal;
import com.mental.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<?> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal
    ){

        String email = principal.getEmail();

        return ResponseEntity.ok(
                dashboardService.getDashboardData(email)
        );
    }
}
