package com.mental.controller;

import com.mental.dto.MeditationSessionRequest;
import com.mental.security.UserPrincipal;
import com.mental.service.MeditationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeditationController {


    private final MeditationService service;


    @GetMapping("/meditations")
    public ResponseEntity<?> list() {

        return ResponseEntity.ok(
                service.getAll()
        );

    }


    @PostMapping("/meditation-sessions")
    public ResponseEntity<?> complete(

            @RequestBody
            @Valid
            MeditationSessionRequest request,

            @AuthenticationPrincipal
            UserPrincipal principal

    ) {

        service.completeSession(
                request,
                principal.getEmail()
        );


        return ResponseEntity.ok(
                "Meditation completed"
        );

    }

    @GetMapping("/meditation-sessions/history")
    public ResponseEntity<?> history(

            @AuthenticationPrincipal
            UserPrincipal principal

    ) {
        return ResponseEntity.ok(
                service.history(
                        principal.getEmail()
                ));
    }
}
