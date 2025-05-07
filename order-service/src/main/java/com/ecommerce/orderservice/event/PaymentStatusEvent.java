package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusEvent {
    private String eventId;
    private String eventType = "PAYMENT_STATUS";
    private LocalDateTime timestamp;
    private String orderId;
    private String paymentId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String transactionId;
}
