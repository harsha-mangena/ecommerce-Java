package com.ecommerce.orderservice.service.impl;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.entity.Payment;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        Order order = mapOrderRequestToEntity(orderRequest);
        
        // Generate unique order number
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.CREATED);
        
        // Calculate total amount based on the order items
        BigDecimal totalAmount = orderRequest.getOrderItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        order.setTotalAmount(totalAmount);
        
        Order savedOrder = orderRepository.save(order);
        
        // Publish order created event to Kafka
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                savedOrder.getUserId(),
                savedOrder.getTotalAmount(),
                savedOrder.getOrderItems().stream()
                        .map(item -> new OrderItemDto(
                                item.getProductId(),
                                item.getProductName(),
                                item.getQuantity(),
                                item.getPrice()))
                        .collect(Collectors.toList())
        );
        
        kafkaTemplate.send("order-created", orderCreatedEvent);
        log.info("Order created and event published: {}", savedOrder.getOrderNumber());
        
        return mapEntityToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        return mapEntityToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderDto> getOrdersByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        return createPagedResponse(orderPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderDto> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return createPagedResponse(orderPage);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        
        try {
            OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(newStatus);
            Order updatedOrder = orderRepository.save(order);
            
            // If relevant, publish an event for order status change
            
            return mapEntityToOrderResponse(updatedOrder);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        
        // Only allow cancellation if order is not yet shipped or delivered
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel order with status: " + order.getStatus());
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        // Publish order cancelled event to Kafka
        // kafkaTemplate.send("order-cancelled", new OrderCancelledEvent(order.getId(), order.getOrderNumber()));
        log.info("Order cancelled: {}", order.getOrderNumber());
    }
    
    private Order mapOrderRequestToEntity(OrderRequest orderRequest) {
        Order order = new Order();
        order.setUserId(orderRequest.getUserId());
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setBillingAddress(orderRequest.getBillingAddress());
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        
        // Map order items
        List<OrderItem> orderItems = orderRequest.getOrderItems().stream()
                .map(itemRequest -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductId(itemRequest.getProductId());
                    orderItem.setProductName(itemRequest.getProductName());
                    orderItem.setQuantity(itemRequest.getQuantity());
                    orderItem.setPrice(itemRequest.getPrice());
                    orderItem.setOrder(order);
                    return orderItem;
                })
                .collect(Collectors.toList());
                
        order.setOrderItems(orderItems);
        return order;
    }
    
    private OrderResponse mapEntityToOrderResponse(Order order) {
        List<OrderItemDto> orderItems = order.getOrderItems().stream()
                .map(item -> new OrderItemDto(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice()))
                .collect(Collectors.toList());
                
        PaymentDto paymentDto = null;
        if (order.getPayment() != null) {
            Payment payment = order.getPayment();
            paymentDto = new PaymentDto(
                    payment.getId(),
                    payment.getPaymentMethod(),
                    payment.getAmount(),
                    payment.getTransactionId(),
                    payment.getStatus().toString()
            );
        }
        
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                orderItems,
                order.getTotalAmount(),
                order.getStatus().toString(),
                order.getShippingAddress(),
                order.getBillingAddress(),
                order.getPaymentMethod(),
                paymentDto,
                order.getOrderDate()
        );
    }
    
    private PagedResponse<OrderDto> createPagedResponse(Page<Order> orderPage) {
        List<OrderDto> orderDtos = orderPage.getContent().stream()
                .map(this::mapEntityToOrderDto)
                .collect(Collectors.toList());
                
        return new PagedResponse<>(
                orderDtos,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isLast()
        );
    }
    
    private OrderDto mapEntityToOrderDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getStatus().toString(),
                order.getOrderDate()
        );
    }
}
