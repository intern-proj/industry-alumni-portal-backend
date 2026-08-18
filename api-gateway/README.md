# API Gateway

The API Gateway is the single entry point for all client requests entering the Industry Alumni Portal backend. Built on **Spring Cloud Gateway (Reactive / WebFlux)**, it handles dynamic routing, client load balancing via Eureka, global CORS policies, and centralized OpenAPI/Swagger UI documentation aggregation.

---

## Technical Details

| Property | Value |
|---|---|
| **Port** | `8080` |
| **Technology** | Spring Cloud Gateway (WebFlux / Netty) |
| **Aggregated Swagger UI** | `http://localhost:8080/swagger-ui.html` |
| **Actuator Gateway Endpoint** | `http://localhost:8080/actuator/gateway/routes` |
| **Discovery Client** | Netflix Eureka Client |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | Gateway listening port |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka Service Discovery URL |

---

## Routing Table

All external API routes are prefixed with `/api/v1` and dispatched dynamically to downstream microservices registered in Eureka:

| Route ID | Path Matcher | Target Service (Eureka) | Description |
|---|---|---|---|
| `auth-service` | `/api/v1/auth/**` | `lb://auth-service` | Authentication, staff registration, partner onboarding |
| `notification-service` | `/api/v1/templates/**` | `lb://notification-service` | Email notification template management |
| `event-participation-service` | `/api/v1/registrations/**`<br>`/api/v1/qr-sessions/**`<br>`/api/v1/events/*/qr-sessions/**` | `lb://event-participation-service` | Student event registrations and QR attendance sessions |
| `event-management-service` | `/api/v1/events/**`<br>`/api/v1/venues/**`<br>`/api/v1/guest-speakers/**`<br>`/api/v1/agendas/**` | `lb://event-management-service` | Events, venue bookings, speakers, and agenda schedules |
| `application-service` | `/api/v1/applications/**` | `lb://application-service` | Job applications and recruitment stage tracking |
| `audit-storage-service` | `/api/v1/storage/**`<br>`/api/v1/audit/**` | `lb://audit-storage-service` | File upload/download and centralized audit logging |
| `platform-management-service` | `/api/v1/partner-verifications/**`<br>`/api/v1/vacancy-approvals/**`<br>`/api/v1/admin/**`<br>`/api/v1/internal/**` | `lb://platform-management-service` | Admin verification & vacancy approval workflows |

---

## Swagger / OpenAPI Aggregation

The Gateway aggregates the OpenAPI documentation from each downstream service into a single unified Swagger UI dropdown.

Access the aggregated documentation at:
**`http://localhost:8080/swagger-ui.html`**

| Service Tab in Swagger UI | Proxied URL |
|---|---|
| **Auth Service** | `/services-docs/auth-service` |
| **Event Management Service** | `/services-docs/event-management-service` |
| **Event Participation Service** | `/services-docs/event-participation-service` |
| **Application Service** | `/services-docs/application-service` |
| **Notification Service** | `/services-docs/notification-service` |
| **Audit & Storage Service** | `/services-docs/audit-storage-service` |
| **Platform Management Service** | `/services-docs/platform-management-service` |

---

## Global CORS Configuration

CORS is globally handled at the Gateway level so frontend clients can communicate seamlessly:
- **Allowed Origins:** `*` (configured with `allowCredentials: true`)
- **Allowed Methods:** `GET, POST, PUT, DELETE, OPTIONS, PATCH`
- **Allowed Headers:** `*`
- **Response Header Deduplication:** Deduplicates `Access-Control-Allow-Origin` and `Access-Control-Allow-Credentials` headers to avoid duplicate header conflicts from downstream services.
