package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.EmailRequest;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.entity.EmailTemplate;

import java.util.Map;

public interface EmailService {
    
    NotificationResponse sendEmail(EmailRequest emailRequest);
    
    NotificationResponse sendTemplatedEmail(Long userId, String recipient, String templateName, Map<String, Object> model, String referenceId, String referenceType);
    
    EmailTemplate getEmailTemplate(String templateName);
    
    String processTemplate(String templateName, Map<String, Object> model);
}
