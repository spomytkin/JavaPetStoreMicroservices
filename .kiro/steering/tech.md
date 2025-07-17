# PetStore Technical Stack

## Build System
- Maven for dependency management and build automation
- Spring Boot 3.4.2 as the parent project
- Java 21 as the target JDK version

## Tech Stack
- **Backend Framework**: Spring Boot 3.4.2
- **Database**: 
  - MongoDB for Product Service (document database for flexible product schema)
  - MySQL for other services
- **Messaging**: Kafka for inter-service communication
- **Authentication**: Keycloak for identity and access management
- **Sopping UI Frontend**: Angular 18
- **Admin UI Frontend**: React 19, Next.js
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **AI Integration**: langchain4j for LLM and Tools integration
- **Containerization**: Docker with Docker Compose for local development

## Observability Stack
- **Metrics**: Prometheus with Micrometer
- **Logging**: Loki with logback appender
- **Tracing**: Tempo with Brave
- **Visualization**: Grafana dashboards

## Testing Framework
- JUnit 5 for unit testing
- TestContainers for integration testing with real database instances
- Spring Boot Test for web layer testing
- Rest Assured for API testing
- Mockito for mocking dependencies

## Common Commands

### Maven Commands
```bash
# Build the project
mvn clean install

# Run tests
mvn test

# Run a specific service
mvn spring-boot:run -pl product-service

# Build Docker images
mvn spring-boot:build-image
```

### Docker Commands
```bash
# Start all services
docker-compose up

# Start specific service
docker-compose up product-service

# Stop all services
docker-compose down
```

### Kubernetes Commands
```bash
# Apply manifests
kubectl apply -f k8s/manifests/

# Check service status
kubectl get pods
kubectl get services
```