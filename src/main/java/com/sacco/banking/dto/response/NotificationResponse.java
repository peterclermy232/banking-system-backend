package com.sacco.banking.dto.response;

import com.sacco.banking.entity.Notification;
import com.sacco.banking.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private Boolean isRead;
    private LocalDateTime readDate;
    private LocalDateTime createdDate;
    private LocalDateTime updatedAt;
    private String referenceId;
    private String referenceType;
    private Integer priority;
    private LocalDateTime expiresAt;

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .readDate(notification.getReadDate())
                .createdDate(notification.getCreatedDate())
                .updatedAt(notification.getUpdatedAt())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .priority(notification.getPriority())
                .expiresAt(notification.getExpiresAt())
                .build();
    }
}
