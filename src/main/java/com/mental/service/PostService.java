package com.mental.service;

import com.mental.dto.Post.PostRequest;
import com.mental.dto.Post.PostResponse;
import com.mental.model.entity.Post;
import com.mental.model.entity.User;
import com.mental.model.entity.PostLike;
import com.mental.model.entity.Notification; // 👈 Notification Entity ကို import လုပ်ပါ
import com.mental.repository.PostLikeRepository;
import com.mental.repository.PostRepository;
import com.mental.repository.UserRepository;
import com.mental.repository.NotificationRepository; // 👈 NotificationRepository ကို import လုပ်ပါ
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
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
    private final NotificationRepository notificationRepository; // 👈 NotificationRepository ကို Inject လုပ်ပေးထားသည်

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

    // UI - Post Detail (ပို့စ်တစ်ခုတည်းကို ID ဖြင့်ကြည့်ခြင်း)
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id, UserPrincipal userPrincipal) {
        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        return convertToPostResponse(post, currentUser);
    }

    // UI - Floating Action Button (+) ဖြင့် ပို့စ်အသစ်တင်ခြင်း (Cloudinary Image ပါဝင်သည်)
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

    // UI - Post တစ်ခုကို Like ပေးခြင်း / ပြန်ဖြုတ်ခြင်း (Toggle)
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

                            // 👈 Like ပြန်ဖြုတ်လိုက်လျှင် ဆောက်ခဲ့သော Noti ကို ပြန်ဖျက်ပေးသည့်အပိုင်း
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

                            // 👈 Like ပေးလိုက်လျှင် ပို့စ်ပိုင်ရှင်ထံ Notification သွားသိမ်းပေးသည့်အပိုင်း
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

    // Helper Method: Entity မှ Response DTO သို့ ပြောင်းလဲခြင်း
    private PostResponse convertToPostResponse(Post post, User currentUser) {
        boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());

        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setContent(post.getContent());
        response.setImageUrl(post.getImageUrl());
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setLikedByMe(isLiked);
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