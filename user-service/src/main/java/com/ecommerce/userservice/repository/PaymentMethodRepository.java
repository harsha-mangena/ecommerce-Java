package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    
    List<PaymentMethod> findByUserId(Long userId);
    
    Optional<PaymentMethod> findByUserIdAndIsDefaultTrue(Long userId);
}
