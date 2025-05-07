package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUserId(Long userId);
    
    List<Notification> findByStatus(NotificationStatus status);
    
    List<Notification> findByType(NotificationType type);
    
    List<Notification> findByReferenceId(String referenceId);
    
    List<Notification> findByReferenceType(String referenceType);
    
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    
    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);
    
    List<Notification> findByStatusAndCreatedAtBefore(NotificationStatus status, LocalDateTime timestamp);
    
    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, Integer maxRetries);
}
