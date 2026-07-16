package com.mental.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "journals")
@Getter
@Setter
public class Journal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String title;

    // Decrypted content field (not persisted — used as transient helper if needed)
    private String content;

    private boolean flagged;
    private String flagReason;


    /** Whether user marked this journal as a favourite */
    @Column(nullable = false)
    private boolean favourite = false;

    /** Whether journal is private (locked) – hidden in community/admin views */
    @Column(nullable = false)
    private boolean isPrivate = false;

    /** Comma-separated list of user-defined tags e.g. "gratitude,family" */
    @Column(length = 512)
    private String tags;

    /** URL / path of an attached photo */
    @Column(length = 1024)
    private String photoUrl;

    @Lob
    private String encryptedText;

    @OneToOne(mappedBy = "journal", cascade = CascadeType.ALL)
    private JournalAnalysis analysis;
}
