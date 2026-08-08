package com.mental.repository;

import com.mental.model.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<com.mental.model.entity.Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);
}
