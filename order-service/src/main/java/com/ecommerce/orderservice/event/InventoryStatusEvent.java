package com.ecommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStatusEvent {
    private String eventId;
    private String eventType = "INVENTORY_STATUS";
    private LocalDateTime timestamp;
    private Long orderId;
    private boolean isInStock;
    private List<InventoryItem> unavailableItems;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItem {
        private Long productId;
        private String productName;
        private Integer requestedQuantity;
        private Integer availableQuantity;
    }
}
