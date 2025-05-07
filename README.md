# Ecommerce Microservices System

This project is a comprehensive ecommerce system built with Spring Boot microservices architecture. It demonstrates how to build a scalable, resilient, and event-driven ecommerce platform using modern Java technologies.

## Why Microservices?

Traditional monolithic applications can become difficult to maintain and scale as they grow. Our microservices approach offers several advantages:

- **Independent Deployment** - Services can be updated independently without affecting the entire system
- **Technology Flexibility** - Each service can use the most appropriate technology stack
- **Scalability** - Services can be scaled individually based on demand (e.g., scaling the product catalog during sales)
- **Resilience** - Failures are isolated to individual services rather than bringing down the entire system
- **Team Organization** - Development teams can work on different services independently
- **Easier Maintenance** - Smaller codebases are easier to understand and maintain

While microservices add complexity in terms of deployment and service coordination, the benefits outweigh the costs for large-scale applications like ecommerce platforms that require flexibility and scalability.

## Architecture

The system consists of the following microservices:

1. **Service Registry** - Eureka server for service discovery
2. **API Gateway** - Spring Cloud Gateway for routing, load balancing, and security
3. **Auth Service** - Handles user authentication and authorization using JWT
4. **User Service** - Manages user profiles, addresses, and payment methods
5. **Product Service** - Manages product catalog and categories
6. **Order Service** - Handles order creation and management
7. **Inventory Service** - Manages product inventory and stock
8. **Payment Service** - Processes payments via various payment methods
9. **Notification Service** - Sends notifications to users via email

### Architecture Diagram

```
                     ┌─────────────────┐
                     │                 │
                     │ Service Registry│
                     │    (Eureka)     │
                     │                 │
                     └─────────────────┘
                             ▲
                             │
                             │ Register/Discover
                             │
                 ┌───────────┴───────────┐
                 │                       │
                 │                       │
 ┌───────────────▼───────────────┐       │
 │                               │       │
 │         API Gateway           │       │
 │     (Spring Cloud Gateway)    │       │
 │                               │       │
 └───────────┬───────────┬───────┘       │
             │           │               │
             │           │               │
   ┌─────────▼─┐   ┌─────▼─────┐   ┌─────▼─────┐
   │           │   │           │   │           │
   │Auth Service│   │User Service│   │Product    │
   │(JWT Auth)  │   │(Profiles) │   │Service    │
   │           │   │           │   │(Catalog)  │
   └─────────┬─┘   └─────┬─────┘   └─────┬─────┘
             │           │               │
             │           │               │
             │     ┌─────▼─────┐   ┌─────▼─────┐
             │     │           │   │           │
             │     │Order      │   │Inventory  │
             └────►│Service    │◄──┤Service    │
                   │(Orders)   │   │(Stock)    │
                   │           │   │           │
                   └────┬──────┘   └───────────┘
                        │
                        │
              ┌─────────▼──────┐   ┌───────────────┐
              │                │   │               │
              │Payment Service │◄──┤Notification   │
              │(Transactions) │   │Service        │
              │                │   │(Emails/SMS)  │
              └────────────────┘   └───────────────┘

            Kafka Event Bus (Asynchronous Communication)
     ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### How It Works

When a user interacts with our ecommerce platform, their request first hits the **API Gateway**, which acts as the single entry point for all client requests. The gateway authenticates the request via the **Auth Service** and then routes it to the appropriate microservice.

Each microservice handles a specific business capability and communicates with others when needed. For instance, when a user places an order, the **Order Service** checks product availability with the **Inventory Service**, processes payment via the **Payment Service**, and triggers the **Notification Service** to send an order confirmation.

All these interactions happen seamlessly, with each microservice focusing on its core responsibility, making the system modular, scalable, and maintainable.

## Technologies

- **Java 17** - Modern Java with enhanced features like records and pattern matching
- **Spring Boot 3.2** - Streamlines application development with auto-configuration
- **Spring Cloud 2023.0.0** - Provides tools for building distributed systems
- **Spring Data JPA** - Simplifies database operations with repository abstractions
- **MySQL** - Robust relational database for persistent data storage
- **Kafka** - High-throughput distributed messaging system for event-driven architecture
- **JWT** - Secure, compact token format for authentication between services
- **Resilience4j** - Fault tolerance library with circuit breakers to prevent cascading failures
- **Docker & Docker Compose** - Containerization for consistent deployment environments
- **Eureka** - Service registry for service discovery in a microservices environment
- **Spring Cloud Gateway** - Modern API gateway built on Spring WebFlux

### Why These Technologies?

This tech stack was carefully chosen to provide a balance of performance, developer productivity, and operational reliability:

- **Spring Boot** accelerates development with sensible defaults while allowing customization.
- **Microservices architecture** enables independent scaling and deployment of components.
- **Event-driven communication** via Kafka reduces coupling between services and improves resilience.
- **Containerization** with Docker simplifies deployment and ensures consistency across environments.
- **Service discovery** with Eureka allows services to find each other without hardcoded URLs.

## Getting Started

### Prerequisites

- JDK 17+
- Maven
- Docker and Docker Compose

### Building the Project

Build all services:

```bash
mvn clean package -DskipTests
```

### Running with Docker Compose

Start all services with Docker Compose:

```bash
docker-compose up -d
```

## Service Endpoints

- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **Auth Service**: http://localhost:8081/api/auth
- **User Service**: http://localhost:8082/api/users
- **Product Service**: http://localhost:8083/api/products
- **Order Service**: http://localhost:8084/api/orders
- **Inventory Service**: http://localhost:8085/api/inventory
- **Payment Service**: http://localhost:8086/api/payments
- **Notification Service**: http://localhost:8087/api/notifications

## Features

### Authentication
- User registration and login
- JWT-based authentication
- Token refresh mechanism

### User Management
- Profile management
- Address management
- Payment method management

### Product Catalog
- Product creation and management
- Category management
- Product search and filtering

### Order Processing
- Shopping cart
- Order creation
- Order status tracking

### Inventory Management
- Stock management
- Inventory updates
- Reservation system

### Payment Processing
- Multiple payment methods
- Payment status tracking
- Integration with external payment gateways

### Notification System
- Email notifications
- Order status updates
- Transactional emails

## Event-Driven Architecture

This project implements event-driven communication between microservices using Kafka, which decouples services and improves system resilience:

- **UserCreatedEvent** - Triggered when a new user is created, allowing services like notification to welcome new users
- **OrderCreatedEvent** - Triggered when a new order is placed, prompting inventory checks and payment processing
- **PaymentProcessedEvent** - Triggered when a payment is processed, updating order status and triggering notifications
- **InventoryUpdatedEvent** - Triggered when inventory is updated, ensuring product availability is reflected across services

### The Power of Event-Driven Design

In a traditional architecture, if the payment service is temporarily down, the entire order process would fail. With our event-driven approach, the order service publishes an event and continues. When the payment service recovers, it processes pending events from the queue.

This architecture also enables:

- **Temporal decoupling** - Services don't need to be available simultaneously
- **Easy extensibility** - New services can subscribe to existing events without modifying publishers
- **Better scalability** - Services can scale independently based on their specific workload
- **Improved resilience** - Failure in one service doesn't cascade to others

## Fault Tolerance

The system implements fault tolerance mechanisms:

- **Circuit Breakers** - Using Resilience4j to prevent cascading failures
- **Retry Mechanisms** - Automatically retry failed operations
- **Fallback Methods** - Provide alternative responses when a service is unavailable

## Security

Our ecommerce platform prioritizes security at every level:

- **JWT Authentication** - Secure API access with JSON Web Tokens that carry encrypted user identity and permissions
- **Role-Based Access Control** - Different privileges for customers, admins, and sellers ensure appropriate access
- **API Gateway Security** - Centralized authentication and authorization at the gateway level
- **Secure Communication** - Service-to-service communication is authenticated to prevent unauthorized access
- **Password Encryption** - User passwords are stored using strong hashing algorithms (BCrypt)
- **Input Validation** - All user inputs are validated to prevent injection attacks
- **Rate Limiting** - Protection against brute force attacks and denial of service

### Security Flow

When a user logs in, the Auth Service validates credentials and issues a JWT token. This token is then included in subsequent requests to prove the user's identity. The API Gateway validates this token before routing requests to internal services, acting as the first line of defense.

## Monitoring

- **Actuator Endpoints** - Health checks and metrics for operational visibility
- **Prometheus Integration** - Collect and store metrics for monitoring
- **Centralized Logging** - Aggregated logs for troubleshooting and analysis
- **Distributed Tracing** - Track requests across multiple services for performance analysis

## User Journey

Let's walk through a typical user journey to understand how our services collaborate:

1. **Registration & Login**:
   - User registers through the Auth Service
   - User credentials are validated and stored securely
   - Upon login, a JWT token is issued for subsequent requests

2. **Browsing Products**:
   - User browses products through the Product Service
   - Inventory Service provides real-time stock information
   - Search and filtering capabilities enhance user experience

3. **Placing an Order**:
   - User adds items to cart (managed by Order Service)
   - Upon checkout, Order Service creates an order
   - Inventory Service checks and reserves stock
   - Payment Service processes payment
   - Upon successful payment, order is confirmed
   - Notification Service sends order confirmation

4. **Order Fulfillment**:
   - Order Service updates order status
   - Inventory Service updates stock levels
   - Notification Service sends shipping updates

Each step involves multiple services working together seamlessly, with events flowing through Kafka to ensure consistency and reliability.

## Code Structure

Each microservice follows a standard structure:
- **Controller** - REST API endpoints
- **Service** - Business logic
- **Repository** - Data access
- **Entity** - Data models
- **DTO** - Data transfer objects
- **Event** - Event models for Kafka
- **Config** - Configuration classes
- **Exception** - Custom exceptions and handlers

## Development Best Practices

This project demonstrates several microservices best practices:

### Domain-Driven Design
Services are organized around business capabilities rather than technical layers. This makes the system more intuitive and aligned with business needs.

### API First Approach
We defined service contracts (APIs) before implementation, ensuring clear interfaces between services.

### Circuit Breaking
Using Resilience4j, services gracefully handle failures of dependent services, preventing cascading failures.

### Database Per Service
Each service has its own database, ensuring loose coupling and independent evolution.

### Continuous Integration
The project is set up for CI/CD pipelines with automated testing and containerized deployment.

### Observability
Comprehensive logging, metrics, and tracing make it easier to monitor and troubleshoot the system.

These practices ensure the system remains maintainable and resilient as it evolves.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
