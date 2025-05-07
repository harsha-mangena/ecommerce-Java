package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.InventoryTransaction;
import com.ecommerce.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    
    @PostMapping
    public ResponseEntity<InventoryResponse> createInventoryItem(@Valid @RequestBody InventoryRequest request) {
        return new ResponseEntity<>(inventoryService.createInventoryItem(request), HttpStatus.CREATED);
    }
    
    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }
    
    @GetMapping("/sku/{sku}")
    public ResponseEntity<InventoryResponse> getInventoryBySku(@PathVariable String sku) {
        return ResponseEntity.ok(inventoryService.getInventoryBySku(sku));
    }
    
    @GetMapping
    public ResponseEntity<PagedResponse<InventoryItemDto>> getAllInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(inventoryService.getAllInventory(page, size));
    }
    
    @PutMapping("/{productId}")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.updateInventory(productId, request));
    }
    
    @PostMapping("/add-stock")
    public ResponseEntity<InventoryResponse> addStock(@Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(inventoryService.addStock(request));
    }
    
    @PostMapping("/remove-stock")
    public ResponseEntity<InventoryResponse> removeStock(@Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(inventoryService.removeStock(request));
    }
    
    @PostMapping("/reserve")
    public ResponseEntity<InventoryResponse> reserveStock(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(inventoryService.reserveStock(request));
    }
    
    @PostMapping("/release")
    public ResponseEntity<InventoryResponse> releaseStock(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(inventoryService.releaseStock(request));
    }
    
    @PostMapping("/confirm")
    public ResponseEntity<InventoryResponse> confirmStockReservation(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(inventoryService.confirmStockReservation(request));
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<PagedResponse<InventoryItemDto>> getLowStockItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(inventoryService.getLowStockItems(page, size));
    }
    
    @GetMapping("/out-of-stock")
    public ResponseEntity<List<InventoryItemDto>> getOutOfStockItems() {
        return ResponseEntity.ok(inventoryService.getOutOfStockItems());
    }
    
    @GetMapping("/transactions/{productId}")
    public ResponseEntity<PagedResponse<InventoryTransaction>> getTransactionHistory(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(inventoryService.getTransactionHistory(productId, page, size));
    }
}
