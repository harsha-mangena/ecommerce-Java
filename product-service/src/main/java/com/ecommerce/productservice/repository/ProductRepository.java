package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Page<Product> findByActiveTrue(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId")
    Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT p FROM Product p JOIN p.attributes a WHERE p.active = true AND LOWER(a.name) = LOWER(:attributeName) AND LOWER(a.value) = LOWER(:attributeValue)")
    List<Product> findByAttributeAndValue(@Param("attributeName") String attributeName, @Param("attributeValue") String attributeValue);
    
    List<Product> findTop10ByOrderByCreatedAtDesc();
}
