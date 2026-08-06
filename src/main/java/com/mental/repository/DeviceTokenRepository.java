package com.mental.repository;

import com.mental.model.entity.DeviceToken;
import com.mental.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    // Token ရှိပြီးသားလား စစ်ဆေးရန် (Duplicate မဖြစ်အောင်)
    boolean existsByToken(String token);

    // User တစ်ယောက်တည်းရဲ့ Device Tokens အားလုံးကို ရှာရန်
    List<DeviceToken> findByUser(User user);

    // Token အဟောင်း သို့မဟုတ် တိကျသော Token တစ်ခုကို ရှာရန်
    Optional<DeviceToken> findByToken(String token);

    // User က Logout လုပ်သွားလျှင် သို့မဟုတ် Token မလိုတော့လျှင် ဖြတ်ရန်
    void deleteByToken(String token);

    // User တစ်ယောက်တည်းရဲ့ Tokens အားလုံးကို တစ်ခါတည်း ဖြတ်ရန်
    void deleteByUser(User user);
}