# Mid Evaluation Report
## Internship Internal Project

**Student Name**: U.V.N.S.Mayuranga  
**Student ID**: 28340  
**Faculty**: Faculty of Computing, NSBM Green University  
**Period**: July 16 Onwards  
**Assigned Service**: Certificate Service (Industry Collaboration Unit System & Vacancy Portal)  

---

## Project 01 – Task Tracker & Technical Fundamentals
**From July 16 – July 28**

### Spring Boot & Microservices
* Learned the basics of Spring Boot 4 and microservices architecture.
* Learned how to organize backend projects using industry-standard package structures (`controller`, `service`, `entity`, `dto`, `repository`, `exception`, `db/migration`).
* Learned how to use an API Gateway and Eureka Discovery Server for microservice routing and service discovery.

### Authentication & Security
* Learned how JWT (JSON Web Tokens) are used for secure stateless authentication.
* Learned how to use Role-Based Access Control (RBAC) to restrict API access based on user roles (Admin, Faculty Coordinator, Student, Industry Partner).
* Learned the basics of Spring Security configurations, filters, and annotations.

### Database & Persistence
* Learned how Spring Boot connects to PostgreSQL and MySQL databases.
* Learned how Models/Entities and objects are mapped to database tables using JPA and Hibernate.
* Learned how Flyway database migration scripts (`db/migration`) are used to manage database schema evolution.

### OOP & Clean Architecture
* Learned how OOP principles (Encapsulation, Inheritance, Polymorphism, Abstraction) are applied in real-world Spring Boot microservices.
* Understood how Data Transfer Objects (DTOs), Repositories, Service layers, and Controllers decouple data transfer from core business logic.

### Project Development
* Applied these concepts while developing task tracking and core backend modules.
* Implemented functionality allowing users to create, assign, update, and manage tasks.
* Implemented role-based authorization for administrative management functions.

### React & Backend Integration
* Learned how to connect a React frontend application with Spring Boot REST APIs.
* Learned how to send data payload requests to the backend and handle JSON responses.
* Learned how to correctly handle HTTP status codes and API error payloads.

### API Testing & Standards
* Learned how to test RESTful API endpoints using Postman and cURL.
* Learned RFC 7807 Problem Details standard for uniform API error payloads.

---

## Project 02 – Industry Collaboration Unit System & Vacancy Portal
**July 28 Onwards (Assigned Module: Certificate Service)**

### System Design & Certificate Service Architecture
* Learned how to decompose a complex enterprise platform into independent microservices.
* Designed and implemented the complete **Certificate Service** module with database tables matching the system ER diagram:
  * `certificates`
  * `certificate_templates`
  * `certificate_verification_logs`
* Implemented Flyway database migration script (`V1__create_certificate_tables.sql`).

### Certificate Generation Logic & QR Code Matrix (Issue #29)
* Implemented dynamic A4 landscape PDF certificate generation using **OpenPDF**.
* Integrated **ZXing QR Code Generator** library to dynamically generate a unique QR code matrix embedding the public verification URL (`/api/v1/certificates/verify/{code}`).
* Added support for overlaying student name, event title, verification code, issue date, and QR code onto custom uploaded template background images.

### Public Certificate Verification & Audit Logs
* Built public certificate verification REST endpoint (`GET /api/v1/certificates/verify/{verificationCode}`) accessible without authentication.
* Implemented automated audit logging into `certificate_verification_logs` capturing verification timestamp and client IP address (`X-Forwarded-For` / `remoteAddr`) for faculty QA and security auditing.

### REST APIs & Data Management
* Developed REST controllers for Certificate Templates (`/api/v1/certificate-templates`) and Certificates (`/api/v1/certificates`).
* Implemented bulk certificate generation (`POST /api/v1/certificates/bulk-generate`) for event attendance completion.
* Implemented PDF certificate download API (`GET /api/v1/certificates/{id}/download`) and student digital wallet retrieval (`GET /api/v1/certificates/student/{studentId}`).
* Configured RFC 7807 Problem Details exception handler (`GlobalExceptionHandler`).

### Docker & Environment Configurations
* Configured database properties in `application.yml` supporting PostgreSQL for production deployment.
* Configured an **H2 In-Memory Database profile** (`spring.profiles.active=h2`) enabling instant zero-dependency local execution and standalone testing.
* Learned how Docker containers manage microservices and database instances.

### Git & Team Collaboration
* Learned how to use Git and GitHub when collaborating in a multi-developer team environment.
* Worked on dedicated feature branches (`feature-certificate-service-backend`) to keep the codebase clean.
* Submitted GitHub Pull Requests (PRs) linked to **Issue #29** for code review by the project team lead.
* Used GitHub Projects, Milestones (`Basic Working System`), and Sprint boards to track task progress.

### Unit Testing & CI Pipelines
* Wrote unit tests using JUnit 5 and Mockito (`CertificateServiceTest` and `CertificateTemplateServiceTest`).
* Verified clean build and test execution (**BUILD SUCCESS - 5/5 tests passed**).
* Learned how GitHub Actions CI/CD pipelines automatically build, test, and validate code on pull request creation.

### Future Learning
* In future sprints, I plan to explore automated CD (Continuous Deployment) pipelines and cloud deployment.
* I also plan to learn more about event-driven messaging (Kafka / RabbitMQ) for asynchronous notification triggers when certificates are issued.
