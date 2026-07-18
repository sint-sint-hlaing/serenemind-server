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

import java.util.Optional;

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
        // ၁။ ၎င်း Token သည် Database ထဲတွင် ရှိပြီးသားလား အရင်စစ်ဆေးပါသည်
        Optional<DeviceToken> existingTokenOpt = deviceTokenRepository.findByToken(token);

        if (existingTokenOpt.isPresent()) {
            DeviceToken existingToken = existingTokenOpt.get();

            // ၂။ Token ရှိပြီးသားဖြစ်ပြီး လက်ရှိ Login ဝင်မည့် User နှင့် တူညီပါက ထပ်မသိမ်းဘဲ ကျော်သွားမည်
            if (existingToken.getUser().getId().equals(user.getId())) {
                return;
            }

            // ၃။ Edge Case: Token က ရှိပြီးသား ဖြစ်သော်လည်း တခြား User အဟောင်းထံတွင် ကပ်နေပါက
            // Data မမှားစေရန် User အဟောင်းထံမှ ဖြုတ်ပြီး လက်ရှိ User သစ်ဆီသို့ လွှဲပြောင်းပေးပါမည်
            existingToken.setUser(user);
            deviceTokenRepository.save(existingToken);
        } else {
            // ၄။ Database ထဲတွင် လုံးဝမရှိသေးသော Token အသစ်ဖြစ်ပါက အသစ်ဆောက်၍ သိမ်းဆည်းပါမည်
            DeviceToken newDeviceToken = new DeviceToken();
            newDeviceToken.setUser(user);
            newDeviceToken.setToken(token);

            deviceTokenRepository.save(newDeviceToken);
        }
    }
}