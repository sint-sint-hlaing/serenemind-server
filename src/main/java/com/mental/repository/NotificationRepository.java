package com.mental.repository;

import com.mental.model.entity.Notification;
import com.mental.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long> {
    List<Notification> findByUserUsernameOrderByCreatedAtDesc(String username);

    long countByUserIdAndIsReadFalse(Long id);
    Page<Notification> findByUserIdAndIsReadFalse(Long id, Pageable pageable);

   // List<Notification> findByUerIdAndIsReadFalse(Long id);
    // User အလိုက် Notification အားလုံးကို အချိန်အသစ်ဆုံးကနေ စီထုတ်ရန် (All Tab အတွက်)
        List<Notification> findByUserOrderByCreatedAtDesc(User user);

        // မဖတ်ရသေးသော Notification များကို အချိန်အသစ်ဆုံးကနေ စီထုတ်ရန် (Unread Tab အတွက်)
        List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

        // အမျိုးအစားအလိုက် ခွဲထုတ်ရန် (Mentions သို့မဟုတ် System Tab အတွက်)
        List<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, String type);

        // Notification တစ်ခုချင်းစီကို Mark as read လုပ်ဖို့ ID နှင့် User ပိုင်ရှင် ဟုတ်မဟုတ် စစ်ဆေးရန်
        Optional<Notification> findByIdAndUser(Long id, User user);

        // မဖတ်ရသေးသော Notification အားလုံးကို ရှာရန် (Mark All as Read အတွက်)
        List<Notification> findByUserAndIsReadFalse(User user);

        void deleteByUserAndTitleAndMessage(User user, String title, String message);

}
