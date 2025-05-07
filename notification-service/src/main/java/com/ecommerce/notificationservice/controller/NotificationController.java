package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.EmailRequest;
import com.ecommerce.notificationservice.dto.NotificationDto;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.dto.PagedResponse;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.service.EmailService;
import com.ecommerce.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final EmailService emailService;
    
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(@Valid @RequestBody NotificationDto notificationDto) {
        return new ResponseEntity<>(notificationService.createNotification(notificationDto), HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<NotificationDto> getNotificationById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResponse<NotificationDto>> getNotificationsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.getNotificationsByUserId(userId, page, size));
    }
    
    @GetMapping
    public ResponseEntity<PagedResponse<NotificationDto>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.getAllNotifications(page, size));
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<PagedResponse<NotificationDto>> getNotificationsByStatus(
            @PathVariable NotificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.getNotificationsByStatus(status, page, size));
    }
    
    @GetMapping("/type/{type}")
    public ResponseEntity<PagedResponse<NotificationDto>> getNotificationsByType(
            @PathVariable NotificationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.getNotificationsByType(type, page, size));
    }
    
    @PostMapping("/email")
    public ResponseEntity<NotificationResponse> sendEmail(@Valid @RequestBody EmailRequest emailRequest) {
        return ResponseEntity.ok(emailService.sendEmail(emailRequest));
    }
    
    @PostMapping("/email/template")
    public ResponseEntity<NotificationResponse> sendTemplatedEmail(
            @RequestParam Long userId,
            @RequestParam String recipient,
            @RequestParam String templateName,
            @RequestBody Map<String, Object> templateModel,
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) String referenceType) {
        return ResponseEntity.ok(emailService.sendTemplatedEmail(
                userId, recipient, templateName, templateModel, referenceId, referenceType));
    }
    
    @PostMapping("/retry-failed")
    public ResponseEntity<Void> retryFailedNotifications() {
        notificationService.retryFailedNotifications();
        return ResponseEntity.ok().build();
    }
}
