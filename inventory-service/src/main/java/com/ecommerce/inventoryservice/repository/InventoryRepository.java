package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    
    Optional<InventoryItem> findByProductId(Long productId);
    
    Optional<InventoryItem> findBySku(String sku);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.quantity <= i.reorderThreshold")
    List<InventoryItem> findItemsToReorder();
    
    List<InventoryItem> findByIsInStockTrue();
    
    List<InventoryItem> findByIsInStockFalse();
}
