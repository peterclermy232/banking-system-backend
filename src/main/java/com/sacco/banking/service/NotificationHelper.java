package com.sacco.banking.service;

import com.sacco.banking.entity.Notification;
import com.sacco.banking.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationHelper {

    private final NotificationRepository notificationRepository;
    public void notifyWelcome(String memberNumber, String name) {
        log.info("Welcome notification sent to {} ({})", name, memberNumber);
    }

    public void notifyPasswordChanged(String memberNumber) {
        log.info("Password change notification sent to {}", memberNumber);
    }

    public void notifySecurityAlert(String memberNumber, String message) {
        log.info("Security alert for {}: {}", memberNumber, message);
    }
    public void notifyAccountCreated(String memberNumber, String accountType) {
        String title = "New Account Created";
        String message = String.format("Your %s account has been successfully created and is now active.", accountType);

        createNotification(memberNumber, title, message, Notification.NotificationType.SUCCESS, 1);
    }

    public void notifyAccountNotification(String memberNumber, String accountType, String message, Notification.NotificationType type) {
        String title = "Account Notification";
        createNotification(memberNumber, title, message, type, 2);
    }

    public void notifyLowBalance(String memberNumber, String accountType, double balance) {
        String title = "Low Balance Alert";
        String message = String.format("Your %s account balance is low: KSH %.2f. Please consider making a deposit.", accountType, balance);

        createNotification(memberNumber, title, message, Notification.NotificationType.WARNING, 3);
    }

    public void notifySystemNotification(String memberNumber, String title, String message, Notification.NotificationType type, int priority) {
        createNotification(memberNumber, title, message, type, priority);
    }

    private void createNotification(String memberNumber, String title, String message, Notification.NotificationType type, int priority) {
        try {
            Notification notification = Notification.builder()
                    .memberNumber(memberNumber)
                    .title(title)
                    .message(message)
                    .type(type)
                    .priority(priority)
                    .isRead(false)
                    .createdDate(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
            log.info("Notification created for member {}: {}", memberNumber, title);

        } catch (Exception e) {
            log.error("Failed to create notification for member {}: {}", memberNumber, e.getMessage());
        }
    }
}