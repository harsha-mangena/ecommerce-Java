package com.ecommerce.productservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {
    private Long productId;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private LocalDateTime timestamp;
    private InventoryEventType eventType;
    
    public enum InventoryEventType {
        STOCK_ADDED, STOCK_REMOVED, STOCK_RESERVED, RESERVATION_CONFIRMED, RESERVATION_CANCELLED
    }
}
