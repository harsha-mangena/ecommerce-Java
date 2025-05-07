package com.ecommerce.notificationservice.service.impl;

import com.ecommerce.notificationservice.dto.NotificationDto;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.dto.PagedResponse;
import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import com.ecommerce.notificationservice.event.PaymentCompletedEvent;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import com.ecommerce.notificationservice.service.EmailService;
import com.ecommerce.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public NotificationResponse createNotification(NotificationDto notificationDto) {
        try {
            Notification notification = mapToEntity(notificationDto);
            Notification savedNotification = notificationRepository.save(notification);
            
            return NotificationResponse.builder()
                    .success(true)
                    .message("Notification created successfully")
                    .notificationId(savedNotification.getId())
                    .build();
        } catch (Exception e) {
            log.error("Failed to create notification", e);
            return NotificationResponse.builder()
                    .success(false)
                    .message("Failed to create notification: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDto getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
                
        return mapToDto(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> getNotificationsByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notificationsPage = notificationRepository.findByUserId(userId, pageable);
        
        List<NotificationDto> content = notificationsPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
                
        return new PagedResponse<>(
                content,
                notificationsPage.getNumber(),
                notificationsPage.getSize(),
                notificationsPage.getTotalElements(),
                notificationsPage.getTotalPages(),
                notificationsPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> getAllNotifications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notificationsPage = notificationRepository.findAll(pageable);
        
        List<NotificationDto> content = notificationsPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
                
        return new PagedResponse<>(
                content,
                notificationsPage.getNumber(),
                notificationsPage.getSize(),
                notificationsPage.getTotalElements(),
                notificationsPage.getTotalPages(),
                notificationsPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> getNotificationsByStatus(NotificationStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notificationsPage = notificationRepository.findByStatus(status, pageable);
        
        List<NotificationDto> content = notificationsPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
                
        return new PagedResponse<>(
                content,
                notificationsPage.getNumber(),
                notificationsPage.getSize(),
                notificationsPage.getTotalElements(),
                notificationsPage.getTotalPages(),
                notificationsPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> getNotificationsByType(NotificationType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notificationsPage = notificationRepository.findAll(pageable);
        
        List<NotificationDto> content = notificationsPage.getContent().stream()
                .filter(notification -> notification.getType() == type)
                .map(this::mapToDto)
                .collect(Collectors.toList());
                
        return new PagedResponse<>(
                content,
                notificationsPage.getNumber(),
                notificationsPage.getSize(),
                content.size(),
                notificationsPage.getTotalPages(),
                notificationsPage.isLast()
        );
    }

    @Override
    @KafkaListener(topics = "${kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreatedEvent(OrderCreatedEvent event, Acknowledgment acknowledgment) {
        log.info("Received order created event: {}", event.getOrderNumber());
        
        try {
            // Prepare template model
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("customerName", event.getCustomerName());
            templateModel.put("orderNumber", event.getOrderNumber());
            templateModel.put("orderDate", event.getOrderDate());
            templateModel.put("totalAmount", event.getTotalAmount());
            templateModel.put("shippingAddress", event.getShippingAddress());
            
            // Format order items for the template
            StringBuilder orderItemsHtml = new StringBuilder();
            for (OrderCreatedEvent.OrderItemDto item : event.getOrderItems()) {
                orderItemsHtml.append("<tr>")
                        .append("<td>").append(item.getProductName()).append("</td>")
                        .append("<td>").append(item.getQuantity()).append("</td>")
                        .append("<td>").append(item.getPrice()).append("</td>")
                        .append("<td>").append(item.getTotalPrice()).append("</td>")
                        .append("</tr>");
            }
            templateModel.put("orderItems", orderItemsHtml.toString());
            
            // Send email notification
            NotificationResponse response = emailService.sendTemplatedEmail(
                    event.getUserId(),
                    event.getCustomerEmail(),
                    "order-confirmation-template.html",
                    templateModel,
                    event.getOrderNumber(),
                    "ORDER"
            );
            
            if (response.isSuccess()) {
                log.info("Order confirmation email sent successfully for order: {}", event.getOrderNumber());
            } else {
                log.error("Failed to send order confirmation email: {}", response.getMessage());
            }
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order created event", e);
            // Don't acknowledge to retry
        }
    }

    @Override
    @KafkaListener(topics = "${kafka.topics.payment-completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentCompletedEvent(PaymentCompletedEvent event, Acknowledgment acknowledgment) {
        log.info("Received payment completed event for order: {}", event.getOrderNumber());
        
        try {
            // Prepare template model
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("customerName", event.getCustomerName());
            templateModel.put("orderNumber", event.getOrderNumber());
            templateModel.put("paymentDate", event.getPaymentDate());
            templateModel.put("paymentMethod", event.getPaymentMethod());
            templateModel.put("amount", event.getAmount());
            templateModel.put("transactionId", event.getTransactionId());
            
            // Send email notification
            NotificationResponse response = emailService.sendTemplatedEmail(
                    event.getUserId(),
                    event.getCustomerEmail(),
                    "payment-confirmation-template.html",
                    templateModel,
                    event.getOrderNumber(),
                    "PAYMENT"
            );
            
            if (response.isSuccess()) {
                log.info("Payment confirmation email sent successfully for order: {}", event.getOrderNumber());
            } else {
                log.error("Failed to send payment confirmation email: {}", response.getMessage());
            }
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment completed event", e);
            // Don't acknowledge to retry
        }
    }

    @Override
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void retryFailedNotifications() {
        log.info("Starting retry for failed notifications");
        List<Notification> failedNotifications = notificationRepository
                .findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3);
                
        for (Notification notification : failedNotifications) {
            try {
                if (notification.getType() == NotificationType.EMAIL) {
                    // Retry sending email using EmailService instead of direct implementation
                    Map<String, Object> model = new HashMap<>();
                    NotificationResponse response = emailService.sendEmail(EmailRequest.builder()
                            .userId(notification.getUserId())
                            .recipient(notification.getRecipient())
                            .subject(notification.getSubject())
                            .body(notification.getContent())
                            .templateName(notification.getTemplateName())
                            .referenceId(notification.getReferenceId())
                            .referenceType(notification.getReferenceType())
                            .build());
                    
                    if (response.isSuccess()) {
                        notification.setStatus(NotificationStatus.SENT);
                        notification.setSentAt(LocalDateTime.now());
                        notification.setUpdatedAt(LocalDateTime.now());
                        notificationRepository.save(notification);
                        
                        log.info("Successfully retried notification ID: {}", notification.getId());
                    } else {
                        throw new RuntimeException(response.getMessage());
                    }
                }
                // Add handling for other notification types if implemented
                
            } catch (Exception e) {
                log.error("Failed to retry notification ID: {}", notification.getId(), e);
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setFailureReason(e.getMessage());
                notification.setUpdatedAt(LocalDateTime.now());
                notificationRepository.save(notification);
            }
        }
    }

    @Override
    @Scheduled(cron = "0 0 1 * * ?") // Run at 1 AM every day
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Starting cleanup of old notifications");
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(3); // Keep notifications for 3 months
        
        List<Notification> oldNotifications = notificationRepository
                .findByStatusAndCreatedAtBefore(NotificationStatus.SENT, cutoffDate);
                
        if (!oldNotifications.isEmpty()) {
            notificationRepository.deleteAll(oldNotifications);
            log.info("Deleted {} old notifications", oldNotifications.size());
        }
    }
    
    private NotificationDto mapToDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .recipient(notification.getRecipient())
                .type(notification.getType())
                .subject(notification.getSubject())
                .content(notification.getContent())
                .templateName(notification.getTemplateName())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .status(notification.getStatus())
                .failureReason(notification.getFailureReason())
                .retryCount(notification.getRetryCount())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
    
    private Notification mapToEntity(NotificationDto dto) {
        Notification notification = new Notification();
        notification.setUserId(dto.getUserId());
        notification.setRecipient(dto.getRecipient());
        notification.setType(dto.getType());
        notification.setSubject(dto.getSubject());
        notification.setContent(dto.getContent());
        notification.setTemplateName(dto.getTemplateName());
        notification.setReferenceId(dto.getReferenceId());
        notification.setReferenceType(dto.getReferenceType());
        notification.setStatus(dto.getStatus() != null ? dto.getStatus() : NotificationStatus.PENDING);
        notification.setFailureReason(dto.getFailureReason());
        notification.setRetryCount(dto.getRetryCount() != null ? dto.getRetryCount() : 0);
        notification.setSentAt(dto.getSentAt());
        
        return notification;
    }
}
