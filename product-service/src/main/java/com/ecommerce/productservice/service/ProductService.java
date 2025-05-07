package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    
    ProductResponse createProduct(ProductRequest productRequest);
    
    ProductResponse updateProduct(Long id, ProductRequest productRequest);
    
    ProductResponse getProductById(Long id);
    
    PagedResponse<ProductResponse> getAllProducts(int page, int size);
    
    PagedResponse<ProductResponse> getProductsByCategory(Long categoryId, int page, int size);
    
    PagedResponse<ProductResponse> searchProducts(String keyword, int page, int size);
    
    List<ProductResponse> getProductsByAttribute(String attributeName, String attributeValue);
    
    void deleteProduct(Long id);
    
    // Inventory related methods
    ProductInventoryDto updateInventory(InventoryRequest inventoryRequest);
    
    ProductInventoryDto getInventoryByProduct(Long productId);
    
    List<ProductResponse> getNewArrivals();
}
