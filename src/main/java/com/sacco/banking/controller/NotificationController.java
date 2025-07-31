package com.sacco.banking.controller;

import com.sacco.banking.dto.request.NotificationCreateRequest;
import com.sacco.banking.dto.response.NotificationCountResponse;
import com.sacco.banking.dto.response.NotificationResponse;
import com.sacco.banking.security.UserPrincipal;
import com.sacco.banking.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management APIs")
@SecurityRequirement(name = "Bearer Authentication")
@CrossOrigin(originPatterns = "*", maxAge = 3600)
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get notifications", description = "Retrieve member notifications with pagination")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {

        Page<NotificationResponse> notifications = notificationService.getNotifications(
                userPrincipal.getMemberNumber(), page, size, unreadOnly);

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count", description = "Get count of unread notifications for member")
    public ResponseEntity<NotificationCountResponse> getUnreadCount(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        NotificationCountResponse counts = notificationService.getNotificationCounts(userPrincipal.getMemberNumber());
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent notifications", description = "Get recent notifications (last 30 days)")
    public ResponseEntity<List<NotificationResponse>> getRecentNotifications(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<NotificationResponse> notifications = notificationService.getRecentNotifications(userPrincipal.getMemberNumber());
        return ResponseEntity.ok(notifications);
    }

    @PostMapping
    @Operation(summary = "Create notification", description = "Create a new notification (admin only)")
    public ResponseEntity<NotificationResponse> createNotification(@Valid @RequestBody NotificationCreateRequest request) {
        NotificationResponse notification = notificationService.createNotification(request);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        boolean success = notificationService.markAsRead(id, userPrincipal.getMemberNumber());

        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Notification marked as read" : "Notification not found or already read"
        ));
    }

    @PutMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read", description = "Mark all notifications as read for the member")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        int updated = notificationService.markAllAsRead(userPrincipal.getMemberNumber());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "updatedCount", updated,
                "message", updated + " notifications marked as read"
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification", description = "Delete a specific notification")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        boolean success = notificationService.deleteNotification(id, userPrincipal.getMemberNumber());

        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Notification deleted successfully" : "Notification not found"
        ));
    }
}