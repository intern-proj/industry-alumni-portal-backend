# Event Participation Service

Manages student event registrations and QR code-based attendance tracking. Each event can have QR sessions, and registrations can be marked through QR scan verification.

---

## Technical Details

| Property | Value |
|---|---|
| **Port** | `8083` |
| **Database** | `event_participation_db` (PostgreSQL) |
| **Swagger UI** | `http://localhost:8083/swagger-ui.html` |
| **OpenAPI Spec** | `http://localhost:8083/v3/api-docs` |
| **ID Type** | UUID (`java.util.UUID`) for all primary keys |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `EVENT_PARTICIPATION_SERVICE_PORT` | `8083` | Server port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/event_participation_db` | PostgreSQL connection |
| `SPRING_DATASOURCE_USERNAME` | `user` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | DB password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka discovery URL |

---

## RabbitMQ Integration

This service connects to RabbitMQ but does **not** currently publish or consume any queues by default. The connection is configured for future use.

---

## API Endpoints

> **Note:** All `id` / `registrationId` / `qrId` parameters are **UUID** strings (e.g., `123e4567-e89b-12d3-a456-426614174000`), and `eventId` in registrations is also a UUID.

---

### Registrations

---

#### Get All Registrations
**`GET`** `/api/v1/registrations`

| Parameter | Type | Required | Description |
|---|---|---|---|
| `eventId` (query) | UUID (string) | ❌ | Filter by event ID |

**Response (200 OK):** Array of registration objects.

---

#### Get Registration by ID
**`GET`** `/api/v1/registrations/{registrationId}`

| Parameter | Type | Required |
|---|---|---|
| `registrationId` (path) | UUID (string) | ✅ |

**Response (200 OK):**
```json
{
  "registrationId": "123e4567-e89b-12d3-a456-426614174000",
  "eventId": "456e7890-e89b-12d3-a456-426614174001",
  "studentId": "789e1234-e89b-12d3-a456-426614174002",
  "status": "REGISTERED",
  "registeredAt": "2026-08-17T10:00:00Z"
}
```

**Response (404):** Registration not found.

---

#### Create Registration
**`POST`** `/api/v1/registrations`

**Request Body:**
```json
{
  "eventId": "456e7890-e89b-12d3-a456-426614174001",
  "studentId": "789e1234-e89b-12d3-a456-426614174002"
}
```

| Field | Type | Required |
|---|---|---|
| `eventId` | UUID (string) | ✅ |
| `studentId` | UUID (string) | ✅ |

**Response (200 OK):** Created registration object.

---

#### Update Registration Status
**`PATCH`** `/api/v1/registrations/{registrationId}/status`

| Parameter | Type | Required |
|---|---|---|
| `registrationId` (path) | UUID (string) | ✅ |

**Request Body:**
```json
{
  "status": "ATTENDED"
}
```

> Typical status values: `REGISTERED`, `ATTENDED`, `CANCELLED`, `NO_SHOW`

**Response (200 OK):** Updated registration object.

---

#### Delete Registration
**`DELETE`** `/api/v1/registrations/{registrationId}`

**Response (200 OK):** Registration deleted.

---

### QR Sessions

QR sessions are time-limited tokens attached to an event. Students scan these QR codes to mark attendance.

---

#### Get QR Sessions by Event
**`GET`** `/api/v1/events/{eventId}/qr-sessions`

| Parameter | Type | Required |
|---|---|---|
| `eventId` (path) | UUID (string) | ✅ |

**Response (200 OK):** Array of QR session objects.

---

#### Generate QR Session for Event
**`POST`** `/api/v1/events/{eventId}/qr-sessions`

| Parameter | Type | Required |
|---|---|---|
| `eventId` (path) | UUID (string) | ✅ |

**Request Body:**
```json
{
  "validForMinutes": 30
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `validForMinutes` | integer | ✅ | How long the QR session remains valid |

**Response (200 OK):** Created QR session with a unique QR code value.

---

#### Get QR Session by ID
**`GET`** `/api/v1/qr-sessions/{qrId}`

| Parameter | Type | Required |
|---|---|---|
| `qrId` (path) | UUID (string) | ✅ |

**Response (200 OK):** QR session details including expiry time and status.

---

#### Verify QR Code (Mark Attendance)
**`GET`** `/api/v1/qr-sessions/{qrCodeValue}/verify`

Verifies a scanned QR code value and returns whether it is currently valid.

| Parameter | Type | Required |
|---|---|---|
| `qrCodeValue` (path) | string | ✅ |

**Response (200 OK):** Verification result (valid/expired).

---

#### Deactivate QR Session
**`DELETE`** `/api/v1/qr-sessions/{qrId}`

Manually deactivates/deletes a QR session before it expires.

| Parameter | Type | Required |
|---|---|---|
| `qrId` (path) | UUID (string) | ✅ |

**Response (200 OK):** QR session deactivated.
