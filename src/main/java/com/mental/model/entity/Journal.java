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

    private String content;
    private boolean flagged;
    private String flagReason;


    @Lob
    private String encryptedText;

    @OneToOne(mappedBy = "journal", cascade = CascadeType.ALL)
    private JournalAnalysis analysis;
}
