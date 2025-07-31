package com.sacco.banking.repository;

import com.sacco.banking.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByMemberNumberOrderByCreatedDateDesc(String memberNumber, Pageable pageable);

    Page<Notification> findByMemberNumberAndIsReadFalseOrderByCreatedDateDesc(String memberNumber, Pageable pageable);



    long countByMemberNumber(String memberNumber);

    long countByMemberNumberAndIsReadFalse(String memberNumber);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.id = :notificationId AND n.memberNumber = :memberNumber")
    int markAsRead(Long notificationId, String memberNumber, LocalDateTime readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.memberNumber = :memberNumber AND n.isRead = false")
    int markAllAsRead(String memberNumber, LocalDateTime readAt);

    @Query("SELECT n FROM Notification n WHERE n.memberNumber = :memberNumber AND n.priority >= 3 AND n.isRead = false")
    List<Notification> findHighPriorityUnreadNotifications(String memberNumber);

    @Query("SELECT n FROM Notification n WHERE n.memberNumber = :memberNumber AND n.createdDate >= :cutoffDate ORDER BY n.createdDate DESC")
    List<Notification> findRecentNotifications(String memberNumber, LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdDate < :cutoff")
    int deleteOldNotifications(LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt < :now")
    int deleteExpiredNotifications(LocalDateTime now);
}
