package com.mental.repository;

import com.mental.model.entity.RefreshToken;
import com.mental.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken,Long> {

    Optional<RefreshToken>
    findByTokenHash(String tokenHash);

    void deleteAllByUserId(Long userId);

}
