package com.ecommerce.userservice.service.impl;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.entity.*;
import com.ecommerce.userservice.event.UserCreatedEvent;
import com.ecommerce.userservice.event.UserUpdatedEvent;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.exception.UserAlreadyExistsException;
import com.ecommerce.userservice.repository.AddressRepository;
import com.ecommerce.userservice.repository.PaymentMethodRepository;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.user-created}")
    private String userCreatedTopic;

    @Value("${kafka.topics.user-updated}")
    private String userUpdatedTopic;

    @Value("${kafka.topics.user-deleted}")
    private String userDeletedTopic;

    @Override
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .status(UserStatus.ACTIVE)
                .addresses(new HashSet<>())
                .paymentMethods(new HashSet<>())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Created user: {}", savedUser.getId());

        // Publish user created event
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .build();
        
        kafkaTemplate.send(userCreatedTopic, savedUser.getId().toString(), event);
        log.info("Published user created event for user: {}", savedUser.getId());

        return mapToUserDto(savedUser);
    }

    @Override
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToUserDto(user);
    }

    @Override
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return mapToUserDto(user);
    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToUserDto(user);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        User updatedUser = userRepository.save(user);
        log.info("Updated user: {}", updatedUser.getId());

        // Publish user updated event
        UserUpdatedEvent event = UserUpdatedEvent.builder()
                .userId(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .phoneNumber(updatedUser.getPhoneNumber())
                .build();
        
        kafkaTemplate.send(userUpdatedTopic, updatedUser.getId().toString(), event);
        log.info("Published user updated event for user: {}", updatedUser.getId());

        return mapToUserDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Change status to DELETED instead of actually deleting
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
        
        log.info("Deleted (marked as DELETED) user: {}", id);

        // Publish user deleted event
        kafkaTemplate.send(userDeletedTopic, id.toString(), id);
        log.info("Published user deleted event for user: {}", id);
    }

    @Override
    @Transactional
    public AddressDto addAddress(Long userId, CreateAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // If setting as default, update all other addresses to non-default
        if (request.isDefault()) {
            user.getAddresses().forEach(address -> address.setDefault(false));
        }

        Address address = Address.builder()
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .isDefault(request.isDefault())
                .addressType(AddressType.valueOf(request.getAddressType()))
                .user(user)
                .build();

        Address savedAddress = addressRepository.save(address);
        log.info("Added address for user: {}", userId);

        return mapToAddressDto(savedAddress);
    }

    @Override
    public List<AddressDto> getUserAddresses(Long userId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        List<Address> addresses = addressRepository.findByUserId(userId);
        return addresses.stream()
                .map(this::mapToAddressDto)
                .collect(Collectors.toList());
    }

    @Override
    public AddressDto getAddress(Long userId, Long addressId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        // Verify address belongs to user
        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found for user");
        }

        return mapToAddressDto(address);
    }

    @Override
    @Transactional
    public AddressDto updateAddress(Long userId, Long addressId, CreateAddressRequest request) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        // Verify address belongs to user
        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found for user");
        }

        // If setting as default, update all other addresses to non-default
        if (request.isDefault() && !address.isDefault()) {
            user.getAddresses().forEach(addr -> {
                if (!addr.getId().equals(addressId)) {
                    addr.setDefault(false);
                }
            });
        }

        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setPostalCode(request.getPostalCode());
        address.setDefault(request.isDefault());
        address.setAddressType(AddressType.valueOf(request.getAddressType()));

        Address updatedAddress = addressRepository.save(address);
        log.info("Updated address: {} for user: {}", addressId, userId);

        return mapToAddressDto(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        // Verify address belongs to user
        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found for user");
        }

        addressRepository.delete(address);
        log.info("Deleted address: {} for user: {}", addressId, userId);
    }

    @Override
    @Transactional
    public PaymentMethodDto addPaymentMethod(Long userId, CreatePaymentMethodRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // If setting as default, update all other payment methods to non-default
        if (request.isDefault()) {
            user.getPaymentMethods().forEach(pm -> pm.setDefault(false));
        }

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .paymentType(PaymentType.valueOf(request.getPaymentType()))
                .cardNumber(request.getCardNumber())
                .expiryDate(request.getExpiryDate())
                .cardHolderName(request.getCardHolderName())
                .isDefault(request.isDefault())
                .user(user)
                .build();

        PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Added payment method for user: {}", userId);

        return mapToPaymentMethodDto(savedPaymentMethod);
    }

    @Override
    public List<PaymentMethodDto> getUserPaymentMethods(Long userId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        List<PaymentMethod> paymentMethods = paymentMethodRepository.findByUserId(userId);
        return paymentMethods.stream()
                .map(this::mapToPaymentMethodDto)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentMethodDto getPaymentMethod(Long userId, Long paymentMethodId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", paymentMethodId));

        // Verify payment method belongs to user
        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Payment method not found for user");
        }

        return mapToPaymentMethodDto(paymentMethod);
    }

    @Override
    @Transactional
    public PaymentMethodDto updatePaymentMethod(Long userId, Long paymentMethodId, CreatePaymentMethodRequest request) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", paymentMethodId));

        // Verify payment method belongs to user
        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Payment method not found for user");
        }

        // If setting as default, update all other payment methods to non-default
        if (request.isDefault() && !paymentMethod.isDefault()) {
            user.getPaymentMethods().forEach(pm -> {
                if (!pm.getId().equals(paymentMethodId)) {
                    pm.setDefault(false);
                }
            });
        }

        paymentMethod.setPaymentType(PaymentType.valueOf(request.getPaymentType()));
        paymentMethod.setCardNumber(request.getCardNumber());
        paymentMethod.setExpiryDate(request.getExpiryDate());
        paymentMethod.setCardHolderName(request.getCardHolderName());
        paymentMethod.setDefault(request.isDefault());

        PaymentMethod updatedPaymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Updated payment method: {} for user: {}", paymentMethodId, userId);

        return mapToPaymentMethodDto(updatedPaymentMethod);
    }

    @Override
    @Transactional
    public void deletePaymentMethod(Long userId, Long paymentMethodId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", paymentMethodId));

        // Verify payment method belongs to user
        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Payment method not found for user");
        }

        paymentMethodRepository.delete(paymentMethod);
        log.info("Deleted payment method: {} for user: {}", paymentMethodId, userId);
    }

    // Helper methods to map entities to DTOs
    private UserDto mapToUserDto(User user) {
        Set<AddressDto> addressDtos = user.getAddresses().stream()
                .map(this::mapToAddressDto)
                .collect(Collectors.toSet());

        Set<PaymentMethodDto> paymentMethodDtos = user.getPaymentMethods().stream()
                .map(this::mapToPaymentMethodDto)
                .collect(Collectors.toSet());

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .addresses(addressDtos)
                .paymentMethods(paymentMethodDtos)
                .build();
    }

    private AddressDto mapToAddressDto(Address address) {
        return AddressDto.builder()
                .id(address.getId())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .isDefault(address.isDefault())
                .addressType(address.getAddressType().name())
                .build();
    }

    private PaymentMethodDto mapToPaymentMethodDto(PaymentMethod paymentMethod) {
        return PaymentMethodDto.builder()
                .id(paymentMethod.getId())
                .paymentType(paymentMethod.getPaymentType().name())
                .cardNumber(paymentMethod.getCardNumber())
                .expiryDate(paymentMethod.getExpiryDate())
                .cardHolderName(paymentMethod.getCardHolderName())
                .isDefault(paymentMethod.isDefault())
                .build();
    }
}
