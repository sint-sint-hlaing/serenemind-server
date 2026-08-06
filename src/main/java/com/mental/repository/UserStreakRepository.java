package com.mental.repository;

import com.mental.model.entity.User;
import com.mental.model.entity.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {
    //UserStreak findByUser(User user);
    Optional<UserStreak> findByUser(User user);
    boolean existsByUser(User user);

}
