package com.ecommerce.notificationservice.dto;

import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private Long userId;
    private String recipient;
    private NotificationType type;
    private String subject;
    private String content;
    private String templateName;
    private String referenceId;
    private String referenceType;
    private NotificationStatus status;
    private String failureReason;
    private Integer retryCount;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
