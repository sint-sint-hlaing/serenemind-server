package com.mental.repository;

import com.mental.model.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // သက်ဆိုင်ရာ Post ID အလိုက် ကွန်မန့်များအားလုံးကို အစဉ်လိုက်ဆွဲထုတ်ရန်
    List<com.mental.model.entity.Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
}
