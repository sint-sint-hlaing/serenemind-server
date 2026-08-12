package com.mental.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "journals", indexes = {
    @Index(name = "idx_journals_user_created", columnList = "user_id, created_at DESC")
})
@Getter
@Setter
public class Journal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String title;

    private String content;

    private boolean flagged;
    private String flagReason;

    @Column(nullable = false)
    private boolean favourite = false;

    @Column(length = 512)
    private String tags;

    @Column(length = 1024)
    private String photoUrl;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @Lob
    private String encryptedText;

    @OneToOne(mappedBy = "journal", cascade = CascadeType.ALL)
    private JournalAnalysis analysis;
}
