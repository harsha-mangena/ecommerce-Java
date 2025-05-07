package com.ecommerce.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private Long paymentId;
    private String transactionId;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private String orderNumber;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime paymentDate;
}
