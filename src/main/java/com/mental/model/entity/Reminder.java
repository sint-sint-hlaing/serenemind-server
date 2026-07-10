package com.mental.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reminders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String repeatType;

    @Column(nullable = false)
    private LocalTime reminderTime;

    @Column(nullable = false)
    private LocalDate startDate;

    private String reminderTone;

    private String note;

    @Column(nullable = false)
    private boolean enabled;
}