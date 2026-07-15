package com.mental.controller;

import com.mental.dto.FavoriteRequest;
import com.mental.dto.meditation.MeditationResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.FavoriteService;
import com.mental.service.MeditationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserMeditationController {


    private final MeditationService meditationService;
    private final FavoriteService favoriteService;


    @GetMapping("/me/recommendations")
    public ResponseEntity<List<MeditationResponse>> recommendations(
            Authentication authentication
    ){

        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                meditationService.getRecommendations(userId)
        );
    }


    @GetMapping("/search")
    public List<MeditationResponse> search(
            @RequestParam String keyword
    ){

        return meditationService.search(keyword);

    }

    @PostMapping("/favorites")
    public ResponseEntity<?> addFavorite(
            @RequestBody FavoriteRequest request,
            Authentication auth
    ){

        favoriteService.addFavorite(
                getUserId(auth),
                request.getMeditationId()
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/continue-listening")
    public List<MeditationResponse> continueListening(
            Authentication auth
    ){

        return meditationService
                .getContinueListening(getUserId(auth));
    }


    private Long getUserId(Authentication authentication){

        UserPrincipal principal =
                (UserPrincipal) authentication.getPrincipal();

        return principal.getId();
    }
    
}

