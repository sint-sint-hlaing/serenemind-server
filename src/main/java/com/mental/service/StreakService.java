package com.mental.service;

import com.mental.dto.Streak.StreakResponse;
import com.mental.model.entity.User;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserRepository userRepository;

    // User က App ထဲမှာ အလုပ်တစ်ခုခုလုပ်တိုင်း (Post တင်ခြင်း၊ မန့်ခြင်း) ဤ Method ကို လှမ်းခေါ်ပေးရပါမည်
    @Transactional
    public StreakResponse updateStreak(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        LocalDate today = LocalDate.now();
        LocalDate lastActive = user.getLastActiveDate();
        boolean isNewBest = false;

        if (lastActive == null) {
            // ပထမဆုံးအကြိမ် App သုံးခြင်း
            user.setCurrentStreak(1);
            user.setLongestStreak(Math.max(user.getLongestStreak(), 1));
            user.setLastActiveDate(today);
        } else if (lastActive.isBefore(today)) {
            if (lastActive.equals(today.minusDays(1))) {
                // မနေ့ကလည်း သုံးခဲ့တယ်၊ ဒီနေ့လည်း သုံးတယ် -> Streak ၁ ရက်တိုးမယ်
                int newStreak = user.getCurrentStreak() + 1;
                user.setCurrentStreak(newStreak);

                // စံချိန်ဟောင်း ကျော်မကျော် စစ်ဆေးခြင်း (Screen 11 အတွက်)
                if (newStreak > user.getLongestStreak()) {
                    user.setLongestStreak(newStreak);
                    isNewBest = true;
                }
            } else {
                // ရက်ကျော်သွားပြီ ဖြစ်သော်လည်း ဒီနေရာမှာတင် Streak ကို ၀ လို့ တန်းမချသေးပါ (Screen 10 အတွက် အခွင့်အရေးချန်ထားမည်)
                // ကွန်ထရိုလာကနေ အရင်စစ်ပြီးမှ ရှင်းမှာမို့ ဒီအတိုင်း ထားပါမယ်
                user.setCurrentStreak(1);
            }
            user.setLastActiveDate(today);
        }
        // ယနေ့ ရက်စွဲတူနေလျှင် ဘာမှမလုပ်ပါ (ဒီနေ့ Streak တိုးပြီးသားမို့)

        User savedUser = userRepository.save(user);
        return convertToStreakResponse(savedUser, isNewBest);
    }

    // Screen 3: လက်ရှိ Streak အခြေအနေများကို ဆွဲထုတ်ခြင်း
    @Transactional(readOnly = true)
    public StreakResponse getStreakDetails(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // အကယ်၍ မနေ့ကကော ဒီနေ့ကော App မဖွင့်ခဲ့ရင် Streak ပြတ်သွားပြီဖြစ်လို့ ၀ ချပေးရန်
        LocalDate today = LocalDate.now();
        if (user.getLastActiveDate() != null && user.getLastActiveDate().isBefore(today.minusDays(1))) {
            user.setCurrentStreak(0); // ဒါပေမဲ့ DB ထဲမှာ Save မလုပ်သေးပါ (Freeze သုံးဖို့ အခွင့်အရေးပေးရန်)
        }

        return convertToStreakResponse(user, false);
    }

    // Screen 10: Streak Freeze အသုံးပြုပြီး Streak ကို အသက်ဆက်ခြင်း
    @Transactional
    public StreakResponse useStreakFreeze(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getStreakFreezeCount() <= 0) {
            throw new IllegalStateException("No streak freeze available");
        }

        // Freeze တစ်ခုလျှော့ပြီး ယမန်နေ့က သုံးခဲ့သလိုမျိုး ရက်စွဲကို မနေ့ကရက် ပြန်ပြင်ပေးလိုက်ခြင်း
        user.setStreakFreezeCount(user.getStreakFreezeCount() - 1);
        user.setLastActiveDate(LocalDate.now().minusDays(1));

        // ရက်စွဲပြင်ပြီးတာနဲ့ Streak ကို ပြန်တိုးပေးလိုက်ပါမယ်
        User savedUser = userRepository.save(user);
        return updateStreak(email);
    }

    // Helper Method: Weekly Overview (Mon-Sun) တွက်ချက်ခြင်း
    private StreakResponse convertToStreakResponse(User user, boolean isNewBest) {
        List<Boolean> weeklyOverview = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // ဒီပတ်ရဲ့ တနင်္လာနေ့ရက်စွဲကို ရှာခြင်း
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        // တနင်္လာကနေ တနင်္ဂနွေအထိ ၇ ရက် ပတ်စစ်ခြင်း
        for (int i = 0; i < 7; i++) {
            LocalDate currentDay = monday.plusDays(i);

            // ရက်စွဲက ဒီနေ့မတိုင်ခင်ဖြစ်ပြီး User ရဲ့ LastActive ထက် စောနေရင် true ပေးခြင်း (ရိုးရှင်းသော logic အဖြစ် သုံးထားသည်)
            if (user.getLastActiveDate() != null && !currentDay.isAfter(today)) {
                // လက်ရှိ Streak ရက်အရေအတွက်အတွင်း အကျုံးဝင်မဝင် တွက်ချက်ခြင်း
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(currentDay, user.getLastActiveDate());
                weeklyOverview.add(daysBetween >= 0 && daysBetween < user.getCurrentStreak());
            } else {
                weeklyOverview.add(false);
            }
        }

        return StreakResponse.builder()
                .currentStreak(user.getCurrentStreak())
                .longestStreak(user.getLongestStreak())
                .streakFreezeCount(user.getStreakFreezeCount())
                .isNewBest(isNewBest)
                .weeklyOverview(weeklyOverview)
                .build();
    }
}
