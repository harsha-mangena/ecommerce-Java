package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.dto.OrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdatedEvent {
    private String eventId;
    private String eventType = "ORDER_UPDATED";
    private LocalDateTime timestamp;
    private OrderDto order;
}
