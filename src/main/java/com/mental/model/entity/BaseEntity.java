package com.mental.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== Convenience Methods =====

    public Instant getCreatedAtAsInstant() {
        return createdAt != null ? createdAt.toInstant(java.time.ZoneOffset.UTC) : null;
    }

    public Instant getUpdatedAtAsInstant() {
        return updatedAt != null ? updatedAt.toInstant(java.time.ZoneOffset.UTC) : null;
    }

    public LocalDateTime getCreatedAtLocalDateTime() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAtLocalDateTime() {
        return updatedAt;
    }
}