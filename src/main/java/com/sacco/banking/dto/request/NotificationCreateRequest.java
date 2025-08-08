package com.sacco.banking.dto.request;

import com.sacco.banking.entity.Notification;
import com.sacco.banking.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationCreateRequest {

    @NotBlank(message = "Member number is required")
    private String memberNumber;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Type is required")
    private NotificationType type;

    private String referenceId;
    private String referenceType;
    private Integer priority = 1;
    private LocalDateTime expiresAt;
}
