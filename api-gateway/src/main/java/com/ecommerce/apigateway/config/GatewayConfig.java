package com.ecommerce.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("product_service_route", r -> r
                        .path("/api/products/**")
                        .filters(f -> f.rewritePath("/api/products/(?<segment>.*)", "/api/products/${segment}")
                                .circuitBreaker(c -> c.setName("productServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/product")))
                        .uri("lb://PRODUCT-SERVICE"))
                .route("order_service_route", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f.rewritePath("/api/orders/(?<segment>.*)", "/api/orders/${segment}")
                                .circuitBreaker(c -> c.setName("orderServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/order")))
                        .uri("lb://ORDER-SERVICE"))
                .route("payment_service_route", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.rewritePath("/api/payments/(?<segment>.*)", "/api/payments/${segment}")
                                .circuitBreaker(c -> c.setName("paymentServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/payment")))
                        .uri("lb://PAYMENT-SERVICE"))
                .route("inventory_service_route", r -> r
                        .path("/api/inventory/**")
                        .filters(f -> f.rewritePath("/api/inventory/(?<segment>.*)", "/api/inventory/${segment}")
                                .circuitBreaker(c -> c.setName("inventoryServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/inventory")))
                        .uri("lb://INVENTORY-SERVICE"))
                .route("notification_service_route", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.rewritePath("/api/notifications/(?<segment>.*)", "/api/notifications/${segment}")
                                .circuitBreaker(c -> c.setName("notificationServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/notification")))
                        .uri("lb://NOTIFICATION-SERVICE"))
                .route("auth_service_route", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.rewritePath("/api/auth/(?<segment>.*)", "/api/auth/${segment}")
                                .circuitBreaker(c -> c.setName("authServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/auth")))
                        .uri("lb://AUTH-SERVICE"))
                .route("user_service_route", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.rewritePath("/api/users/(?<segment>.*)", "/api/users/${segment}")
                                .circuitBreaker(c -> c.setName("userServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user")))
                        .uri("lb://USER-SERVICE"))
                .build();
    }
}
