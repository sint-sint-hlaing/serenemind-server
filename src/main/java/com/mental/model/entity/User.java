package com.mental.model.entity;

import com.mental.model.entity.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile profile;

    private boolean isActive;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<MoodEntry> moods;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Journal> journals;

    private LocalDateTime lastLogin;
    private LocalDateTime loginTime;

    public UserProfile getUserProfile() {
        return profile;
    }
}