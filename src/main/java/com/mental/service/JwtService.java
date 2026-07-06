package com.mental.service;

import com.mental.model.entity.User;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    // လုံခြုံစိတ်ချရအောင် စာလုံးရေ ၆၄ လုံး (64 bytes / 512 bits) ပြည့်အောင် သတ်မှတ်ထားပါတယ်
    private static final String SECRET_KEY = "YourSuperSecretKeyThatIsAtLeast64BytesLongAndVerySecureForHS512Algorithm!!!";

    // SecretKey ကို ဗဟိုကနေ တစ်နေရာထဲကပဲ ခေါ်သုံးဖို့ ပြင်ဆင်ခြင်း
    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 နာရီ
                .signWith(getSignInKey()) // ဒီနေရာမှာ ဗဟို key ကို သုံးထားပါတယ်
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey()) // ဒီနေရာမှာလည်း ဗဟို key ကိုပဲ သုံးပြီး စစ်ထားပါတယ်
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}