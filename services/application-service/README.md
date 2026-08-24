# Application Service

Manages job application submissions from alumni, recruitment stage tracking, and application status audits. Each application goes through multiple recruitment stages (e.g., screening, interview, offer).

---

## Technical Details

| Property | Value |
|---|---|
| **Port** | `8084` |
| **Database** | `application_service_db` (PostgreSQL) |
| **Swagger UI** | `http://localhost:8084/swagger-ui.html` |
| **OpenAPI Spec** | `http://localhost:8084/v3/api-docs` |
| **ID Type** | UUID for all primary keys |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/application_service_db` | PostgreSQL connection |
| `SPRING_DATASOURCE_USERNAME` | `user` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | DB password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host (configured, not yet actively used) |
| `RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka discovery URL |

---

## API Endpoints

---

### Applications

---

#### Create Application
**`POST`** `/api/v1/applications`

Submits a new job application from an alumni for a specific vacancy.

**Request Body:**
```json
{
  "vacancyId": "uuid-of-vacancy",
  "alumniId": "uuid-of-alumni",
  "resumeUrl": "https://storage.domain.com/resumes/john-doe.pdf",
  "coverLetter": "I am very interested in this position..."
}
```

| Field | Type | Required |
|---|---|---|
| `vacancyId` | UUID (string) | ✅ |
| `alumniId` | UUID (string) | ✅ |
| `resumeUrl` | string (URL) | ✅ |
| `coverLetter` | string | ❌ |

**Response (200 OK):** Created application object with generated ID and initial status.

---

#### Get Application by ID
**`GET`** `/api/v1/applications/{id}`

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | UUID (string) | ✅ |

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "vacancyId": "abc12345-...",
  "alumniId": "def67890-...",
  "resumeUrl": "https://storage.domain.com/resumes/john-doe.pdf",
  "coverLetter": "...",
  "status": "SUBMITTED",
  "appliedAt": "2026-08-17T10:00:00Z"
}
```

**Response (404):** Application not found.

---

#### Get Applications by Vacancy
**`GET`** `/api/v1/applications/vacancy/{vacancyId}`

Returns all applications for a specific vacancy (used by company/admin).

| Parameter | Type | Required |
|---|---|---|
| `vacancyId` (path) | UUID (string) | ✅ |

**Response (200 OK):** Array of application objects.

---

#### Get Applications by Alumni
**`GET`** `/api/v1/applications/alumni/{alumniId}`

Returns all applications submitted by a specific alumni user.

| Parameter | Type | Required |
|---|---|---|
| `alumniId` (path) | UUID (string) | ✅ |

**Response (200 OK):** Array of application objects.

---

#### Update Application Status
**`PUT`** `/api/v1/applications/{id}/status`

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | UUID (string) | ✅ |

**Request Body:**
```json
{
  "newStatus": "UNDER_REVIEW",
  "changedBy": "uuid-of-reviewer",
  "changeReason": "Application meets the minimum criteria"
}
```

| Field | Type | Required |
|---|---|---|
| `newStatus` | string (enum) | ✅ |
| `changedBy` | UUID (string) | ✅ |
| `changeReason` | string | ❌ |

> Common status values: `SUBMITTED`, `UNDER_REVIEW`, `SHORTLISTED`, `REJECTED`, `OFFER_EXTENDED`, `HIRED`, `WITHDRAWN`

**Response (200 OK):** Updated application object.

---

#### Get Status Audit Trail
**`GET`** `/api/v1/applications/{id}/audits`

Returns the full history of all status changes for an application.

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | UUID (string) | ✅ |

**Response (200 OK):** Array of audit entries showing status, who changed it, and when.

---

### Recruitment Stages

Recruitment stages track individual steps in the hiring pipeline for an application (e.g., phone screening, technical interview, HR interview).

---

#### Get Stages for Application
**`GET`** `/api/v1/applications/{id}/stages`

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | UUID (string) | ✅ |

**Response (200 OK):** Array of recruitment stage objects with status and scheduling info.

---

#### Schedule a Stage
**`POST`** `/api/v1/applications/{id}/stages`

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | UUID (string) | ✅ |

**Request Body:**
```json
{
  "stageName": "Technical Interview",
  "scheduledAt": "2026-09-01T10:00:00Z",
  "interviewerName": "Dr. Jane Smith"
}
```

| Field | Type | Required |
|---|---|---|
| `stageName` | string | ✅ |
| `scheduledAt` | ISO datetime (string) | ✅ |
| `interviewerName` | string | ❌ |

**Response (200 OK):** Created recruitment stage object.

---

#### Update Stage
**`PUT`** `/api/v1/applications/{id}/stages/{stageId}`

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | UUID (string) | ✅ |
| `stageId` (path) | UUID (string) | ✅ |

**Request Body:**
```json
{
  "stageStatus": "COMPLETED",
  "score": 85,
  "feedback": "Strong technical knowledge. Proceed to HR round."
}
```

| Field | Type | Required |
|---|---|---|
| `stageStatus` | string (enum) | ✅ |
| `score` | number | ❌ |
| `feedback` | string | ❌ |

**Response (200 OK):** Updated stage object.
