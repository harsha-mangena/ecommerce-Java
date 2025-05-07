package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.RefundRequest;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGatewayService {

    /**
     * Process a payment through the payment gateway
     * 
     * @param paymentRequest the payment request containing all the necessary info to process payment
     * @return a map containing the transaction details from the payment gateway
     */
    Map<String, Object> processPayment(PaymentRequest paymentRequest);
    
    /**
     * Process a refund through the payment gateway
     * 
     * @param refundRequest the refund request containing all the necessary info to process refund
     * @param transactionId the original transaction ID to be refunded
     * @return a map containing the refund details from the payment gateway
     */
    Map<String, Object> processRefund(RefundRequest refundRequest, String transactionId);
    
    /**
     * Verify the status of a payment transaction
     * 
     * @param transactionId the transaction ID to verify
     * @return a map containing the transaction status details
     */
    Map<String, Object> verifyPayment(String transactionId);
    
    /**
     * Calculate payment fees for the given amount
     * 
     * @param amount the payment amount
     * @return the calculated fee
     */
    BigDecimal calculateFees(BigDecimal amount);
}
