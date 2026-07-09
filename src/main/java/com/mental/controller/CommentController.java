package com.mental.controller;

import com.mental.dto.Comment.CommentRequest;
import com.mental.dto.Comment.CommentResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // UI - ပို့စ်တစ်ခုချင်းစီအောက်က ကွန်မန့်များအားလုံးကို ဆွဲထုတ်ခြင်း
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getCommentsByPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<CommentResponse> comments = commentService.getCommentsByPostId(postId, userPrincipal);
        return ResponseEntity.ok(comments);
    }

    // UI - "Add a comment..." နေရာတွင် ကွန်မန့်အသစ် ရိုက်ထည့်ခြင်း
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CommentRequest request) {

        CommentResponse response = commentService.createComment(postId, userPrincipal, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
