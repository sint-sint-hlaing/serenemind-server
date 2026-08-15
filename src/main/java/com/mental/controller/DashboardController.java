package com.mental.controller;

import com.mental.dto.home.DashboardResponse;
import com.mental.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboardData(@AuthenticationPrincipal UserDetails userDetails) {
        DashboardResponse response = dashboardService.getDashboardData(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}