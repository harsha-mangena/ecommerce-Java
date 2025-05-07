package com.ecommerce.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    
    private Long userId;
    private String recipient;
    private String subject;
    private String body;
    private String templateName;
    private Map<String, Object> templateModel;
    private String referenceId;
    private String referenceType;
}
