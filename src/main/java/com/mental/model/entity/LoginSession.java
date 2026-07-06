package com.mental.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "login_sessions")
@Getter
@Setter
public class LoginSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String device;
    private String ipAddress;
    private Instant lastLogin;
}
