package com.mental.controller;

import com.mental.dto.Post.PostRequest;
import com.mental.dto.Post.PostResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // UI - Community Screen (Popular, Recent, Following feed များအတွက်)
    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "recent") String filter) {

        // filter parameter မူတည်ပြီး Popular သို့မဟုတ် Recent ခွဲထုတ်နိုင်ပါတယ်
        List<PostResponse> posts = postService.getAllPosts(userPrincipal, filter);
        return ResponseEntity.ok(posts);
    }

    // UI - Post Detail Screen (ပို့စ်တစ်ခုတည်းကို ID ဖြင့်ကြည့်ခြင်း)
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        PostResponse post = postService.getPostById(id, userPrincipal);
        return ResponseEntity.ok(post);
    }

    // UI - Floating Action Button (+) နှိပ်ပြီး ပို့စ်အသစ်တင်ခြင်း
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestPart("post") @Valid PostRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        PostResponse response = postService.createPost(userPrincipal, request, imageFile);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // UI - Post တစ်ခုကို Like ပေးခြင်း သို့မဟုတ် Like ပြန်ဖြုတ်ခြင်း (Toggle)
    @PostMapping("/{id}/like")
    public ResponseEntity<Void> toggleLikePost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        postService.toggleLikePost(id, userPrincipal);
        return ResponseEntity.ok().build();
    }
}
