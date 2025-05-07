package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderDto;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.PagedResponse;

public interface OrderService {
    
    OrderResponse createOrder(OrderRequest orderRequest);
    
    OrderResponse getOrderById(Long id);
    
    PagedResponse<OrderDto> getOrdersByUserId(Long userId, int page, int size);
    
    PagedResponse<OrderDto> getAllOrders(int page, int size);
    
    OrderResponse updateOrderStatus(Long id, String status);
    
    void cancelOrder(Long id);
}
