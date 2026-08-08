package com.mental.service;

import com.mental.dto.Post.PostRequest;
import com.mental.dto.Post.PostResponse;
import com.mental.model.entity.*;
import com.mental.repository.*;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mental.exception.ResourceNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final StreakService streakService;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final PostSaveRepository postSaveRepository;
    // UI - Community Feed (Recent သို့မဟုတ် Popular အလိုက် ဆွဲထုတ်ခြင်း)
    @Transactional(readOnly = true)
    public List<PostResponse> getAllPosts(UserPrincipal userPrincipal, String filter) {
        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Post> posts = filter.equalsIgnoreCase("popular")
                ? postRepository.findAllByOrderByLikeCountDesc()
                : postRepository.findAllByOrderByCreatedAtDesc();

        return posts.stream()
                .map(post -> convertToPostResponse(post, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id, UserPrincipal userPrincipal) {
        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        return convertToPostResponse(post, currentUser);
    }

    @Transactional
    public PostResponse createPost(UserPrincipal userPrincipal, PostRequest request, MultipartFile imageFile) {
        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String imageUrl = cloudinaryService.uploadImage(imageFile);

        Post post = new Post();
        post.setContent(request.getContent());
        post.setImageUrl(imageUrl);
        post.setUser(user);
        post.setAnonymous(request.isAnonymous());
        post.setLikeCount(0);
        post.setCommentCount(0);

        Post savedPost = postRepository.save(post);
        streakService.updateStreak(userPrincipal.getEmail());
        return convertToPostResponse(savedPost, user);
    }

    @Transactional
    public void deletePost(Long id, UserPrincipal userPrincipal) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this post");
        }

        postLikeRepository.deleteByPostId(id);
        postSaveRepository.deleteByPostId(id);

        postRepository.delete(post);
    }


    @Transactional
    public void toggleLikePost(Long id, UserPrincipal userPrincipal) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        postLikeRepository.findByPostIdAndUserId(id, user.getId())
                .ifPresentOrElse(
                        like -> {
                            postLikeRepository.delete(like);
                            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));

                            if (!post.getUser().getId().equals(user.getId())) {
                                String targetMessage = user.getUsername() + " liked your post: \"" + post.getContent() + "\"";
                                notificationRepository.deleteByUserAndTitleAndMessage(post.getUser(), "New like on your post", targetMessage);
                            }
                        },
                        () -> {
                            PostLike newLike = new PostLike();
                            newLike.setPost(post);
                            newLike.setUser(user);
                            postLikeRepository.save(newLike);
                            post.setLikeCount(post.getLikeCount() + 1);

                            if (!post.getUser().getId().equals(user.getId())) {
                                notificationService.createNotification(
                                        post.getUser(),
                                        "New like on your post",
                                        user.getUsername() + " liked your post: \"" + post.getContent() + "\"",
                                        "LIKE",
                                        post.getId(),
                                        "POST"
                                );
                            }
                        }
                );
        postRepository.save(post);
    }

    @Transactional
    public void toggleSavePost(Long id, UserPrincipal userPrincipal) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        postSaveRepository.findByPostIdAndUserId(id, user.getId())
                .ifPresentOrElse(
                        postSaveRepository::delete,
                        () -> {
                            PostSave newSave = PostSave.builder()
                                    .post(post)
                                    .user(user)
                                    .build();
                            postSaveRepository.save(newSave);
                        }
                );
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getSavedPosts(UserPrincipal userPrincipal) {
        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<PostSave> savedPosts = postSaveRepository.findAllByUserIdOrderByPostCreatedAtDesc(user.getId());

        return savedPosts.stream()
                .map(save -> convertToPostResponse(save.getPost(), user))
                .collect(Collectors.toList());
    }

    private PostResponse convertToPostResponse(Post post, User currentUser) {
        boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());
        boolean isSaved = postSaveRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId()); // 👈 စစ်ဆေးရန်

        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setContent(post.getContent());
        response.setImageUrl(post.getImageUrl());
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setLikedByMe(isLiked);
        response.setSavedByMe(isSaved);
        response.setCreatedAt(post.getCreatedAt());
        response.setAnonymous(post.isAnonymous());

        if (post.isAnonymous()) {
            if (post.getUser().getId().equals(currentUser.getId())) {
                response.setUsername("Anonymous (You)");
            } else {
                response.setUsername("Anonymous");
            }
            response.setUserProfilePicture(null);
        } else {
            String profilePic = (post.getUser().getUserProfile() != null)
                    ? post.getUser().getUserProfile().getAvatar()
                    : null;
            response.setUsername(post.getUser().getUsername());
            response.setUserProfilePicture(profilePic);
        }

        return response;
    }
}