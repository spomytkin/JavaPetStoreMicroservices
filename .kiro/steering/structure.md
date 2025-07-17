# PetStore Project Structure

## Overall Architecture
The PetStore application follows a microservices architecture with the following components:
- Multiple backend microservices (product, order, inventory, notification)
- API Gateway for routing and cross-cutting concerns
- Angular frontend application
- Shared infrastructure (databases, message broker, observability tools)

## Repository Organization
```
/
├── api-gateway/               # Spring Cloud Gateway MVC service
├── docker/                    # Docker configurations for infrastructure components
│   ├── grafana/               # Grafana dashboards and configuration
│   ├── keycloak/              # Keycloak identity server configuration
│   ├── mysql/                 # MySQL database configuration
│   ├── prometheus/            # Prometheus monitoring configuration
│   └── tempo/                 # Tempo tracing configuration
├── docs/                      # Project documentation
├── frontend/                  # Angular 18 frontend application
├── inventory-service/         # Inventory management microservice
├── k8s/                       # Kubernetes deployment configurations
│   ├── manifests/             # Kubernetes manifest files
│   └── [k3s, kind, minikube]  # Local Kubernetes options
├── notification-service/      # Notification handling microservice
├── order-service/             # Order processing microservice
├── product-service/           # Product catalog microservice with AI assistant
└── pom.xml                    # Root Maven POM file
```

## Service Structure Pattern
Each microservice follows a consistent structure:
```
service-name/
├── src/main/java/com/PetStore/[service]/
│   ├── config/                # Configuration classes
│   ├── controller/            # REST API controllers
│   ├── dto/                   # Data Transfer Objects
│   ├── exception/             # Custom exceptions and error handling
│   ├── model/                 # Domain entities
│   ├── repository/            # Data access layer
│   ├── service/               # Business logic layer
│   └── [ServiceName]Application.java  # Main application class
├── src/main/resources/
│   ├── application.yml        # Application configuration
│   └── logback-spring.xml     # Logging configuration
├── src/test/                  # Test classes mirroring main structure
└── pom.xml                    # Service-specific Maven configuration
```

## Code Style Conventions
- Use Lombok annotations (`@Data`, `@Builder`, etc.) for reducing boilerplate
- Follow standard Java naming conventions
  - CamelCase for class names and methods
  - Package names in lowercase
- Use Spring annotations for dependency injection
- MongoDB entities use `@Document` annotation
- REST controllers use `@RestController` and follow REST principles
- DTOs are used for API request/response objects
- Validation uses Jakarta Bean Validation annotations

## Testing Conventions
- Unit tests for service and repository layers
- Integration tests using TestContainers for database operations
- Test classes named with `*Test` suffix
- Test methods follow the given-when-then pattern
- Use `@DataMongoTest` for MongoDB repository tests
- Use `@SpringBootTest` for integration tests