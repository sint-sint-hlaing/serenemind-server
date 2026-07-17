package com.mental.service;

import com.mental.model.entity.DeviceToken;
import com.mental.model.entity.User;
import com.mental.repository.DeviceTokenRepository;
import com.mental.repository.UserRepository;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveToken(UserPrincipal userPrincipal, String token) {
        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Token ရှိပြီးသားဆို ထပ်မသိမ်းဘဲ ကျော်မယ်၊ မရှိရင် အသစ်သွင်းမယ်
        if (!deviceTokenRepository.existsByToken(token)) {
            DeviceToken deviceToken = DeviceToken.builder()
                    .user(user)
                    .token(token)
                    .build();
            deviceTokenRepository.save(deviceToken);
        }
    }

    @Transactional
    public void saveTokenForUser(User user, String token) {
        if (!deviceTokenRepository.existsByToken(token)) {
            DeviceToken deviceToken = DeviceToken.builder()
                    .user(user)
                    .token(token)
                    .build();
            deviceTokenRepository.save(deviceToken);
        }
    }
}