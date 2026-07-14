package com.mental.repository;

import com.mental.model.entity.User;
import com.mental.model.entity.UserGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserGoalRepository extends JpaRepository<UserGoal, Long> {

    List<UserGoal> findByUserUsername(String username);

    List<UserGoal> findByUser(User user);
}
