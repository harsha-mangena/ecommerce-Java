package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {
    
    Optional<ProductInventory> findByProductId(Long productId);
    
    @Query("SELECT pi FROM ProductInventory pi WHERE pi.quantity > 0 AND pi.product.active = true")
    List<ProductInventory> findAllInStock();
    
    @Query("SELECT pi FROM ProductInventory pi WHERE pi.quantity <= :threshold AND pi.product.active = true")
    List<ProductInventory> findAllLowStock(@Param("threshold") int threshold);
}
