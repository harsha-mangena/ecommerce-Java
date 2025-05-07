package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.InventoryTransaction;
import com.ecommerce.inventoryservice.event.OrderCreatedEvent;
import com.ecommerce.inventoryservice.event.OrderCancelledEvent;

import java.util.List;

public interface InventoryService {
    
    InventoryResponse createInventoryItem(InventoryRequest request);
    
    InventoryResponse getInventoryByProductId(Long productId);
    
    InventoryResponse getInventoryBySku(String sku);
    
    PagedResponse<InventoryItemDto> getAllInventory(int page, int size);
    
    InventoryResponse updateInventory(Long productId, InventoryRequest request);
    
    InventoryResponse addStock(StockUpdateRequest request);
    
    InventoryResponse removeStock(StockUpdateRequest request);
    
    InventoryResponse reserveStock(ReservationRequest request);
    
    InventoryResponse releaseStock(ReservationRequest request);
    
    InventoryResponse confirmStockReservation(ReservationRequest request);
    
    PagedResponse<InventoryItemDto> getLowStockItems(int page, int size);
    
    List<InventoryItemDto> getOutOfStockItems();
    
    void handleOrderCreatedEvent(OrderCreatedEvent event);
    
    void handleOrderCancelledEvent(OrderCancelledEvent event);
    
    PagedResponse<InventoryTransaction> getTransactionHistory(Long productId, int page, int size);
}
