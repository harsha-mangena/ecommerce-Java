package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    
    List<InventoryTransaction> findByProductId(Long productId);
    
    List<InventoryTransaction> findByReferenceId(String referenceId);
    
    List<InventoryTransaction> findByReferenceType(String referenceType);
    
    Page<InventoryTransaction> findByProductId(Long productId, Pageable pageable);
    
    List<InventoryTransaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<InventoryTransaction> findByType(InventoryTransaction.TransactionType type);
}
