package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationDto;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.dto.PagedResponse;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import com.ecommerce.notificationservice.event.PaymentCompletedEvent;

public interface NotificationService {
    
    NotificationResponse createNotification(NotificationDto notificationDto);
    
    NotificationDto getNotificationById(Long id);
    
    PagedResponse<NotificationDto> getNotificationsByUserId(Long userId, int page, int size);
    
    PagedResponse<NotificationDto> getAllNotifications(int page, int size);
    
    PagedResponse<NotificationDto> getNotificationsByStatus(NotificationStatus status, int page, int size);
    
    PagedResponse<NotificationDto> getNotificationsByType(NotificationType type, int page, int size);
    
    void handleOrderCreatedEvent(OrderCreatedEvent event);
    
    void handlePaymentCompletedEvent(PaymentCompletedEvent event);
    
    void retryFailedNotifications();
    
    void cleanupOldNotifications();
}
