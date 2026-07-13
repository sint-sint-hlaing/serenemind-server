package com.mental.service;

import com.mental.dto.Comment.CommentRequest;
import com.mental.dto.Comment.CommentResponse;
import com.mental.model.entity.Comment;
import com.mental.model.entity.Post;
import com.mental.model.entity.User;
import com.mental.repository.CommentRepository;
import com.mental.repository.PostRepository;
import com.mental.repository.UserRepository;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import com.mental.exception.ResourceNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final StreakService streakService;
    private final NotificationService notificationService;

    // UI - သက်ဆိုင်ရာ Post ID အလိုက် ကွန်မန့်များအားလုံး ဆွဲထုတ်ခြင်း
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPostId(Long postId, UserPrincipal userPrincipal) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found with id: " + postId);
        }

        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);

        // ပို့စ်တစ်ခုတည်းအောက်က Anonymous User တစ်ယောက်စီကို နံပါတ်စဉ် သတ်မှတ်ရန် Map ဆောက်ခြင်း
        Map<Long, Integer> anonymousUserMap = new HashMap<>();
        int anonymousCounter = 1;

        for (Comment comment : comments) {
            if (comment.isAnonymous()) {
                Long userId = comment.getUser().getId();
                if (!anonymousUserMap.containsKey(userId)) {
                    anonymousUserMap.put(userId, anonymousCounter++);
                }
            }
        }

        return comments.stream()
                .map(comment -> convertToCommentResponse(comment, anonymousUserMap, currentUser))
                .collect(Collectors.toList());
    }

    // UI - ကွန်မန့်အသစ် ရေးသားခြင်း
    @Transactional
    public CommentResponse createComment(Long postId, UserPrincipal userPrincipal, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setPost(post);
        comment.setUser(user);
        comment.setAnonymous(request.isAnonymous());

        Comment savedComment = commentRepository.save(comment);

        // ပို့စ်၏ Comment ရေတွက်မှုကို တိုးပေးခြင်း
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        // 👈 ကွန်မန့်အသစ်တင်လိုက်လျှင် ပို့စ်ပိုင်ရှင်ထံ Notification သွားသိမ်းပေးမည့်အပိုင်း
        if (!post.getUser().getId().equals(user.getId())) { // မိမိပို့စ်ကို မိမိပြန်မန့်လျှင် Noti မတက်စေရန်

            // ကွန်မန့်ရှင်က Anonymous ဖြစ်နေရင် Noti မှာ "Anonymous" လို့ပြပြီး ပုံမှန်ဆိုရင် ၎င်း၏ Username ပြရန်
            String commenterName = comment.isAnonymous() ? "Anonymous" : user.getUsername();

            notificationService.createNotification(
                    post.getUser(), // Noti လက်ခံမည့် ပို့စ်ပိုင်ရှင်
                    "New comment on your post", // Title
                    commenterName + " commented: \"" + comment.getContent() + "\"", // Message
                    "COMMENT", // Type (Icon ပြောင်းရန်)
                    post.getId(), // Target ID (Post Detail သို့ သွားရန်)
                    "COMMENT" // Target Type
            );
        }

        streakService.updateStreak(userPrincipal.getEmail());

        // အသစ်မန့်လိုက်တဲ့အချိန်မှာ သီးခြားခေါ်တာမို့ Map အလွတ်တစ်ခု ပေးလိုက်ပါတယ်
        return convertToCommentResponse(savedComment, new HashMap<>(), user);
    }

    // Helper Method: ပြင်ဆင်ထားသော စနစ်သစ်
    // CommentService.java ရဲ့ convertToCommentResponse method ကို အခုလို ပြင်ပါ

    private CommentResponse convertToCommentResponse(Comment comment, Map<Long, Integer> anonymousUserMap, User currentUser) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCreatedAt());
        response.setAnonymous(comment.isAnonymous());

        if (comment.isAnonymous()) {
            // Map ထဲကနေ သူနဲ့ဆိုင်တဲ့ နံပါတ်စဉ်ကို ယူရပါမယ်
            Integer anonymousNumber = anonymousUserMap.getOrDefault(comment.getUser().getId(), 1);

            String displayName = "Anonymous " + anonymousNumber;

            // 👈 ၁။ ကွန်မန့်ရှင်က ပို့စ်ပိုင်ရှင် ဖြစ်နေသလား စစ်ဆေးခြင်း (Author တပ်ရန်)
            if (comment.getUser().getId().equals(comment.getPost().getUser().getId())) {
                displayName += " (Author)";
            }

            // 👈 ၂။ လက်ရှိ ကြည့်နေတဲ့လူက ဒီကွန်မန့်ရှင်ကိုယ်တိုင် ဖြစ်နေရင် (You) ထပ်တပ်ရန်
            if (comment.getUser().getId().equals(currentUser.getId())) {
                displayName += " (You)";
            }

            response.setUsername(displayName);
            response.setUserProfilePicture(null);
        } else {
            String profilePic = (comment.getUser().getUserProfile() != null)
                    ? comment.getUser().getUserProfile().getAvatar()
                    : null;
            response.setUsername(comment.getUser().getUsername());
            response.setUserProfilePicture(profilePic);
        }

        return response;
    }
}