package com.mental.service;

import com.mental.dto.*;
import com.mental.exception.ResourceNotFoundException;
import com.mental.model.entity.RefreshToken;
import com.mental.model.entity.User;
import com.mental.model.entity.UserProfile;
import com.mental.model.entity.enums.Role;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final FcmTokenService fcmTokenService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest req) {
        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRole(Role.USER);
        user.setActive(true);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        user.setProfile(profile);

        users.save(user);

        String access = jwtService.generate(user);
        String refresh = refreshTokenService.createToken(user);

        return new AuthResponse(access, refresh);
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + req.email()));

        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid password");
        }

        if (req.fcmToken() != null && !req.fcmToken().isBlank()) {
            fcmTokenService.saveTokenForUser(user, req.fcmToken());
        }

        String access = jwtService.generate(user);
        String refresh = refreshTokenService.createToken(user);

        return new AuthResponse(access, refresh);
    }

    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshTokenService.validate(request.refreshToken());
        User user = token.getUser();

        refreshTokenService.revoke(request.refreshToken());

        String newRefresh = refreshTokenService.createToken(user);
        String access = jwtService.generate(user);

        return new AuthResponse(access, newRefresh);
    }

    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    public AuthResponse registerAdmin(RegisterRequest req) {
        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRole(Role.ADMIN);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        user.setProfile(profile);

        users.save(user);

        String access = jwtService.generate(user);
        String refresh = refreshTokenService.createToken(user);

        return new AuthResponse(access, refresh);
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest req) {
        User user = users.findByEmail(req.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + req.email()));

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));
        users.save(user);

        return token;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        User user = users.findByResetPasswordToken(req.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));

        if (user.getResetPasswordTokenExpiry() == null ||
                user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token has expired");
        }

        user.setPasswordHash(encoder.encode(req.newPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        users.save(user);
    }
}