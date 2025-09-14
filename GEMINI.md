# Gemini Workspace Configuration

This file provides context and instructions for the Gemini AI assistant to effectively interact with the JavaPetStoreMicroservices project.

## Project Overview

This project is an educational example of a Java EE application redesigned into a microservices architecture. It demonstrates modern technologies, application design, and development techniques for enterprise applications.

## Architecture

The application is composed of the following microservices:

- **Product Service:** Manages product information and includes an AI agent chat feature using langchain4j.
- **Order Service:** Handles customer orders.
- **Inventory Service:** Manages product inventory.
- **Notification Service:** Sends notifications to users.
- **API Gateway:** Routes requests to the appropriate microservices using Spring Cloud Gateway.
- **Shop Frontend:** A user-facing web application built with Angular.

## Tech Stack

- **Backend:** Spring Boot 3
- **Frontend:** Angular 18
- **AI Integration:** langchain4j
- **Databases:** MongoDB, MySQL
- **Messaging:** Kafka
- **Identity and Access Management:** Keycloak
- **Testing:** Test Containers with Wiremock
- **Observability:** Grafana Stack (Prometheus, Grafana, Loki, and Tempo)
- **Deployment:** Docker, Kubernetes

## Development

### Build

To build the entire project, run the following command from the root directory:

```bash
mvn clean install
```

### Test

To run the tests for the entire project, run the following command from the root directory:

```bash
mvn test
```

### Run

To run the application, you can use the `docker-compose.yml` file in the root directory:

```bash
docker-compose up -d
```

## Key Files

- `pom.xml`: The main Maven project file, defining dependencies and build settings.
- `docker-compose.yml`: Defines the services, networks, and volumes for running the application with Docker Compose.
- `k8s/`: Contains Kubernetes configuration files.
- `docs/`: Contains project documentation.
- `api-gateway/`: The Spring Cloud Gateway service.
- `product-service/`: The product microservice.
- `order-service/`: The order microservice.
- `inventory-service/`: The inventory microservice.
- `notification-service/`: The notification microservice.
- `frontend/`: The Angular frontend application.
