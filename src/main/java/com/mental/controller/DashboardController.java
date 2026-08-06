package com.mental.controller;

import com.mental.dto.home.DashboardResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;


    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal
    ){

        return ResponseEntity.ok(
                dashboardService.getDashboardData(
                        principal.getEmail()
                )
        );
    }
}