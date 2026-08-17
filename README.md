# Industry Alumni Portal — Backend

A Spring Boot microservices backend for the Industry Alumni Collaboration Portal at NSBM.

---

## Project Documentation

> **New to this project? Start here.**

| Document | Description |
|---|---|
| [`docs/ultimate_technical_guide.txt`](./docs/ultimate_technical_guide.txt) | Full technical roadmap — all 11 services, required endpoints, RabbitMQ events, and completion criteria |
| [`docs/Services.pdf`](./docs/Services.pdf) | Original service specification document |
| [`docs/R&D_Final.pdf`](./docs/R&D_Final.pdf) | Research & Design document outlining the 3 business modules |
| [`docs/Internal Project System Diagram.gif`](./docs/Internal%20Project%20System%20Diagram.gif) | Full system architecture diagram |
| [`docs/dummy-data/`](./docs/dummy-data/) | SQL scripts to seed all databases with test data |
| [`docs/openapi/`](./docs/openapi/) | OpenAPI JSON specs for all completed services |

---

## Architecture Overview

All microservices register with **Eureka Discovery Server** (`http://localhost:8761`) and are accessible through the **API Gateway** (`http://localhost:8080`). Services communicate via **HTTP REST** for synchronous calls and **RabbitMQ** for asynchronous events (e.g. email notifications).

```
Client
  │
  ▼
API Gateway (8080)   ──► Eureka Discovery Server (8761)
  │
  │  ── IMPLEMENTED ──────────────────────────────────────
  ├──► Auth Service (8081)                  ──► auth_db
  ├──► Event Management Service (8082)      ──► event_management_db
  ├──► Event Participation Service (8083)   ──► event_participation_db
  ├──► Application Service (8084)           ──► application_service_db
  ├──► Platform Management Service (8086)   ──► platform_management_db
  ├──► Notification Service (8088)          ──► notification
  ├──► Audit & Storage Service (8089)       ──► audit_storage_db
  │                                         ──► S3 / MinIO (9000)
  │
  │  ── PLANNED ──────────────────────────────────────────
  ├──► User Service                         ──► user_db
  ├──► Certificate Service                  ──► certificate_db
  ├──► Vacancy Service                      ──► vacancy_db
  └──► AI Service                           ──► (Vector DB / Storage)
```

---

## Service Directory

| Service | Port | Database | Status | Swagger UI |
|---|---|---|---|---|
| [API Gateway](./api-gateway/README.md) | **8080** | — | ✅ Live | `http://localhost:8080/swagger-ui.html` |
| [Auth Service](./services/auth-service/README.md) | **8081** | `auth_db` | ✅ Live | `http://localhost:8081/swagger-ui.html` |
| [Event Management Service](./services/event-management-service/README.md) | **8082** | `event_management_db` | ✅ Live | `http://localhost:8082/swagger-ui.html` |
| [Event Participation Service](./services/event-participation-service/README.md) | **8083** | `event_participation_db` | ✅ Live | `http://localhost:8083/swagger-ui.html` |
| [Application Service](./services/application-service/README.md) | **8084** | `application_service_db` | ✅ Live | `http://localhost:8084/swagger-ui.html` |
| [Platform Management Service](./services/platform-management-service/README.md) | **8086** | `platform_management_db` | ✅ Live | `http://localhost:8086/swagger-ui.html` |
| [Notification Service](./services/notification-service/README.md) | **8088** | `notification` | ✅ Live | `http://localhost:8088/swagger-ui.html` |
| [Audit & Storage Service](./services/audit-storage-service/README.md) | **8089** | `audit_storage_db` | ✅ Live | `http://localhost:8089/swagger-ui.html` |
| [Discovery Server](./discovery-server/README.md) | **8761** | — | ✅ Live | `http://localhost:8761` |
| *User Service* | *TBD* | *user_db* | 🔲 Planned | — |
| *Certificate Service* | *TBD* | *certificate_db* | 🔲 Planned | — |
| *Vacancy Service* | *TBD* | *vacancy_db* | 🔲 Planned | — |
| *AI Service* | *TBD* | *(Vector DB)* | 🔲 Planned | — |

> The aggregated Swagger UI (all services combined) is available at `http://localhost:8080/swagger-ui.html` when the gateway is running.

---

## Prerequisites

| Dependency | Version | Default Port |
|---|---|---|
| Java | 21+ | — |
| Maven Wrapper | included | — |
| PostgreSQL | 15+ | 5432 |
| RabbitMQ | 3.x+ | 5672 |
| MinIO / AWS S3 | any | 9000 (local) |

### Required Databases (PostgreSQL)
Create these databases before starting the services:
```sql
CREATE DATABASE auth_db;
CREATE DATABASE event_management_db;
CREATE DATABASE event_participation_db;
CREATE DATABASE application_service_db;
CREATE DATABASE platform_management_db;
CREATE DATABASE notification;
CREATE DATABASE audit_storage_db;
```

### Default Credentials
| Resource | Username | Password |
|---|---|---|
| PostgreSQL | `user` | `root` |
| RabbitMQ | `guest` | `guest` |
| MinIO | `minioadmin` | `minioadmin` |
| Default Admin User | `admin001` | `Admin@123` |

---

## Quick Start

> **Important**: Services must be started in order. Eureka must be running before services register, and the API Gateway should be started before sending traffic.

```bash
# 1. Build all modules (skip tests for speed)
.\mvnw.cmd clean package -DskipTests

# 2. Start the Discovery Server FIRST and wait for it to be ready
.\mvnw.cmd spring-boot:run -pl discovery-server
#    → Verify: http://localhost:8761

# 3. Start the API Gateway
.\mvnw.cmd spring-boot:run -pl api-gateway

# 4. Start individual services (order does not matter for these)
.\mvnw.cmd spring-boot:run -pl services/auth-service
.\mvnw.cmd spring-boot:run -pl services/event-management-service
.\mvnw.cmd spring-boot:run -pl services/event-participation-service
.\mvnw.cmd spring-boot:run -pl services/application-service
.\mvnw.cmd spring-boot:run -pl services/platform-management-service
.\mvnw.cmd spring-boot:run -pl services/notification-service
.\mvnw.cmd spring-boot:run -pl services/audit-storage-service
```

> **Tip**: To seed all databases with test data before running, execute the SQL scripts in [`docs/dummy-data/`](./docs/dummy-data/) against each corresponding database.

---

## Environment Variables

Each service supports configuration via environment variables. See individual service READMEs for the full list. Common variables across all services:

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/<db>` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `user` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | DB password |
| `JWT_SECRET` | *(set in application.yml)* | Shared secret for JWT signing — must be identical across all services |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `RABBITMQ_USERNAME` | `guest` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka service URL |

> **Security Note**: Never commit real values for `JWT_SECRET` or database passwords. Use environment-specific config files or a secrets manager in production.

---

## API Gateway Routing

The gateway routes requests based on path prefixes. All routes are prefixed with `/api/v1`:

| Path Pattern | Routed To | Status |
|---|---|---|
| `/api/v1/auth/**` | Auth Service | ✅ Live |
| `/api/v1/templates/**` | Notification Service | ✅ Live |
| `/api/v1/registrations/**`, `/api/v1/qr-sessions/**` | Event Participation Service | ✅ Live |
| `/api/v1/events/**`, `/api/v1/venues/**`, `/api/v1/guest-speakers/**`, `/api/v1/agendas/**` | Event Management Service | ✅ Live |
| `/api/v1/applications/**` | Application Service | ✅ Live |
| `/api/v1/storage/**`, `/api/v1/audit/**` | Audit & Storage Service | ✅ Live |
| `/api/v1/partner-verifications/**`, `/api/v1/vacancy-approvals/**`, `/api/v1/partners/**` | Platform Management Service | ✅ Live |
| `/api/v1/users/**` | User Service | 🔲 Planned |
| `/api/v1/vacancies/**` | Vacancy Service | 🔲 Planned |
| `/api/v1/certificates/**` | Certificate Service | 🔲 Planned |
| `/api/v1/ai/**` | AI Service | 🔲 Planned |

---

## RabbitMQ Overview

All notification events flow through a single **Topic Exchange** called `notification.exchange`.

| Exchange | Type |
|---|---|
| `notification.exchange` | Topic |

### Inbound (Consumed by Notification Service)

| Routing Key | Queue | Published By | Description |
|---|---|---|---|
| `notification.otp` | `otp.queue` | Auth Service | Sends OTP / 2FA emails |
| `notification.reminder` | `reminders.queue` | Event Management Service | Sends event reminder emails |
| `notification.invitation` | `invitation.queue` | Auth / Event Service | Sends onboarding & invitation emails |
| `notification.announcement` | `announcement.queue` | Event Management Service | Sends broadcast announcements |
| `notification.update` | `update.queue` | Application Service | Sends application status change emails |

### Outbound (Published by Notification Service — delivery status callbacks)

| Routing Key | Queue | Consumed By | Description |
|---|---|---|---|
| `notification.status.otp` | `otp.status.queue` | Auth Service | OTP delivery status callback |
| `notification.status.reminder` | `reminder.status.queue` | Event Management Service | Reminder delivery status callback |
| `notification.status.invitation` | `invitation.status.queue` | Auth / Event Service | Invitation delivery status callback |
| `notification.status.announcement` | `announcement.status.queue` | Event Management Service | Announcement delivery status callback |
| `notification.status.update` | `update.status.queue` | Application Service | Update delivery status callback |

### Future Events (Planned)

| Routing Key | Published By | Consumed By | Description |
|---|---|---|---|
| `event.completed` | Event Management | Certificate Service | Triggers auto certificate generation |
| `partner.approved` | Platform Management | Auth + Notification | Activates partner account & sends email |
| `vacancy.created` | Vacancy Service | Platform Management | Creates admin approval task |
| `vacancy.approved` | Platform Management | Vacancy + Notification | Makes vacancy live & notifies matched candidates |

---

## Health Checks

All services expose Spring Boot Actuator endpoints:

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Service health status (UP / DOWN) |
| `GET /actuator/info` | Application version and build info |
| `GET /actuator/metrics` | JVM and HTTP metrics |

- **Direct**: `http://localhost:<port>/actuator/health`
- **Via Eureka Dashboard**: `http://localhost:8761` — shows all registered services and their status
