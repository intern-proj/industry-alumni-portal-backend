# Discovery Server (Eureka)

Service Registry and Discovery Server for the Industry Alumni Portal microservices ecosystem. Built on **Spring Cloud Netflix Eureka Server**, it allows microservices to register dynamically and discover each other without hardcoded IP addresses or ports.

---

## Technical Details

| Property | Value |
|---|---|
| **Port** | `8761` |
| **Technology** | Spring Cloud Netflix Eureka Server |
| **Eureka Dashboard** | `http://localhost:8761` |
| **Service URL** | `http://localhost:8761/eureka/` |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8761` | Eureka server listening port |
| `EUREKA_HOSTNAME` | `localhost` | Hostname for the Eureka instance |

---

## Registered Services Overview

When all backend services are running, the Eureka Dashboard at `http://localhost:8761` will display the following instances:

| Service Name | Default Port | Description |
|---|---|---|
| `API-GATEWAY` | `8080` | Client entry point and router |
| `AUTH-SERVICE` | `8081` | Authentication & user registration |
| `EVENT-MANAGEMENT-SERVICE` | `8082` | Events, venues, speakers, agendas |
| `EVENT-PARTICIPATION-SERVICE` | `8083` | Event registrations & QR sessions |
| `APPLICATION-SERVICE` | `8084` | Job applications & stages |
| `PLATFORM-MANAGEMENT-SERVICE` | `8086` | Partner & vacancy approvals |
| `NOTIFICATION-SERVICE` | `8088` | Email notifications & templates |
| `AUDIT-STORAGE-SERVICE` | `8089` | S3 file storage & audit logs |

---

## Running the Discovery Server

Always start the Discovery Server **first** before launching downstream services so they can register upon startup:

```bash
.\mvnw.cmd spring-boot:run -pl discovery-server
```
