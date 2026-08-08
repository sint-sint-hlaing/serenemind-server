package com.mental.repository;

import com.mental.model.entity.DeviceToken;
import com.mental.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    boolean existsByToken(String token);

    List<DeviceToken> findByUser(User user);

    Optional<DeviceToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUser(User user);
}