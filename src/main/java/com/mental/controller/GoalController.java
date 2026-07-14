package com.mental.controller;

import com.mental.dto.GoalRequest;
import com.mental.dto.goal.UserGoal;
import com.mental.security.UserPrincipal;
import com.mental.service.UserGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {


    private final UserGoalService goalService;



    @PostMapping
    public ResponseEntity<UserGoal> createGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody GoalRequest request
    ){

        return ResponseEntity.ok(
                goalService.createGoal(
                        principal.getEmail(),
                        request
                )
        );
    }




    @GetMapping
    public ResponseEntity<List<UserGoal>> getAllGoals(
            @AuthenticationPrincipal UserPrincipal principal
    ){

        return ResponseEntity.ok(
                goalService.getUserGoals(
                        principal.getEmail()
                )
        );
    }




    @PutMapping("/{id}")
    public ResponseEntity<UserGoal> updateProgress(
            @PathVariable Long id
    ){

        return ResponseEntity.ok(
                goalService.updateProgress(id)
        );
    }




    @PostMapping("/{id}/complete")
    public ResponseEntity<UserGoal> completeGoal(
            @PathVariable Long id
    ){

        return ResponseEntity.ok(
                goalService.completeGoal(id)
        );
    }




    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable Long id
    ){

        goalService.deleteGoal(id);

        return ResponseEntity.noContent().build();
    }

}