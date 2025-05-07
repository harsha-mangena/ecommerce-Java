package com.ecommerce.notificationservice.service.impl;

import com.ecommerce.notificationservice.dto.EmailRequest;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.entity.EmailTemplate;
import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.repository.EmailTemplateRepository;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import com.ecommerce.notificationservice.service.EmailService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final SpringTemplateEngine templateEngine;
    
    @Value("${notification.email.from}")
    private String fromEmail;
    
    @Value("${notification.email.templates.order-confirmation}")
    private String orderConfirmationTemplate;
    
    @Value("${notification.email.templates.payment-confirmation}")
    private String paymentConfirmationTemplate;

    @Override
    @Transactional
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendEmailFallback")
    public NotificationResponse sendEmail(EmailRequest emailRequest) {
        try {
            // Create notification entity
            Notification notification = new Notification();
            notification.setUserId(emailRequest.getUserId());
            notification.setRecipient(emailRequest.getRecipient());
            notification.setType(NotificationType.EMAIL);
            notification.setSubject(emailRequest.getSubject());
            notification.setContent(emailRequest.getBody());
            notification.setTemplateName(emailRequest.getTemplateName());
            notification.setReferenceId(emailRequest.getReferenceId());
            notification.setReferenceType(emailRequest.getReferenceType());
            notification.setStatus(NotificationStatus.PENDING);
            
            // Save notification first
            Notification savedNotification = notificationRepository.save(notification);
            
            // Send email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(emailRequest.getRecipient());
            helper.setSubject(emailRequest.getSubject());
            helper.setText(emailRequest.getBody(), true);
            
            mailSender.send(message);
            
            // Update notification status
            savedNotification.setStatus(NotificationStatus.SENT);
            savedNotification.setSentAt(LocalDateTime.now());
            notificationRepository.save(savedNotification);
            
            log.info("Email sent successfully to: {}", emailRequest.getRecipient());
            
            return NotificationResponse.builder()
                    .success(true)
                    .message("Email sent successfully")
                    .notificationId(savedNotification.getId())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to send email to: {}", emailRequest.getRecipient(), e);
            
            // Save failed notification
            try {
                Optional<Notification> notificationOpt = notificationRepository.findById(emailRequest.getReferenceId() != null 
                        ? Long.parseLong(emailRequest.getReferenceId()) : 0L);
                        
                if (notificationOpt.isPresent()) {
                    Notification notification = notificationOpt.get();
                    notification.setStatus(NotificationStatus.FAILED);
                    notification.setFailureReason(e.getMessage());
                    notification.setRetryCount(notification.getRetryCount() + 1);
                    notificationRepository.save(notification);
                }
            } catch (Exception ex) {
                log.error("Failed to update notification status", ex);
            }
            
            return NotificationResponse.builder()
                    .success(false)
                    .message("Failed to send email: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public NotificationResponse sendTemplatedEmail(Long userId, String recipient, String templateName, 
                                                 Map<String, Object> model, String referenceId, String referenceType) {
        try {
            String processedContent = processTemplate(templateName, model);
            String subject = determineSubject(templateName, model);
            
            EmailRequest emailRequest = EmailRequest.builder()
                    .userId(userId)
                    .recipient(recipient)
                    .subject(subject)
                    .body(processedContent)
                    .templateName(templateName)
                    .templateModel(model)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .build();
                    
            return sendEmail(emailRequest);
            
        } catch (Exception e) {
            log.error("Failed to send templated email", e);
            return NotificationResponse.builder()
                    .success(false)
                    .message("Failed to send templated email: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public EmailTemplate getEmailTemplate(String templateName) {
        return emailTemplateRepository.findByName(templateName)
                .orElseThrow(() -> new RuntimeException("Email template not found: " + templateName));
    }

    @Override
    public String processTemplate(String templateName, Map<String, Object> model) {
        try {
            // First try to get template from database
            Optional<EmailTemplate> dbTemplate = emailTemplateRepository.findByName(templateName);
            
            if (dbTemplate.isPresent()) {
                Context context = new Context();
                context.setVariables(model);
                return templateEngine.process(dbTemplate.get().getContent(), context);
            }
            
            // If not in database, try to load from classpath resources
            String templatePath = "templates/" + templateName;
            ClassPathResource resource = new ClassPathResource(templatePath);
            
            if (resource.exists()) {
                String templateContent = FileCopyUtils.copyToString(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
                
                Context context = new Context();
                context.setVariables(model);
                return templateEngine.process(templateContent, context);
            }
            
            throw new RuntimeException("Template not found: " + templateName);
            
        } catch (IOException e) {
            log.error("Failed to process email template", e);
            throw new RuntimeException("Failed to process email template: " + e.getMessage());
        }
    }
    
    private String determineSubject(String templateName, Map<String, Object> model) {
        // Try to get from database first
        Optional<EmailTemplate> dbTemplate = emailTemplateRepository.findByName(templateName);
        if (dbTemplate.isPresent()) {
            return dbTemplate.get().getSubject();
        }
        
        // Default subjects based on template name
        switch (templateName) {
            case "order-confirmation-template.html":
                return "Order Confirmation - " + model.getOrDefault("orderNumber", "");
            case "payment-confirmation-template.html":
                return "Payment Confirmation - " + model.getOrDefault("orderNumber", "");
            case "order-cancellation-template.html":
                return "Order Cancellation - " + model.getOrDefault("orderNumber", "");
            case "payment-failed-template.html":
                return "Payment Failed - " + model.getOrDefault("orderNumber", "");
            case "shipping-confirmation-template.html":
                return "Your Order Has Shipped - " + model.getOrDefault("orderNumber", "");
            default:
                return "Notification from Our Store";
        }
    }
    
    // Fallback method for circuit breaker
    private NotificationResponse sendEmailFallback(EmailRequest emailRequest, Exception e) {
        log.error("Fallback triggered for email sending", e);
        
        try {
            // Save notification with failed status
            Notification notification = new Notification();
            notification.setUserId(emailRequest.getUserId());
            notification.setRecipient(emailRequest.getRecipient());
            notification.setType(NotificationType.EMAIL);
            notification.setSubject(emailRequest.getSubject());
            notification.setContent(emailRequest.getBody());
            notification.setTemplateName(emailRequest.getTemplateName());
            notification.setReferenceId(emailRequest.getReferenceId());
            notification.setReferenceType(emailRequest.getReferenceType());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason("Circuit breaker open: " + e.getMessage());
            
            Notification savedNotification = notificationRepository.save(notification);
            
            return NotificationResponse.builder()
                    .success(false)
                    .message("Email sending temporarily unavailable")
                    .notificationId(savedNotification.getId())
                    .build();
        } catch (Exception ex) {
            log.error("Failed to save notification in fallback", ex);
            return NotificationResponse.builder()
                    .success(false)
                    .message("Email sending temporarily unavailable")
                    .build();
        }
    }
}
