package com.mental.repository;

import com.mental.model.entity.User;
import com.mental.model.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile,Long>{
    Optional<UserProfile> findByUser(User user);

}
