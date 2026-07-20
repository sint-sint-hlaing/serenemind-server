package com.mental.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Report {

        @Id
        private Long id;

        private String reason;

        private String status;

        private long today;
        @CreationTimestamp // Report အသစ်လုပ်တိုင်း အလိုအလျောက် အချိန်မှတ်ပေးရန်
        private LocalDateTime createdAt;

        @ManyToOne
        private User reportedBy;

        @ManyToOne
        private Post post;
    }

