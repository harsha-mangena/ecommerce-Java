package com.ecommerce.inventoryservice.service.impl;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.entity.InventoryTransaction;
import com.ecommerce.inventoryservice.event.InventoryUpdatedEvent;
import com.ecommerce.inventoryservice.event.OrderCancelledEvent;
import com.ecommerce.inventoryservice.event.OrderCreatedEvent;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.repository.InventoryTransactionRepository;
import com.ecommerce.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final KafkaTemplate<String, InventoryUpdatedEvent> kafkaTemplate;
    
    @Override
    @Transactional
    public InventoryResponse createInventoryItem(InventoryRequest request) {
        log.info("Creating inventory item for product: {}", request.getProductId());
        
        // Check if inventory already exists for this product
        if (inventoryRepository.findByProductId(request.getProductId()).isPresent()) {
            return InventoryResponse.builder()
                    .success(false)
                    .message("Inventory already exists for this product")
                    .build();
        }
        
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProductId(request.getProductId());
        inventoryItem.setProductName(request.getProductName());
        inventoryItem.setSku(request.getSku());
        inventoryItem.setQuantity(request.getQuantity());
        inventoryItem.setReorderThreshold(request.getReorderThreshold());
        
        InventoryItem savedItem = inventoryRepository.save(inventoryItem);
        
        // Record the transaction
        recordTransaction(
                savedItem.getProductId(),
                InventoryTransaction.TransactionType.STOCK_ADDITION,
                savedItem.getQuantity(),
                0,
                savedItem.getQuantity(),
                "INITIAL_STOCK",
                "Initial inventory creation"
        );
        
        // Publish inventory updated event
        publishInventoryUpdatedEvent(savedItem);
        
        return InventoryResponse.builder()
                .success(true)
                .message("Inventory created successfully")
                .data(mapToDto(savedItem))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId) {
        InventoryItem inventoryItem = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + productId));
        
        return InventoryResponse.builder()
                .success(true)
                .message("Inventory found")
                .data(mapToDto(inventoryItem))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryBySku(String sku) {
        InventoryItem inventoryItem = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for SKU: " + sku));
        
        return InventoryResponse.builder()
                .success(true)
                .message("Inventory found")
                .data(mapToDto(inventoryItem))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InventoryItemDto> getAllInventory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("productName").ascending());
        Page<InventoryItem> inventoryPage = inventoryRepository.findAll(pageable);
        
        List<InventoryItemDto> content = inventoryPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
                
        return new PagedResponse<>(
                content,
                inventoryPage.getNumber(),
                inventoryPage.getSize(),
                inventoryPage.getTotalElements(),
                inventoryPage.getTotalPages(),
                inventoryPage.isLast()
        );
    }

    @Override
    @Transactional
    public InventoryResponse updateInventory(Long productId, InventoryRequest request) {
        InventoryItem inventoryItem = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + productId));
        
        // Update only the fields that can be changed
        inventoryItem.setProductName(request.getProductName());
        inventoryItem.setSku(request.getSku());
        inventoryItem.setReorderThreshold(request.getReorderThreshold());
        
        // If quantity is being updated, record the transaction
        if (!inventoryItem.getQuantity().equals(request.getQuantity())) {
            int oldQuantity = inventoryItem.getQuantity();
            inventoryItem.setQuantity(request.getQuantity());
            
            InventoryTransaction.TransactionType type = request.getQuantity() > oldQuantity 
                    ? InventoryTransaction.TransactionType.STOCK_ADDITION 
                    : InventoryTransaction.TransactionType.STOCK_REMOVAL;
            
            recordTransaction(
                    productId,
                    type,
                    Math.abs(request.getQuantity() - oldQuantity),
                    oldQuantity,
                    request.getQuantity(),
                    "MANUAL_UPDATE",
                    "Manual inventory update"
            );
        }
        
        InventoryItem updatedItem = inventoryRepository.save(inventoryItem);
        
        // Publish inventory updated event
        publishInventoryUpdatedEvent(updatedItem);
        
        return InventoryResponse.builder()
                .success(true)
                .message("Inventory updated successfully")
                .data(mapToDto(updatedItem))
                .build();
    }

    @Override
    @Transactional
    public InventoryResponse addStock(StockUpdateRequest request) {
        InventoryItem inventoryItem = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + request.getProductId()));
        
        int oldQuantity = inventoryItem.getQuantity();
        inventoryItem.increaseQuantity(request.getQuantity());
        
        InventoryItem updatedItem = inventoryRepository.save(inventoryItem);
        
        // Record the transaction
        recordTransaction(
                request.getProductId(),
                InventoryTransaction.TransactionType.STOCK_ADDITION,
                request.getQuantity(),
                oldQuantity,
                updatedItem.getQuantity(),
                request.getReferenceId(),
                request.getNotes()
        );
        
        // Publish inventory updated event
        publishInventoryUpdatedEvent(updatedItem);
        
        return InventoryResponse.builder()
                .success(true)
                .message("Stock added successfully")
                .data(mapToDto(updatedItem))
                .build();
    }

    @Override
    @Transactional
    public InventoryResponse removeStock(StockUpdateRequest request) {
        InventoryItem inventoryItem = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + request.getProductId()));
        
        if (inventoryItem.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock available for product: " + inventoryItem.getProductName());
        }
        
        int oldQuantity = inventoryItem.getQuantity();
        inventoryItem.decreaseQuantity(request.getQuantity());
        
        InventoryItem updatedItem = inventoryRepository.save(inventoryItem);
        
        // Record the transaction
        recordTransaction(
                request.getProductId(),
                InventoryTransaction.TransactionType.STOCK_REMOVAL,
                request.getQuantity(),
                oldQuantity,
                updatedItem.getQuantity(),
                request.getReferenceId(),
                request.getNotes()
        );
        
        // Publish inventory updated event
        publishInventoryUpdatedEvent(updatedItem);
        
        return InventoryResponse.builder()
                .success(true)
                .message("Stock removed successfully")
                .data(mapToDto(updatedItem))
                .build();
    }

    @Override
    @Transactional
    public InventoryResponse reserveStock(ReservationRequest request) {
        InventoryItem inventoryItem = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + request.getProductId()));
        
        if (inventoryItem.getAvailableQuantity() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock available for product: " + inventoryItem.getProductName());
        }
        
        int oldReservedQuantity = inventoryItem.getReservedQuantity();
        inventoryItem.reserveQuantity(request.getQuantity());
        
        InventoryItem updatedItem = inventoryRepository.save(inventoryItem);
        
        // Record the transaction
        recordTransaction(
                request.getProductId(),
                InventoryTransaction.TransactionType.RESERVATION,
                request.getQuantity(),
                oldReservedQuantity,
                updatedItem.getReservedQuantity(),
                request.getOrderId(),
                "Stock reserved for order: " + request.getOrderId()
        );
        
        // Publish inventory updated event
        publishInventoryUpdatedEvent(updatedItem);
        
        return InventoryResponse.builder()
                .success(true)
                .message("Stock reserved successfully")
                .data(mapToDto(updatedItem))
                .build();
    }

    @Override
    @Transactional
    public InventoryResponse releaseStock(ReservationRequest request) {
        InventoryItem inventoryItem = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + request.getProductId()));
        
        if (inventoryItem.getReservedQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException("Cannot release more stock than reserved for product: " + inventoryItem.getProductName());
        }
        
        int oldReservedQuantity = inventoryItem.getReservedQuantity();
        inventoryItem.releaseReservedQuantity(request.getQuantity());
        
        InventoryItem updatedItem = inventoryRepository.save(inventoryItem);
        
        // Record the transaction
        recordTransaction(
                request.getProductId(),
                InventoryTransaction.TransactionType.RESERVATION_RELEASE,
                request.getQuantity(),
                oldReservedQuantity,
                updatedItem.getReservedQuantity(),
                request.getOrderId(),
                "Stock released for order: " + request.getOrderId()
        );
        
        // Publish inventory updated event
        publishInventoryUpdatedEvent(updatedItem);
        
        return InventoryResponse.builder()
                .success(true)
                .message("Stock released successfully")
                .data(mapToDto(updatedItem))
                .build();
    }

    @Override
    @Transactional
    public InventoryResponse confirmStockReservation(ReservationRequest request) {
        InventoryItem inventoryItem = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + request.getProductId()));
        
        if (inventoryItem.getReservedQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException("Cannot confirm more stock than reserved for product: " + inventoryItem.getProductName());
        }
        
        int oldReservedQuantity = inventoryItem.getReservedQuantity();
        int oldQuantity = inventoryItem.getQuantity();
        inventoryItem.confirmReservation(request.getQuantity());
        
        InventoryItem updatedItem = inventoryRepository.save(inventoryItem);
        
        // Record the transaction
        recordTransaction(
                request.getProductId(),
                InventoryTransaction.TransactionType.RESERVATION_CONFIRMATION,
                request.getQuantity(),
                oldQuantity,
                updatedItem.getQuantity(),
                request.getOrderId(),
                "Stock confirmation for order: " + request.getOrderId()
        );
        
        // Publish inventory updated event
        publishInventoryUpdatedEvent(updatedItem);
        
        return InventoryResponse.builder()
                .success(true)
                .message("Stock reservation confirmed successfully")
                .data(mapToDto(updatedItem))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InventoryItemDto> getLowStockItems(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InventoryItem> itemsPage = inventoryRepository.findAll(pageable);
        
        List<InventoryItemDto> lowStockItems = itemsPage.getContent().stream()
                .filter(item -> item.getQuantity() <= item.getReorderThreshold())
                .map(this::mapToDto)
                .collect(Collectors.toList());
                
        return new PagedResponse<>(
                lowStockItems,
                itemsPage.getNumber(),
                itemsPage.getSize(),
                lowStockItems.size(),
                itemsPage.getTotalPages(),
                itemsPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemDto> getOutOfStockItems() {
        List<InventoryItem> outOfStockItems = inventoryRepository.findByIsInStockFalse();
        
        return outOfStockItems.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @KafkaListener(topics = "${kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreatedEvent(OrderCreatedEvent event, Acknowledgment acknowledgment) {
        log.info("Received order created event: {}", event.getOrderNumber());
        
        try {
            // Reserve stock for each order item
            for (OrderCreatedEvent.OrderItemDto orderItem : event.getOrderItems()) {
                ReservationRequest reservationRequest = new ReservationRequest();
                reservationRequest.setProductId(orderItem.getProductId());
                reservationRequest.setQuantity(orderItem.getQuantity());
                reservationRequest.setOrderId(event.getOrderNumber());
                
                try {
                    reserveStock(reservationRequest);
                    log.info("Reserved {} units of product {} for order {}", 
                             orderItem.getQuantity(), orderItem.getProductId(), event.getOrderNumber());
                } catch (Exception e) {
                    log.error("Failed to reserve stock for product {} in order {}: {}", 
                              orderItem.getProductId(), event.getOrderNumber(), e.getMessage());
                    // In a real system, we'd need to handle this failure better, 
                    // perhaps by releasing already reserved items and sending a failure event
                }
            }
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order created event", e);
            // Don't acknowledge to retry
        }
    }

    @Override
    @KafkaListener(topics = "${kafka.topics.order-cancelled}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCancelledEvent(OrderCancelledEvent event, Acknowledgment acknowledgment) {
        log.info("Received order cancelled event: {}", event.getOrderNumber());
        
        try {
            // Find all reservations for this order
            List<InventoryTransaction> reservations = transactionRepository.findByReferenceId(event.getOrderNumber())
                    .stream()
                    .filter(tx -> tx.getType() == InventoryTransaction.TransactionType.RESERVATION)
                    .collect(Collectors.toList());
            
            // Release each reservation
            for (InventoryTransaction reservation : reservations) {
                ReservationRequest releaseRequest = new ReservationRequest();
                releaseRequest.setProductId(reservation.getProductId());
                releaseRequest.setQuantity(reservation.getQuantity());
                releaseRequest.setOrderId(event.getOrderNumber());
                
                try {
                    releaseStock(releaseRequest);
                    log.info("Released {} units of product {} for cancelled order {}", 
                             reservation.getQuantity(), reservation.getProductId(), event.getOrderNumber());
                } catch (Exception e) {
                    log.error("Failed to release stock for product {} in cancelled order {}: {}", 
                              reservation.getProductId(), event.getOrderNumber(), e.getMessage());
                }
            }
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order cancelled event", e);
            // Don't acknowledge to retry
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InventoryTransaction> getTransactionHistory(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<InventoryTransaction> transactionsPage = transactionRepository.findByProductId(productId, pageable);
        
        return new PagedResponse<>(
                transactionsPage.getContent(),
                transactionsPage.getNumber(),
                transactionsPage.getSize(),
                transactionsPage.getTotalElements(),
                transactionsPage.getTotalPages(),
                transactionsPage.isLast()
        );
    }
    
    private InventoryItemDto mapToDto(InventoryItem item) {
        return InventoryItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .sku(item.getSku())
                .quantity(item.getQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .availableQuantity(item.getAvailableQuantity())
                .reorderThreshold(item.getReorderThreshold())
                .isInStock(item.getIsInStock())
                .build();
    }
    
    private void recordTransaction(
            Long productId,
            InventoryTransaction.TransactionType type,
            Integer quantity,
            Integer previousQuantity,
            Integer newQuantity,
            String referenceId,
            String notes) {
        
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductId(productId);
        transaction.setType(type);
        transaction.setQuantity(quantity);
        transaction.setPreviousQuantity(previousQuantity);
        transaction.setNewQuantity(newQuantity);
        transaction.setReferenceId(referenceId);
        
        if (type == InventoryTransaction.TransactionType.RESERVATION || 
            type == InventoryTransaction.TransactionType.RESERVATION_RELEASE ||
            type == InventoryTransaction.TransactionType.RESERVATION_CONFIRMATION) {
            transaction.setReferenceType("ORDER");
        } else {
            transaction.setReferenceType(referenceId);
        }
        
        transaction.setNotes(notes);
        
        transactionRepository.save(transaction);
    }
    
    private void publishInventoryUpdatedEvent(InventoryItem item) {
        InventoryUpdatedEvent event = new InventoryUpdatedEvent(
                item.getProductId(),
                item.getProductName(),
                item.getSku(),
                item.getQuantity(),
                item.getReservedQuantity(),
                item.getAvailableQuantity(),
                item.getIsInStock()
        );
        
        kafkaTemplate.send("inventory-updated", item.getProductId().toString(), event);
        log.info("Published inventory updated event for product: {}", item.getProductId());
    }
}
