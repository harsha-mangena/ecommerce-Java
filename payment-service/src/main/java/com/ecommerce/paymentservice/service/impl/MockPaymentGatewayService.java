package com.ecommerce.paymentservice.service.impl;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.RefundRequest;
import com.ecommerce.paymentservice.service.PaymentGatewayService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class MockPaymentGatewayService implements PaymentGatewayService {

    @Value("${payment.gateway.api-key}")
    private String apiKey;
    
    @Value("${payment.gateway.secret-key}")
    private String secretKey;
    
    @Value("${payment.gateway.base-url}")
    private String baseUrl;
    
    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.029"); // 2.9%
    private static final BigDecimal FIXED_FEE = new BigDecimal("0.30"); // $0.30
    
    @Override
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentGateway")
    public Map<String, Object> processPayment(PaymentRequest paymentRequest) {
        log.info("Processing payment for order: {}", paymentRequest.getOrderNumber());
        
        // In a real implementation, this would make an HTTP call to the payment gateway API
        // For demo purposes, we'll simulate a payment gateway response
        
        // Simulate some processing delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate payment success/failure based on card number if provided
        boolean isSuccess = true;
        String errorMessage = null;
        
        if (paymentRequest.getCardNumber() != null) {
            // If card number ends with "0000", simulate a failed payment
            if (paymentRequest.getCardNumber().endsWith("0000")) {
                isSuccess = false;
                errorMessage = "Card declined: insufficient funds";
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", isSuccess);
        response.put("transactionId", UUID.randomUUID().toString());
        response.put("amount", paymentRequest.getAmount());
        response.put("currency", "USD");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("orderId", paymentRequest.getOrderId());
        response.put("orderNumber", paymentRequest.getOrderNumber());
        response.put("paymentMethod", paymentRequest.getPaymentMethod().toString());
        
        if (!isSuccess) {
            response.put("errorCode", "INSUFFICIENT_FUNDS");
            response.put("errorMessage", errorMessage);
        }
        
        return response;
    }
    
    @Override
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "processRefundFallback")
    @Retry(name = "paymentGateway")
    public Map<String, Object> processRefund(RefundRequest refundRequest, String transactionId) {
        log.info("Processing refund for order: {}", refundRequest.getOrderNumber());
        
        // In a real implementation, this would make an HTTP call to the payment gateway API
        // For demo purposes, we'll simulate a refund gateway response
        
        // Simulate some processing delay
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        boolean isSuccess = true;
        String errorMessage = null;
        
        // Randomly fail 5% of refunds to simulate errors
        if (Math.random() < 0.05) {
            isSuccess = false;
            errorMessage = "Refund failed: payment gateway error";
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", isSuccess);
        response.put("refundId", UUID.randomUUID().toString());
        response.put("originalTransactionId", transactionId);
        response.put("amount", refundRequest.getAmount());
        response.put("currency", "USD");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("orderNumber", refundRequest.getOrderNumber());
        
        if (!isSuccess) {
            response.put("errorCode", "GATEWAY_ERROR");
            response.put("errorMessage", errorMessage);
        }
        
        return response;
    }
    
    @Override
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "verifyPaymentFallback")
    @Retry(name = "paymentGateway")
    public Map<String, Object> verifyPayment(String transactionId) {
        log.info("Verifying payment for transaction: {}", transactionId);
        
        // In a real implementation, this would make an HTTP call to the payment gateway API
        // For demo purposes, we'll simulate a payment verification response
        
        boolean isSuccess = true;
        
        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", transactionId);
        response.put("verified", isSuccess);
        response.put("status", "COMPLETED");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return response;
    }
    
    @Override
    public BigDecimal calculateFees(BigDecimal amount) {
        // Calculate fee based on percentage + fixed amount
        return amount.multiply(FEE_PERCENTAGE).add(FIXED_FEE).setScale(2, RoundingMode.HALF_UP);
    }
    
    // Circuit breaker fallback methods
    
    private Map<String, Object> processPaymentFallback(PaymentRequest paymentRequest, Exception e) {
        log.error("Payment gateway service is down. Using fallback for order: {}", 
                  paymentRequest.getOrderNumber(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("transactionId", "fallback-" + UUID.randomUUID().toString());
        response.put("amount", paymentRequest.getAmount());
        response.put("currency", "USD");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("orderId", paymentRequest.getOrderId());
        response.put("orderNumber", paymentRequest.getOrderNumber());
        response.put("errorCode", "SERVICE_UNAVAILABLE");
        response.put("errorMessage", "Payment service is currently unavailable. Please try again later.");
        
        return response;
    }
    
    private Map<String, Object> processRefundFallback(RefundRequest refundRequest, String transactionId, Exception e) {
        log.error("Payment gateway service is down. Using fallback for refund on order: {}", 
                  refundRequest.getOrderNumber(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("refundId", "fallback-" + UUID.randomUUID().toString());
        response.put("originalTransactionId", transactionId);
        response.put("amount", refundRequest.getAmount());
        response.put("currency", "USD");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("orderNumber", refundRequest.getOrderNumber());
        response.put("errorCode", "SERVICE_UNAVAILABLE");
        response.put("errorMessage", "Refund service is currently unavailable. Please try again later.");
        
        return response;
    }
    
    private Map<String, Object> verifyPaymentFallback(String transactionId, Exception e) {
        log.error("Payment gateway service is down. Using fallback for verification of transaction: {}", 
                  transactionId, e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", transactionId);
        response.put("verified", false);
        response.put("status", "UNKNOWN");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("errorCode", "SERVICE_UNAVAILABLE");
        response.put("errorMessage", "Verification service is currently unavailable. Please try again later.");
        
        return response;
    }
}
