package com.sacco.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "member_number", nullable = false)
    private String memberNumber;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;


    @Column(name = "read_date")
    private LocalDateTime readDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedAt;


    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public enum NotificationType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR,
        SYSTEM
    }

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (isRead == null) {
            isRead = false;
        }
        if (priority == null) {
            priority = 1;
        }
    }
}