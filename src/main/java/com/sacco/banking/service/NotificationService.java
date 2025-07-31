package com.sacco.banking.service;

import com.sacco.banking.dto.request.NotificationCreateRequest;
import com.sacco.banking.dto.response.NotificationCountResponse;
import com.sacco.banking.dto.response.NotificationResponse;
import com.sacco.banking.entity.Notification;
import com.sacco.banking.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationResponse createNotification(NotificationCreateRequest request) {
        Notification notification = Notification.builder()
                .memberNumber(request.getMemberNumber())
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .priority(request.getPriority() != null ? request.getPriority() : 1)
                .expiresAt(request.getExpiresAt())
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Created notification {} for member {}", notification.getId(), request.getMemberNumber());

        return NotificationResponse.fromEntity(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(String memberNumber, int page, int size, boolean unreadOnly) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Notification> notifications = unreadOnly
                ? notificationRepository.findByMemberNumberAndIsReadFalseOrderByCreatedDateDesc(memberNumber, pageable)
                : notificationRepository.findByMemberNumberOrderByCreatedDateDesc(memberNumber, pageable);


        return notifications.map(NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public NotificationCountResponse getNotificationCounts(String memberNumber) {
        long unreadCount = notificationRepository.countByMemberNumberAndIsReadFalse(memberNumber);
        long totalCount = notificationRepository.countByMemberNumber(memberNumber);
        long highPriorityCount = notificationRepository.findHighPriorityUnreadNotifications(memberNumber).size();

        return NotificationCountResponse.builder()
                .unreadCount(unreadCount)
                .totalCount(totalCount)
                .highPriorityCount(highPriorityCount)
                .build();
    }

    public boolean markAsRead(Long notificationId, String memberNumber) {
        int updated = notificationRepository.markAsRead(notificationId, memberNumber, LocalDateTime.now());
        if (updated > 0) {
            log.info("Marked notification {} as read for member {}", notificationId, memberNumber);
            return true;
        }
        return false;
    }

    public int markAllAsRead(String memberNumber) {
        int updated = notificationRepository.markAllAsRead(memberNumber, LocalDateTime.now());
        log.info("Marked {} notifications as read for member {}", updated, memberNumber);
        return updated;
    }

    public boolean deleteNotification(Long notificationId, String memberNumber) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        if (notification.isPresent() && notification.get().getMemberNumber().equals(memberNumber)) {
            notificationRepository.deleteById(notificationId);
            log.info("Deleted notification {} for member {}", notificationId, memberNumber);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(String memberNumber) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Notification> notifications = notificationRepository.findRecentNotifications(memberNumber, thirtyDaysAgo);
        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Async
    public void createTransactionNotification(String memberNumber, String transactionType,
                                              Double amount, String transactionId) {
        String title = getTransactionNotificationTitle(transactionType);
        String message = String.format("Your %s of KSH %.2f has been processed successfully.",
                transactionType.toLowerCase(), amount);

        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setMemberNumber(memberNumber);
        request.setTitle(title);
        request.setMessage(message);
        request.setType(Notification.NotificationType.SUCCESS);
        request.setReferenceId(transactionId);
        request.setReferenceType("TRANSACTION");
        request.setPriority(2);

        createNotification(request);
    }

    @Async
    public void createAccountNotification(String memberNumber, String accountType, String message,
                                          Notification.NotificationType type) {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setMemberNumber(memberNumber);
        request.setTitle("Account Update");
        request.setMessage(message);
        request.setType(type);
        request.setReferenceType("ACCOUNT");
        request.setPriority(type == Notification.NotificationType.ERROR ? 3 : 1);

        createNotification(request);
    }

    @Async
    public void createLoanNotification(String memberNumber, String loanId, String status,
                                       Double amount, Notification.NotificationType type) {
        String title = "Loan Update";
        String message = String.format("Your loan application for KSH %.2f is now %s.", amount, status.toLowerCase());

        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setMemberNumber(memberNumber);
        request.setTitle(title);
        request.setMessage(message);
        request.setType(type);
        request.setReferenceId(loanId);
        request.setReferenceType("LOAN");
        request.setPriority(3);

        createNotification(request);
    }

    @Async
    public void createSystemNotification(String memberNumber, String title, String message,
                                         Notification.NotificationType type, Integer priority) {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setMemberNumber(memberNumber);
        request.setTitle(title);
        request.setMessage(message);
        request.setType(type);
        request.setReferenceType("SYSTEM");
        request.setPriority(priority != null ? priority : 1);

        createNotification(request);
    }

    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        int deletedOld = notificationRepository.deleteOldNotifications(cutoff);
        log.info("Deleted {} old notifications", deletedOld);

        int deletedExpired = notificationRepository.deleteExpiredNotifications(LocalDateTime.now());
        log.info("Deleted {} expired notifications", deletedExpired);
    }

    private String getTransactionNotificationTitle(String transactionType) {
        return switch (transactionType.toUpperCase()) {
            case "DEPOSIT" -> "Deposit Successful";
            case "WITHDRAWAL" -> "Withdrawal Successful";
            case "TRANSFER" -> "Transfer Successful";
            case "LOAN" -> "Loan Transaction";
            case "PAYMENT" -> "Payment Successful";
            default -> "Transaction Successful";
        };
    }
}
