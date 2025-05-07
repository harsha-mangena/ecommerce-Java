package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.RefundRequest;
import com.ecommerce.paymentservice.event.OrderCreatedEvent;

import java.util.List;

public interface PaymentService {
    
    PaymentResponse processPayment(PaymentRequest paymentRequest);
    
    PaymentResponse getPaymentById(Long paymentId);
    
    PaymentResponse getPaymentByOrderId(Long orderId);
    
    PaymentResponse getPaymentByOrderNumber(String orderNumber);
    
    List<PaymentResponse> getPaymentsByUserId(Long userId);
    
    PaymentResponse processRefund(RefundRequest refundRequest);
    
    void handleOrderCreated(OrderCreatedEvent orderCreatedEvent);
}
