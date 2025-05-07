package com.ecommerce.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private Long productId;
    
    @Column(nullable = false)
    private String productName;
    
    @Column(nullable = false)
    private String sku;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "reserved_quantity")
    private Integer reservedQuantity = 0;
    
    @Column(name = "reorder_threshold")
    private Integer reorderThreshold;
    
    @Column(name = "is_in_stock")
    private Boolean isInStock = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updateStockStatus();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateStockStatus();
    }
    
    private void updateStockStatus() {
        isInStock = (quantity > reservedQuantity);
    }
    
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
    
    public void increaseQuantity(Integer amount) {
        if (amount > 0) {
            quantity += amount;
            updateStockStatus();
        }
    }
    
    public void decreaseQuantity(Integer amount) {
        if (amount > 0 && quantity >= amount) {
            quantity -= amount;
            updateStockStatus();
        }
    }
    
    public void reserveQuantity(Integer amount) {
        if (amount > 0 && getAvailableQuantity() >= amount) {
            reservedQuantity += amount;
            updateStockStatus();
        }
    }
    
    public void releaseReservedQuantity(Integer amount) {
        if (amount > 0 && reservedQuantity >= amount) {
            reservedQuantity -= amount;
            updateStockStatus();
        }
    }
    
    public void confirmReservation(Integer amount) {
        if (amount > 0 && reservedQuantity >= amount) {
            reservedQuantity -= amount;
            quantity -= amount;
            updateStockStatus();
        }
    }
}
