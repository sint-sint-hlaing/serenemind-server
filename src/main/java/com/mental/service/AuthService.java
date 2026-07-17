package com.mental.service;

import com.mental.dto.*;
import com.mental.model.entity.RefreshToken;
import com.mental.model.entity.User;
import com.mental.model.entity.UserProfile;
import com.mental.model.entity.enums.Role;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setAvatar("avatar-1");
        user.setProfile(profile);

        users.save(user);

        String access = jwtService.generate(user);

        String refresh = refreshTokenService.createToken(user);

        return new AuthResponse(access, refresh);
    }

    public AuthResponse login(LoginRequest req) {

        User user = users.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        // 👈 ယူဆာ Login အောင်မြင်ပြီဖြစ်၍ ၎င်း၏ FCM Token ကို အသစ်/အဟောင်း စစ်ပြီး Database ထဲ သိမ်းဆည်းခြင်း
        if (req.fcmToken() != null && !req.fcmToken().isBlank()) {
            // UserPrincipal သို့မဟုတ် User Entity တိုက်ရိုက် သုံးနိုင်ရန် FcmTokenService ပေါ်မူတည်၍ ပြင်ပါ
            // ဤနေရာတွင် User Entity ကို တိုက်ရိုက် လက်ခံသည့် Method အသစ်ဖြင့် သိမ်းခြင်းဖြစ်ပါသည်
            fcmTokenService.saveTokenForUser(user, req.fcmToken());
        }

        String access = jwtService.generate(user);
        String refresh = refreshTokenService.createToken(user);

        return new AuthResponse(access, refresh);
    }

    public AuthResponse refresh(
            RefreshRequest request
    ){


        RefreshToken token =
                refreshTokenService
                        .validate(
                                request.refreshToken()
                        );



        User user =
                token.getUser();




// rotate token

        refreshTokenService.revoke(
                request.refreshToken()
        );



        String newRefresh =
                refreshTokenService.createToken(
                        user
                );


        String access =
                jwtService.generate(
                        user
                );



        return new AuthResponse(
                access,
                newRefresh
        );

    }







    public void logout(
            LogoutRequest request
    ){


        refreshTokenService.revoke(
                request.refreshToken()
        );

    }

}