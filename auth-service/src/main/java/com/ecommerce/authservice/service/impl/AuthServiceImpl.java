package com.ecommerce.authservice.service.impl;

import com.ecommerce.authservice.dto.*;
import com.ecommerce.authservice.entity.RefreshToken;
import com.ecommerce.authservice.entity.Role;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.event.UserCreatedEvent;
import com.ecommerce.authservice.exception.TokenException;
import com.ecommerce.authservice.exception.UserAlreadyExistsException;
import com.ecommerce.authservice.exception.UserNotFoundException;
import com.ecommerce.authservice.repository.RefreshTokenRepository;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.authservice.security.JwtService;
import com.ecommerce.authservice.service.AuthService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;
    
    @Value("${kafka.topics.user-created}")
    private String userCreatedTopic;

    @Override
    @Transactional
    @CircuitBreaker(name = "authService", fallbackMethod = "registerFallback")
    public AuthResponse register(RegisterRequest request) {
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
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.ROLE_USER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Publish user created event
        publishUserCreatedEvent(savedUser);
        
        // Generate tokens
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", savedUser.getRole().name());
        
        String accessToken = jwtService.generateToken(claims, savedUser);
        RefreshToken refreshToken = createRefreshToken(savedUser);
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtExpiration / 1000)
                .user(mapToUserDto(savedUser))
                .build();
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "authService", fallbackMethod = "loginFallback")
    public AuthResponse login(LoginRequest request) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        
        User user = (User) authentication.getPrincipal();
        
        // Update last login timestamp
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        // Generate tokens
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        
        String accessToken = jwtService.generateToken(claims, user);
        
        // Revoke any existing refresh tokens for this user
        refreshTokenRepository.revokeAllTokensByUser(user);
        
        // Create new refresh token
        RefreshToken refreshToken = createRefreshToken(user);
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtExpiration / 1000)
                .user(mapToUserDto(user))
                .build();
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "authService", fallbackMethod = "refreshTokenFallback")
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        return refreshTokenRepository.findByToken(request.getRefreshToken())
                .map(this::verifyRefreshToken)
                .map(RefreshToken::getUser)
                .map(user -> {
                    // Generate new access token
                    Map<String, Object> claims = new HashMap<>();
                    claims.put("role", user.getRole().name());
                    String accessToken = jwtService.generateToken(claims, user);
                    
                    return AuthResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(request.getRefreshToken())
                            .expiresIn(jwtExpiration / 1000)
                            .user(mapToUserDto(user))
                            .build();
                })
                .orElseThrow(() -> new TokenException("Invalid refresh token"));
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new TokenException("Invalid refresh token"));
        
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Override
    public void validateToken(String token) {
        jwtService.extractUsername(token);
        // If no exception is thrown, token is valid
    }
    
    private RefreshToken verifyRefreshToken(RefreshToken refreshToken) {
        if (refreshToken.isExpired() || refreshToken.isRevoked()) {
            throw new TokenException("Refresh token expired or revoked");
        }
        return refreshToken;
    }
    
    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    private void publishUserCreatedEvent(User user) {
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
        
        kafkaTemplate.send(userCreatedTopic, user.getId().toString(), event);
        log.info("Published user created event for user: {}", user.getUsername());
    }
    
    // Fallback methods for circuit breaker
    private AuthResponse registerFallback(RegisterRequest request, Exception e) {
        log.error("Fallback: Unable to register user due to: {}", e.getMessage());
        throw new RuntimeException("Service is temporarily unavailable. Please try again later.");
    }
    
    private AuthResponse loginFallback(LoginRequest request, Exception e) {
        log.error("Fallback: Unable to login user due to: {}", e.getMessage());
        throw new RuntimeException("Service is temporarily unavailable. Please try again later.");
    }
    
    private AuthResponse refreshTokenFallback(RefreshTokenRequest request, Exception e) {
        log.error("Fallback: Unable to refresh token due to: {}", e.getMessage());
        throw new RuntimeException("Service is temporarily unavailable. Please try again later.");
    }
}
