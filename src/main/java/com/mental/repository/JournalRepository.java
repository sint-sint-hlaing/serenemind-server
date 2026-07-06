package com.mental.repository;

import com.mental.model.entity.Journal;
import com.mental.model.entity.RefreshToken;
import com.mental.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface JournalRepository  extends JpaRepository<Journal,Long> {
    List<Journal> findByUser(User user);
    Page<Journal> findAllByFlaggedTrueOrderByCreatedAtDesc(Pageable pageable);
}
