package com.ecommerce.productservice.service.impl;

import com.ecommerce.productservice.dto.*;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.ProductAttribute;
import com.ecommerce.productservice.entity.ProductInventory;
import com.ecommerce.productservice.event.InventoryEvent;
import com.ecommerce.productservice.event.ProductEvent;
import com.ecommerce.productservice.exception.BadRequestException;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.ProductInventoryRepository;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductInventoryRepository inventoryRepository;
    private final KafkaTemplate<String, ProductEvent> productKafkaTemplate;
    private final KafkaTemplate<String, InventoryEvent> inventoryKafkaTemplate;
    
    @Value("${kafka.topics.product-created}")
    private String productCreatedTopic;
    
    @Value("${kafka.topics.product-updated}")
    private String productUpdatedTopic;
    
    @Value("${kafka.topics.product-deleted}")
    private String productDeletedTopic;
    
    @Value("${kafka.topics.inventory-update}")
    private String inventoryUpdateTopic;
    
    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        // Validate category
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", productRequest.getCategoryId()));
        
        // Create product
        Product product = Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .category(category)
                .imageUrl(productRequest.getImageUrl())
                .active(true)
                .attributes(new HashSet<>())
                .build();
        
        // Add attributes
        if (productRequest.getAttributes() != null) {
            Set<ProductAttribute> attributes = productRequest.getAttributes().stream()
                    .map(dto -> ProductAttribute.builder()
                            .product(product)
                            .name(dto.getName())
                            .value(dto.getValue())
                            .build())
                    .collect(Collectors.toSet());
            product.setAttributes(attributes);
        }
        
        Product savedProduct = productRepository.save(product);
        
        // Create initial inventory
        ProductInventory inventory = ProductInventory.builder()
                .product(savedProduct)
                .quantity(productRequest.getInitialStock() != null ? productRequest.getInitialStock() : 0)
                .reservedQuantity(0)
                .build();
        ProductInventory savedInventory = inventoryRepository.save(inventory);
        
        // Send product created event
        ProductEvent productEvent = ProductEvent.builder()
                .id(savedProduct.getId())
                .name(savedProduct.getName())
                .description(savedProduct.getDescription())
                .price(savedProduct.getPrice())
                .categoryId(savedProduct.getCategory().getId())
                .imageUrl(savedProduct.getImageUrl())
                .timestamp(LocalDateTime.now())
                .eventType(ProductEvent.ProductEventType.CREATED)
                .build();
        productKafkaTemplate.send(productCreatedTopic, productEvent);
        
        // Send inventory created event
        if (productRequest.getInitialStock() != null && productRequest.getInitialStock() > 0) {
            InventoryEvent inventoryEvent = InventoryEvent.builder()
                    .productId(savedProduct.getId())
                    .quantity(savedInventory.getQuantity())
                    .reservedQuantity(savedInventory.getReservedQuantity())
                    .availableQuantity(savedInventory.getAvailableQuantity())
                    .timestamp(LocalDateTime.now())
                    .eventType(InventoryEvent.InventoryEventType.STOCK_ADDED)
                    .build();
            inventoryKafkaTemplate.send(inventoryUpdateTopic, inventoryEvent);
        }
        
        return mapProductToResponse(savedProduct, savedInventory);
    }
    
    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        // Fetch the product
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        
        // Validate category
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", productRequest.getCategoryId()));
        
        // Update product
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setCategory(category);
        product.setImageUrl(productRequest.getImageUrl());
        
        // Update attributes - remove existing and add new ones
        product.getAttributes().clear();
        if (productRequest.getAttributes() != null) {
            Set<ProductAttribute> attributes = productRequest.getAttributes().stream()
                    .map(dto -> ProductAttribute.builder()
                            .product(product)
                            .name(dto.getName())
                            .value(dto.getValue())
                            .build())
                    .collect(Collectors.toSet());
            product.getAttributes().addAll(attributes);
        }
        
        Product updatedProduct = productRepository.save(product);
        
        // Fetch inventory
        ProductInventory inventory = inventoryRepository.findByProductId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", id));
        
        // Send product updated event
        ProductEvent productEvent = ProductEvent.builder()
                .id(updatedProduct.getId())
                .name(updatedProduct.getName())
                .description(updatedProduct.getDescription())
                .price(updatedProduct.getPrice())
                .categoryId(updatedProduct.getCategory().getId())
                .imageUrl(updatedProduct.getImageUrl())
                .timestamp(LocalDateTime.now())
                .eventType(ProductEvent.ProductEventType.UPDATED)
                .build();
        productKafkaTemplate.send(productUpdatedTopic, productEvent);
        
        return mapProductToResponse(updatedProduct, inventory);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        
        // Check if product is active
        if (!product.isActive()) {
            throw new ResourceNotFoundException("Product", "id", id);
        }
        
        ProductInventory inventory = inventoryRepository.findByProductId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", id));
        
        return mapProductToResponse(product, inventory);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findByActiveTrue(pageable);
        
        List<ProductResponse> content = products.stream()
                .map(product -> {
                    ProductInventory inventory = inventoryRepository.findByProductId(product.getId())
                            .orElse(new ProductInventory());
                    return mapProductToResponse(product, inventory);
                })
                .collect(Collectors.toList());
        
        return new PagedResponse<>(
                content,
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages(),
                products.isLast()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProductsByCategory(Long categoryId, int page, int size) {
        // Validate that category exists
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findByCategoryId(categoryId, pageable);
        
        List<ProductResponse> content = products.stream()
                .map(product -> {
                    ProductInventory inventory = inventoryRepository.findByProductId(product.getId())
                            .orElse(new ProductInventory());
                    return mapProductToResponse(product, inventory);
                })
                .collect(Collectors.toList());
        
        return new PagedResponse<>(
                content,
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages(),
                products.isLast()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(keyword, pageable);
        
        List<ProductResponse> content = products.stream()
                .map(product -> {
                    ProductInventory inventory = inventoryRepository.findByProductId(product.getId())
                            .orElse(new ProductInventory());
                    return mapProductToResponse(product, inventory);
                })
                .collect(Collectors.toList());
        
        return new PagedResponse<>(
                content,
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages(),
                products.isLast()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByAttribute(String attributeName, String attributeValue) {
        List<Product> products = productRepository.findByAttributeAndValue(attributeName, attributeValue);
        
        return products.stream()
                .map(product -> {
                    ProductInventory inventory = inventoryRepository.findByProductId(product.getId())
                            .orElse(new ProductInventory());
                    return mapProductToResponse(product, inventory);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        
        // Soft delete - mark as inactive
        product.setActive(false);
        Product deletedProduct = productRepository.save(product);
        
        // Send product deleted event
        ProductEvent productEvent = ProductEvent.builder()
                .id(deletedProduct.getId())
                .name(deletedProduct.getName())
                .timestamp(LocalDateTime.now())
                .eventType(ProductEvent.ProductEventType.DELETED)
                .build();
        productKafkaTemplate.send(productDeletedTopic, productEvent);
    }
    
    @Override
    @Transactional
    public ProductInventoryDto updateInventory(InventoryRequest inventoryRequest) {
        // Validate product
        Product product = productRepository.findById(inventoryRequest.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", inventoryRequest.getProductId()));
        
        // Get inventory
        ProductInventory inventory = inventoryRepository.findByProductId(inventoryRequest.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", inventoryRequest.getProductId()));
        
        int oldQuantity = inventory.getQuantity();
        InventoryEvent.InventoryEventType eventType;
        
        // Update inventory based on action
        switch (inventoryRequest.getAction().toUpperCase()) {
            case "ADD":
                inventory.setQuantity(inventory.getQuantity() + inventoryRequest.getQuantity());
                eventType = InventoryEvent.InventoryEventType.STOCK_ADDED;
                break;
            case "SUBTRACT":
                if (inventory.getQuantity() < inventoryRequest.getQuantity()) {
                    throw new BadRequestException("Not enough inventory to subtract");
                }
                inventory.setQuantity(inventory.getQuantity() - inventoryRequest.getQuantity());
                eventType = InventoryEvent.InventoryEventType.STOCK_REMOVED;
                break;
            case "SET":
                if (inventoryRequest.getQuantity() < inventory.getReservedQuantity()) {
                    throw new BadRequestException("Cannot set inventory less than reserved quantity");
                }
                inventory.setQuantity(inventoryRequest.getQuantity());
                eventType = inventoryRequest.getQuantity() > oldQuantity 
                        ? InventoryEvent.InventoryEventType.STOCK_ADDED 
                        : InventoryEvent.InventoryEventType.STOCK_REMOVED;
                break;
            default:
                throw new BadRequestException("Invalid inventory action: " + inventoryRequest.getAction());
        }
        
        ProductInventory updatedInventory = inventoryRepository.save(inventory);
        
        // Send inventory update event
        InventoryEvent inventoryEvent = InventoryEvent.builder()
                .productId(updatedInventory.getProduct().getId())
                .quantity(updatedInventory.getQuantity())
                .reservedQuantity(updatedInventory.getReservedQuantity())
                .availableQuantity(updatedInventory.getAvailableQuantity())
                .timestamp(LocalDateTime.now())
                .eventType(eventType)
                .build();
        inventoryKafkaTemplate.send(inventoryUpdateTopic, inventoryEvent);
        
        return mapInventoryToDto(updatedInventory);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductInventoryDto getInventoryByProduct(Long productId) {
        // Validate product
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        // Get inventory
        ProductInventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));
        
        return mapInventoryToDto(inventory);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getNewArrivals() {
        List<Product> products = productRepository.findTop10ByOrderByCreatedAtDesc();
        
        return products.stream()
                .filter(Product::isActive)
                .map(product -> {
                    ProductInventory inventory = inventoryRepository.findByProductId(product.getId())
                            .orElse(new ProductInventory());
                    return mapProductToResponse(product, inventory);
                })
                .collect(Collectors.toList());
    }
    
    // Helper methods
    private ProductResponse mapProductToResponse(Product product, ProductInventory inventory) {
        CategoryDto categoryDto = CategoryDto.builder()
                .id(product.getCategory().getId())
                .name(product.getCategory().getName())
                .description(product.getCategory().getDescription())
                .parentId(product.getCategory().getParent() != null ? product.getCategory().getParent().getId() : null)
                .build();
        
        List<ProductAttributeDto> attributeDtos = product.getAttributes().stream()
                .map(attr -> ProductAttributeDto.builder()
                        .name(attr.getName())
                        .value(attr.getValue())
                        .build())
                .collect(Collectors.toList());
        
        ProductInventoryDto inventoryDto = inventory != null ? mapInventoryToDto(inventory) : null;
        
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(categoryDto)
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .active(product.isActive())
                .attributes(attributeDtos)
                .inventory(inventoryDto)
                .build();
    }
    
    private ProductInventoryDto mapInventoryToDto(ProductInventory inventory) {
        return ProductInventoryDto.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .inStock(inventory.isInStock())
                .build();
    }
}
