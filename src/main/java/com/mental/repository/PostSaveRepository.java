package com.mental.repository;

import com.mental.model.entity.PostSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostSaveRepository extends JpaRepository<PostSave, Long> {
    void deleteByPostId(Long postId);
    Optional<PostSave> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    List<PostSave> findAllByUserIdOrderByPostCreatedAtDesc(Long userId); // User သိမ်းထားသော ပို့စ်များ ဆွဲထုတ်ရန်
}