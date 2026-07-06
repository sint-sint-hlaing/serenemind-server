package com.mental.service;

import com.mental.model.entity.RefreshToken;
import com.mental.model.entity.User;
import com.mental.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {


    private final RefreshTokenRepository repository;


    private final SecureRandom random = new SecureRandom();


    private final long refreshExpire =
            30L * 24 * 60 * 60;


    public String createToken(User user) {


        String rawToken =
                generateRandomToken();

        RefreshToken refreshToken =
                new RefreshToken();

        refreshToken.setUser(user);

        refreshToken.setTokenHash(
                hash(rawToken)
        );


        refreshToken.setExpiresAt(
                Instant.now()
                        .plusSeconds(refreshExpire)
        );

        refreshToken.setRevoked(false);

        repository.save(refreshToken);


        return rawToken;
    }


    public RefreshToken validate(
            String token
    ) {


        RefreshToken refreshToken =
                repository
                        .findByTokenHash(
                                hash(token)
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Refresh token not found")
                        );


        if (refreshToken.isRevoked()) {

            throw new RuntimeException(
                    "Refresh token revoked"
            );
        }


        if (refreshToken.getExpiresAt()
                .isBefore(Instant.now())) {


            throw new RuntimeException(
                    "Refresh token expired"
            );
        }


        return refreshToken;

    }


    public void revoke(
            String token
    ) {

        RefreshToken refreshToken =
                repository
                        .findByTokenHash(
                                hash(token)
                        )
                        .orElse(null);


        if (refreshToken != null) {

            refreshToken.setRevoked(true);

            repository.save(refreshToken);
        }

    }


    public void revokeAll(
            Long userId
    ) {

        repository
                .deleteAllByUserId(userId);

    }


    private String generateRandomToken() {


        byte[] bytes =
                new byte[64];


        random.nextBytes(bytes);


        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

    }


    private String hash(
            String value
    ) {

        try {

            MessageDigest digest =
                    MessageDigest
                            .getInstance("SHA-256");


            byte[] hash =
                    digest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8));


            return Base64
                    .getEncoder()
                    .encodeToString(hash);


        } catch (Exception e) {

            throw new RuntimeException(e);
        }

    }

}
