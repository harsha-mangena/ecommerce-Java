package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.*;

import java.util.List;

public interface UserService {

    UserDto createUser(CreateUserRequest request);
    
    UserDto getUserById(Long id);
    
    UserDto getUserByUsername(String username);
    
    UserDto getUserByEmail(String email);
    
    UserDto updateUser(Long id, UpdateUserRequest request);
    
    void deleteUser(Long id);
    
    AddressDto addAddress(Long userId, CreateAddressRequest request);
    
    List<AddressDto> getUserAddresses(Long userId);
    
    AddressDto getAddress(Long userId, Long addressId);
    
    AddressDto updateAddress(Long userId, Long addressId, CreateAddressRequest request);
    
    void deleteAddress(Long userId, Long addressId);
    
    PaymentMethodDto addPaymentMethod(Long userId, CreatePaymentMethodRequest request);
    
    List<PaymentMethodDto> getUserPaymentMethods(Long userId);
    
    PaymentMethodDto getPaymentMethod(Long userId, Long paymentMethodId);
    
    PaymentMethodDto updatePaymentMethod(Long userId, Long paymentMethodId, CreatePaymentMethodRequest request);
    
    void deletePaymentMethod(Long userId, Long paymentMethodId);
}
