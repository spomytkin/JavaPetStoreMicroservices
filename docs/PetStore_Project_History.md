# PetStore Project History
The PetStore project is not just a simple "pet project" but one with a real-life-like history, evolving through several iterations:
- **Version 0:** A quick-hack using ASP VBScript during the .NET boom, which was not scalable and required a complete rewrite.
- **First Java Version:** Implemented using Java BluePrints by Sun Microsystems, utilizing JavaServer Pages (JSP), Java Servlets, Enterprise JavaBeans (EJB), and Java Message Service (JMS). This version was deployed on a cluster of BEA Weblogic servers for fault tolerance.
- **Refactoring to J2EE 1.3:** Following the dot-com bust, the application was refactored to J2EE 1.3, improving design patterns, scalability, modularity, and component reusability. The Struts framework was used for MVC design.
- **Java EE 5 Refactor:** In 2006, the application was again refactored with Java EE 5, utilizing JSF, EJB Lite, JPA, Annotations, and Dependency Injection.
- **Java EE 6 Update:** Java EE 6 brought CDI, JPA 2.0, Bean Validation, and RESTful web services in 2009.
- **Java EE 7 Update:** The last backend update was 11 years ago with JPA 2.1, CDI 1.1, Bean Validation 1.1, EJB Lite 3.2, JSF 2.2, and JAX-RS 2.0. The UI was updated with Twitter Bootstrap, JQuery, and PrimeFaces. The application was also deployed to an open-source JBoss server.
- **Cloud and Mobile:** The application was deployed to cloud providers, with Docker adoption for easy deployment. The Web UI was modernized with a rewrite to Angular, while the backend remained the same.

## Move to SaaS
After years of cost-cutting and strong revenue growth, PetStore was acquired by BigCompany. The plan is to turn PetStore into a SaaS solution. However, simply adding TenantID columns won't work for the existing 10-20-year-old codebase. The current database design, while ensuring data integrity, has become a liability with high-volume customers pushing vertical scalability limits.

## SaaS PetStore Rewrite
The need to host hundreds of clients on a SaaS solution requires a complete redesign:
- **Microservices Architecture:** Decoupling domains and building independent, scalable microservices.
- **Observability:** Implementing observability, traceability, and monitoring is crucial for a complex distributed system.
- **Retiring Legacy Components:** Some legacy components, like SignOn, can be replaced by 3rd-party products.
- **New Features:** New features like click-stream integration are required by the business.

The rewrite started with an MVP (Minimum Viable Product) implementing basic PetStore functionality on a new platform, with first sprints focusing on the following services:
- **Product Service**
- **Order Service**
- **Inventory Service**
- **Notification Service**

Users should be able to browse the product catalog, place orders, and receive notifications. The legacy system will be replaced gradually using the strangler fig pattern.

### Database Change
A key change involves the database for the Product Catalog. The strict relational schema doesn't fit well with the multitude of product types. The project moved to using a **document database, MongoDB**, to provide flexibility and scalability, while supporting schema validation.

## MVP
In MVP phase following Services was completed

- Product Service
- Order Service
- Inventory Service
- Notification Service
- PetStore Frontend using Angular 18

For non-functional requrements (observability, traceability, and monitoring) added
- **Grafana Stack** (Prometheus, Grafana, Loki, and Tempo)
- **API Gateway** using Spring Cloud Gateway MVC
now to test in production-like env. pushint it to
- **Kubernetes**

## Productionize

Following the successful MVP implementation — with core microservices (Product, Order, Inventory, Notification), Angular 18 frontend, Grafana observability stack, Spring Cloud Gateway, and initial Kubernetes deployment — the PetStore project now advances to the **Productionize** phase.  

This phase elevates the SaaS-ready microservices architecture to true enterprise production standards. It addresses heightened security requirements in PaaS/cloud environments, resolves real-world data governance gaps exposed by production-like incidents, and delivers the advanced analytics capabilities demanded by marketing and management teams.  

The Productionize roadmap focuses on three tightly integrated pillars that build directly on the existing Kubernetes foundation, Kafka event streams, and MongoDB/MySQL data stores.

### HashiCorp Vault (HCV) – Secure External Secrets Management

As PetStore scales to hundreds of tenants in a full PaaS deployment, the attack surface expands dramatically. Growing dangers from global actors and sophisticated **Advanced Persistent Threats (APTs)** have forced stricter compliance and security standards across the industry. Static secrets in environment variables, config maps, or Git repositories are no longer acceptable. Requirements now include automated regular secret rotation, dynamic credentials with short TTLs, comprehensive audit trails, and zero-trust access patterns.

**HashiCorp Vault** is introduced as the central, external secrets management platform.  

Key capabilities added:  
- Native Kubernetes integration (Vault Agent Injector + CSI provider) for automatic, just-in-time secret injection into pods.  
- Support for all major auth methods (Kubernetes, AppRole, AWS IAM, Token, etc.) and both KV v1/v2 engines.  
- Dynamic database credentials for MongoDB/MySQL and rotation of API keys, TLS certificates, and encryption keys.  
- Full audit logging and policy-based access control aligned with least-privilege principles.  

This eliminates secret sprawl, enables zero-downtime rotation, and brings the entire stack into compliance with modern PaaS security baselines.

### OpenMetadata – Unified Metadata Platform for Data Governance, Lineage & Quality

Early production incidents quickly revealed critical gaps:  
- Data quality degradation (nulls, duplicates, schema drift) silently propagated into reports.  
- Missing or incorrect lineage made root-cause analysis impossible when marketing dashboards showed misleading customer metrics.  
- Management decisions were based on untrustworthy data, creating business risk and compliance exposure.  

To solve these systematically, the project adopts **OpenMetadata** — the leading open-source unified metadata platform. It consolidates in one system:  
- **Data cataloging** & discovery across microservices, Kafka topics, MongoDB collections, and MySQL tables.  
- **Column-level lineage** with automatic extraction from Spark jobs, Kafka streams, and Trino queries.  
- **Data quality** profiling, tests, and KPIs with automated alerts.  
- **Observability** of pipelines, freshness, and SLA compliance.  
- **Governance** (ownership, glossary, tags, access policies) and team collaboration features.  

OpenMetadata becomes the single source of truth for the entire data ecosystem, directly addressing the lineage and quality pain points that caused the incidents.

**FOSS contribution required for seamless integration**  
Our tech stack relies on HashiCorp Vault for secrets. To enable OpenMetadata connectors and ingestion pipelines to securely retrieve credentials without hard-coding or using insecure patterns, a comprehensive HashiCorp Vault integration was contributed upstream.  

See the pull request (and full diff):  
https://github.com/spomytkin/OpenMetadata/pull/1  

The contribution adds:  
- JSON schema and Pydantic models for all Vault auth methods and KV engines.  
- Python `hvac`-based secrets manager + Java server-side implementation.  
- Factory pattern updates, comprehensive tests, and detailed documentation.  

This FOSS work not only unlocked production-grade integration for PetStore but benefits the entire OpenMetadata community.

### Data Lakehouse on Spark + Iceberg (S3) with Trino & Superset

Marketing and management requirements have evolved beyond basic operational reporting. Stakeholders now demand:  
- Real-time and near-real-time customer 360° insights.  
- Flexible ad-hoc analytics for campaign performance, inventory forecasting, and personalization.
- Deep historical trend analysis across months and years.
- Self-service BI dashboards without burdening engineering teams.  
- Cost-effective scaling without expensive traditional data warehouses.  


The solution is a modern **Data Lakehouse** layered on top of the microservices data flows:  

- **Apache Spark running on Kubernetes** – Handles scalable batch and streaming ETL/ELT pipelines that consume Kafka events and operational databases, performing transformations, enrichment, and quality checks.  
- **S3 + Apache Iceberg** – Open table format that provides ACID transactions, schema evolution, hidden partitioning, and immutable snapshots on cheap S3 storage. Iceberg adds full warehouse-grade historical capabilities to the lakehouse. No extra ETL or history tables needed — all trends run directly on cheap object storage while preserving compute-storage separation and OpenMetadata governance. 
- **Trino** – Distributed SQL query engine for lightning-fast, “in-the-fly” (ad-hoc) analytics. Trino federates queries across the Iceberg lakehouse, operational MySQL/MongoDB, and even external sources, with native Iceberg optimizations for metadata-based pruning and sub-second BI workloads.  
- **Apache Superset** – Modern BI visualization layer connected directly to Trino. Business users gain drag-and-drop dashboards, charts, and self-service exploration while all metadata, lineage, and quality rules are governed by OpenMetadata.  

**Benefits realized**  
- Compute-storage separation → elastic scaling and dramatic cost reduction.  
- Open formats → no vendor lock-in and multi-engine interoperability (Spark, Trino, future Flink, etc.).  
- End-to-end governance via OpenMetadata → trustworthy analytics.  
- Real-time + batch unification in one platform.
- Full historical trend analysis (MoM, QoQ, YoY) and point-in-time reporting on immutable snapshots — zero extra ETL or history tables required

This Data Lakehouse completes the transition from a simple microservices MVP to a fully production-grade, analytics-enabled SaaS platform.


