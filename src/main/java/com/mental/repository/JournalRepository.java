package com.mental.repository;

import com.mental.model.entity.Journal;
import com.mental.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    /** Get all journals for a user ordered by newest first */
    List<Journal> findByUserOrderByCreatedAtDesc(User user);

    /** Get only favourite journals for a user */
    List<Journal> findByUserAndFavouriteTrueOrderByCreatedAtDesc(User user);

    /** Get all tagged (has tags) journals for a user */
    @Query("SELECT j FROM Journal j WHERE j.user = :user AND j.tags IS NOT NULL AND j.tags <> '' ORDER BY j.createdAt DESC")
    List<Journal> findTaggedByUser(@Param("user") User user);

    /** Full-text search across title and encrypted preview/content */
    @Query("SELECT j FROM Journal j WHERE j.user = :user AND LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY j.createdAt DESC")
    List<Journal> searchByUserAndTitle(@Param("user") User user, @Param("query") String query);

    /** Admin: get all flagged journals with pagination */
    Page<Journal> findAllByFlaggedTrueOrderByCreatedAtDesc(Pageable pageable);

    /** Keep backward-compatible finder */
    List<Journal> findByUser(User user);

    /** Count journals by user (used in dashboard streak/stats) */
    long countByUser(User user);
}
