package com.ecommerce.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotBlank(message = "Product name is required")
    private String productName;
    
    @NotBlank(message = "SKU is required")
    private String sku;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be a positive number")
    private Integer quantity;
    
    @Min(value = 0, message = "Reorder threshold must be a positive number")
    private Integer reorderThreshold;
}
