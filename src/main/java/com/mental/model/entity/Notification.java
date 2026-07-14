package com.mental.model.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.mental.model.entity.enums.NotificationType;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private String title; // e.g., "New like on your journal", "Reminder missed"

    @Column(nullable = false, length = 500)
    private String message; // e.g., "Aye Thinzar liked your journal..."

    @Column(nullable = false)
    private String type; // LIKE, COMMENT, REMINDER, SYSTEM, GOAL, STREAK, INSPIRATION

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "target_id")
    private Long targetId; // Post ID သို့မဟုတ် Reminder ID သိမ်းရန်

    @Column(name = "target_type")
    private String targetType;
}
