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
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserUsernameOrderByCreatedAtDesc(String username);

    long countByUserIdAndIsReadFalse(Long id);

    Page<Notification> findByUserIdAndIsReadFalse(Long id, Pageable pageable);

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);


    List<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, String type);

    Optional<Notification> findByIdAndUser(Long id, User user);

    List<Notification> findByUserAndIsReadFalse(User user);

    void deleteByUserAndTitleAndMessage(User user, String title, String message);

}
