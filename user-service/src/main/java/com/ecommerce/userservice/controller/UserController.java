package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.service.UserService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @CircuitBreaker(name = "userService", fallbackMethod = "createUserFallback")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        return new ResponseEntity<>(userService.createUser(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/username/{username}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByUsernameFallback")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @GetMapping("/email/{email}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @PutMapping("/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "updateUserFallback")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "deleteUserFallback")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Address endpoints
    @PostMapping("/{userId}/addresses")
    @CircuitBreaker(name = "userService", fallbackMethod = "addAddressFallback")
    public ResponseEntity<AddressDto> addAddress(
            @PathVariable Long userId,
            @Valid @RequestBody CreateAddressRequest request) {
        return new ResponseEntity<>(userService.addAddress(userId, request), HttpStatus.CREATED);
    }

    @GetMapping("/{userId}/addresses")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserAddressesFallback")
    public ResponseEntity<List<AddressDto>> getUserAddresses(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserAddresses(userId));
    }

    @GetMapping("/{userId}/addresses/{addressId}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getAddressFallback")
    public ResponseEntity<AddressDto> getAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        return ResponseEntity.ok(userService.getAddress(userId, addressId));
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    @CircuitBreaker(name = "userService", fallbackMethod = "updateAddressFallback")
    public ResponseEntity<AddressDto> updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody CreateAddressRequest request) {
        return ResponseEntity.ok(userService.updateAddress(userId, addressId, request));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    @CircuitBreaker(name = "userService", fallbackMethod = "deleteAddressFallback")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    // Payment method endpoints
    @PostMapping("/{userId}/payment-methods")
    @CircuitBreaker(name = "userService", fallbackMethod = "addPaymentMethodFallback")
    public ResponseEntity<PaymentMethodDto> addPaymentMethod(
            @PathVariable Long userId,
            @Valid @RequestBody CreatePaymentMethodRequest request) {
        return new ResponseEntity<>(userService.addPaymentMethod(userId, request), HttpStatus.CREATED);
    }

    @GetMapping("/{userId}/payment-methods")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserPaymentMethodsFallback")
    public ResponseEntity<List<PaymentMethodDto>> getUserPaymentMethods(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserPaymentMethods(userId));
    }

    @GetMapping("/{userId}/payment-methods/{paymentMethodId}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getPaymentMethodFallback")
    public ResponseEntity<PaymentMethodDto> getPaymentMethod(
            @PathVariable Long userId,
            @PathVariable Long paymentMethodId) {
        return ResponseEntity.ok(userService.getPaymentMethod(userId, paymentMethodId));
    }

    @PutMapping("/{userId}/payment-methods/{paymentMethodId}")
    @CircuitBreaker(name = "userService", fallbackMethod = "updatePaymentMethodFallback")
    public ResponseEntity<PaymentMethodDto> updatePaymentMethod(
            @PathVariable Long userId,
            @PathVariable Long paymentMethodId,
            @Valid @RequestBody CreatePaymentMethodRequest request) {
        return ResponseEntity.ok(userService.updatePaymentMethod(userId, paymentMethodId, request));
    }

    @DeleteMapping("/{userId}/payment-methods/{paymentMethodId}")
    @CircuitBreaker(name = "userService", fallbackMethod = "deletePaymentMethodFallback")
    public ResponseEntity<Void> deletePaymentMethod(
            @PathVariable Long userId,
            @PathVariable Long paymentMethodId) {
        userService.deletePaymentMethod(userId, paymentMethodId);
        return ResponseEntity.noContent().build();
    }

    // Fallback methods
    public ResponseEntity<UserDto> createUserFallback(CreateUserRequest request, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<UserDto> getUserByIdFallback(Long id, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<UserDto> getUserByUsernameFallback(String username, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<UserDto> getUserByEmailFallback(String email, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<UserDto> updateUserFallback(Long id, UpdateUserRequest request, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<Void> deleteUserFallback(Long id, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<AddressDto> addAddressFallback(Long userId, CreateAddressRequest request, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<List<AddressDto>> getUserAddressesFallback(Long userId, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<AddressDto> getAddressFallback(Long userId, Long addressId, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<AddressDto> updateAddressFallback(Long userId, Long addressId, CreateAddressRequest request, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<Void> deleteAddressFallback(Long userId, Long addressId, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<PaymentMethodDto> addPaymentMethodFallback(Long userId, CreatePaymentMethodRequest request, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<List<PaymentMethodDto>> getUserPaymentMethodsFallback(Long userId, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<PaymentMethodDto> getPaymentMethodFallback(Long userId, Long paymentMethodId, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<PaymentMethodDto> updatePaymentMethodFallback(Long userId, Long paymentMethodId, CreatePaymentMethodRequest request, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    public ResponseEntity<Void> deletePaymentMethodFallback(Long userId, Long paymentMethodId, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }
}
