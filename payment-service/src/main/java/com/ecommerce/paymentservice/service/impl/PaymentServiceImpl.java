package com.ecommerce.paymentservice.service.impl;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.RefundRequest;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentMethod;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.event.OrderCreatedEvent;
import com.ecommerce.paymentservice.event.PaymentCompletedEvent;
import com.ecommerce.paymentservice.event.PaymentFailedEvent;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.exception.PaymentProcessingException;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.PaymentGatewayService;
import com.ecommerce.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final KafkaTemplate<String, PaymentCompletedEvent> paymentCompletedKafkaTemplate;
    private final KafkaTemplate<String, PaymentFailedEvent> paymentFailedKafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${kafka.topics.payment-completed}")
    private String paymentCompletedTopic;
    
    @Value("${kafka.topics.payment-failed}")
    private String paymentFailedTopic;
    
    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        log.info("Processing payment for order: {}", paymentRequest.getOrderNumber());
        
        // Check if payment already exists for the order
        paymentRepository.findByOrderId(paymentRequest.getOrderId())
                .ifPresent(existingPayment -> {
                    if (existingPayment.getStatus() == PaymentStatus.COMPLETED) {
                        throw new PaymentProcessingException("Payment already completed for this order");
                    }
                });
        
        // Create a new payment record with PENDING status
        Payment payment = mapToPaymentEntity(paymentRequest);
        payment.setStatus(PaymentStatus.PENDING);
        
        Payment savedPayment = paymentRepository.save(payment);
        
        try {
            // Process payment through payment gateway
            Map<String, Object> gatewayResponse = paymentGatewayService.processPayment(paymentRequest);
            
            boolean isSuccess = (boolean) gatewayResponse.get("success");
            String transactionId = (String) gatewayResponse.get("transactionId");
            
            if (isSuccess) {
                // Update payment record with successful transaction
                savedPayment.setTransactionId(transactionId);
                savedPayment.setStatus(PaymentStatus.COMPLETED);
                savedPayment.setPaymentDetails(objectMapper.writeValueAsString(gatewayResponse));
                
                Payment completedPayment = paymentRepository.save(savedPayment);
                
                // Publish payment completed event
                publishPaymentCompletedEvent(completedPayment);
                
                return mapToPaymentResponse(completedPayment);
            } else {
                // Update payment record with failed transaction
                String errorMessage = (String) gatewayResponse.getOrDefault("errorMessage", "Payment failed");
                savedPayment.setStatus(PaymentStatus.FAILED);
                savedPayment.setErrorMessage(errorMessage);
                savedPayment.setTransactionId(transactionId);
                savedPayment.setPaymentDetails(objectMapper.writeValueAsString(gatewayResponse));
                
                Payment failedPayment = paymentRepository.save(savedPayment);
                
                // Publish payment failed event
                publishPaymentFailedEvent(failedPayment);
                
                throw new PaymentProcessingException(errorMessage);
            }
        } catch (Exception e) {
            // Handle exceptions and update payment status
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setErrorMessage(e.getMessage());
            Payment failedPayment = paymentRepository.save(savedPayment);
            
            // Publish payment failed event
            publishPaymentFailedEvent(failedPayment);
            
            if (e instanceof PaymentProcessingException) {
                throw (PaymentProcessingException) e;
            }
            throw new PaymentProcessingException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));
                
        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order id: " + orderId));
                
        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderNumber(String orderNumber) {
        Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order number: " + orderNumber));
                
        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentResponse processRefund(RefundRequest refundRequest) {
        log.info("Processing refund for payment ID: {} and order: {}", 
                refundRequest.getPaymentId(), refundRequest.getOrderNumber());
        
        // Get the original payment
        Payment payment = paymentRepository.findById(refundRequest.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + refundRequest.getPaymentId()));
        
        // Verify payment status is COMPLETED
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentProcessingException("Cannot refund a payment that is not completed. Current status: " + payment.getStatus());
        }
        
        // Process refund through payment gateway
        Map<String, Object> refundResponse = paymentGatewayService.processRefund(refundRequest, payment.getTransactionId());
        
        boolean isSuccess = (boolean) refundResponse.get("success");
        if (isSuccess) {
            // Update payment status to REFUNDED
            payment.setStatus(PaymentStatus.REFUNDED);
            try {
                payment.setPaymentDetails(objectMapper.writeValueAsString(refundResponse));
            } catch (Exception e) {
                log.error("Error serializing refund response", e);
            }
            
            Payment refundedPayment = paymentRepository.save(payment);
            
            // Could publish a refund completed event here
            
            return mapToPaymentResponse(refundedPayment);
        } else {
            String errorMessage = (String) refundResponse.getOrDefault("errorMessage", "Refund failed");
            throw new PaymentProcessingException(errorMessage);
        }
    }

    @Override
    @KafkaListener(topics = "${kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(OrderCreatedEvent orderCreatedEvent, Acknowledgment acknowledgment) {
        log.info("Received order created event for order: {}", orderCreatedEvent.getOrderNumber());
        
        try {
            // Check if a payment already exists for this order
            boolean paymentExists = paymentRepository.findByOrderId(orderCreatedEvent.getOrderId()).isPresent();
            
            if (!paymentExists) {
                // Create a pending payment entry
                Payment payment = new Payment();
                payment.setOrderId(orderCreatedEvent.getOrderId());
                payment.setOrderNumber(orderCreatedEvent.getOrderNumber());
                payment.setUserId(orderCreatedEvent.getUserId());
                payment.setAmount(orderCreatedEvent.getTotalAmount());
                payment.setStatus(PaymentStatus.PENDING);
                payment.setPaymentMethod(PaymentMethod.CREDIT_CARD); // Default, will be updated when actual payment is made
                
                paymentRepository.save(payment);
                log.info("Created pending payment for order: {}", orderCreatedEvent.getOrderNumber());
            } else {
                log.info("Payment already exists for order: {}", orderCreatedEvent.getOrderNumber());
            }
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order created event", e);
            // Do not acknowledge, message will be redelivered
        }
    }
    
    private Payment mapToPaymentEntity(PaymentRequest paymentRequest) {
        Payment payment = new Payment();
        payment.setOrderId(paymentRequest.getOrderId());
        payment.setOrderNumber(paymentRequest.getOrderNumber());
        payment.setUserId(paymentRequest.getUserId());
        payment.setAmount(paymentRequest.getAmount());
        payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        
        return payment;
    }
    
    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .orderNumber(payment.getOrderNumber())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().toString())
                .status(payment.getStatus().toString())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
    
    private void publishPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getOrderNumber(),
                payment.getAmount(),
                payment.getStatus().toString(),
                payment.getTransactionId()
        );
        
        paymentCompletedKafkaTemplate.send(paymentCompletedTopic, payment.getOrderNumber(), event);
        log.info("Published payment completed event for order: {}", payment.getOrderNumber());
    }
    
    private void publishPaymentFailedEvent(Payment payment) {
        PaymentFailedEvent event = new PaymentFailedEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getOrderNumber(),
                payment.getAmount(),
                payment.getErrorMessage()
        );
        
        paymentFailedKafkaTemplate.send(paymentFailedTopic, payment.getOrderNumber(), event);
        log.info("Published payment failed event for order: {}", payment.getOrderNumber());
    }
}
