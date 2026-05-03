# JEE PetStore redesigning to microservices and SaaS architecture

Educational "pet project" inspired by Java Pet Store Sun/Oracle BluePrints Program projects and courses on microservices development with Spring Boot 3 and current tech.stack.

This project demonstrates the transformation of a legacy JavaEE monolith into a modern SaaS microservices architecture. Originally a traditional enterprise application, PetStore was acquired and is being redesigned to host hundreds of clients through a complete architectural overhaul using the strangler fig pattern.

For Step-by-Step story on refactoring of legacy JavaEE monolith application to microservices architecture, please refer to the separate document: [docs\PetStore_Project_History.md](docs/PetStore_Project_History.md)  
For a one-page app summary, see [docs/app-summary.html](docs/app-summary.html).


## Purpose
The PetStore Project serves as a practical example for Java developers, Full Stack Web App developers, team leads, and IT managers of evolving enterprise applications. It demonstrates modern technologies, application design, and development techniques.

## Architecture
### Services Overview
- Product Service with AI agent chat - langchain4j for LLM and Tools integration
- Order Service
- Inventory Service
- Notification Service
- API Gateway using Spring Cloud Gateway MVC
- Shop Frontend using Angular 18

### Tech Stack
- **Spring Boot**
- **Angular**
- **langchain4j**
- **MongoDB** (Product Catalog - document database for flexibility)
- **MySQL**
- **Kafka**
- **Keycloak**
- **Test Containers with Wiremock**
- **Grafana Stack** (Prometheus, Grafana, Loki, and Tempo)
- **API Gateway** using Spring Cloud Gateway MVC
- **Kubernetes**
For local development environment Kubernetes options, please refer to the separate document: [k8s/local_k8s_options.md](k8s/local_k8s_options.md)  

### Application Architecture
The application architecture is depicted in the provided image.
