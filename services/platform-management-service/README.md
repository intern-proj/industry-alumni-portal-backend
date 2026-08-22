# Platform Management Service

Admin-facing service for managing partner verification workflows and job vacancy approval workflows. It operates with two separate access tiers:

- **`/internal/...`** — Called by other microservices to submit new verification/approval requests
- **`/admin/...`** — Called by admin users to review, claim, and make decisions on requests
- **`/partner-verifications/...`** (no prefix) — Called by partners to upload supporting documents

---

## Technical Details

| Property | Value |
|---|---|
| **Port** | `8086` |
| **Database** | `platform_management_db` (PostgreSQL) |
| **Swagger UI** | `http://localhost:8086/swagger-ui.html` |
| **OpenAPI Spec** | `http://localhost:8086/v3/api-docs` |
| **DB Schema Management** | Flyway migrations (`classpath:db/migration`) |
| **ID Type** | UUID for all primary keys |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8086` | Server port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/platform_management_db` | PostgreSQL connection |
| `DB_USERNAME` | `user` | DB username |
| `DB_PASSWORD` | `root` | DB password |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka discovery URL |

---

## Workflow Overview

### Partner Verification Workflow
```
Other Service → POST /internal/partner-verifications  (creates PENDING_DOCUMENTS)
Partner       → POST /partner-verifications/{id}/documents  (uploads supporting docs)
Partner       → POST /partner-verifications/{id}/submit-for-review  (moves to PENDING_REVIEW)
Admin         → POST /admin/partner-verifications/{id}/claim  (moves to UNDER_REVIEW, assigns reviewer)
Admin         → POST /admin/partner-verifications/{id}/decision  (APPROVED or REJECTED)
```

### Vacancy Approval Workflow
```
Other Service → POST /internal/vacancy-approvals  (creates PENDING_REVIEW)
Admin         → POST /admin/vacancy-approvals/{id}/claim  (assigns reviewer)
Admin         → POST /admin/vacancy-approvals/{id}/decision  (APPROVED or REJECTED)
```

---

## Status Enums

### Partner Verification Status
| Value | Description |
|---|---|
| `PENDING_DOCUMENTS` | Waiting for partner to upload documents |
| `PENDING_REVIEW` | Documents submitted, waiting for admin to pick up |
| `UNDER_REVIEW` | Admin has claimed and is reviewing |
| `MORE_INFO_REQUIRED` | Admin requested additional information |
| `APPROVED` | Verification approved |
| `REJECTED` | Verification rejected |

### Vacancy Approval Status
(Similar flow — check OpenAPI spec at `/v3/api-docs` for full enum values)

---

## API Endpoints

### Internal (Service-to-Service)

---

#### Submit Partner Verification Request
**`POST`** `/api/v1/internal/partner-verifications`

Called by other services (e.g., Auth Service) when a partner registration is approved and needs platform verification.

**Request Body:**
```json
{
  "userId": "uuid-of-partner-user",
  "organizationNameSnapshot": "Tech Corp Ltd",
  "contactEmailSnapshot": "jane@techcorp.com"
}
```

| Field | Type | Required |
|---|---|---|
| `userId` | UUID (string) | ✅ |
| `organizationNameSnapshot` | string | ✅ |
| `contactEmailSnapshot` | string (email) | ✅ |

**Response (200 OK):** Created verification record with status `PENDING_DOCUMENTS`.

---

#### Submit Vacancy Approval Request
**`POST`** `/api/v1/internal/vacancy-approvals`

Called by other services when a company posts a vacancy and it requires admin approval.

**Request Body:**
```json
{
  "vacancyId": "uuid-of-vacancy",
  "companyUserId": "uuid-of-company-user",
  "submittedByUserId": "uuid-of-submitter",
  "vacancyTitleSnapshot": "Software Engineer",
  "companyNameSnapshot": "Tech Corp Ltd"
}
```

| Field | Type | Required |
|---|---|---|
| `vacancyId` | UUID (string) | ✅ |
| `companyUserId` | UUID (string) | ✅ |
| `submittedByUserId` | UUID (string) | ✅ |
| `vacancyTitleSnapshot` | string | ✅ |
| `companyNameSnapshot` | string | ✅ |

**Response (200 OK):** Created approval record.

---

### Partner-Facing

---

#### List Documents for Verification
**`GET`** `/api/v1/partner-verifications/{verificationId}/documents`

| Parameter | Type | Required |
|---|---|---|
| `verificationId` (path) | UUID (string) | ✅ |

**Response (200 OK):** Array of document records.

---

#### Add Document to Verification
**`POST`** `/api/v1/partner-verifications/{verificationId}/documents`

Partners upload document references (the actual file must first be uploaded via the Audit & Storage Service).

| Parameter | Type | Required |
|---|---|---|
| `verificationId` (path) | UUID (string) | ✅ |

**Request Body:**
```json
{
  "documentType": "BUSINESS_REGISTRATION",
  "storageFileId": "uuid-of-file-in-storage-service",
  "originalFilename": "business_reg.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 204800
}
```

| Field | Type | Required |
|---|---|---|
| `documentType` | string | ✅ |
| `storageFileId` | UUID (string) | ✅ |
| `originalFilename` | string | ✅ |
| `contentType` | string | ❌ |
| `sizeBytes` | long | ❌ |

**Response (200 OK):** Added document record.

---

#### Delete Document from Verification
**`DELETE`** `/api/v1/partner-verifications/{verificationId}/documents/{documentId}`

---

#### Submit Verification for Review
**`POST`** `/api/v1/partner-verifications/{verificationId}/submit-for-review`

Moves the verification status from `PENDING_DOCUMENTS` to `PENDING_REVIEW`. Call this after all documents are uploaded.

| Parameter | Type | Required |
|---|---|---|
| `verificationId` (path) | UUID (string) | ✅ |

**Response (200 OK):** Updated verification record.

---

### Admin

---

#### List Partner Verifications
**`GET`** `/api/v1/admin/partner-verifications`

| Parameter | Type | Required | Description |
|---|---|---|---|
| `status` (query) | string (enum) | ✅ | Filter by status (e.g., `PENDING_REVIEW`) |
| `page` / `size` / `sort` (query) | Pageable | ✅ | Pagination |

**Response (200 OK):** Paginated list of partner verification records.

---

#### Get Partner Verification by ID
**`GET`** `/api/v1/admin/partner-verifications/{id}`

---

#### Edit Partner Verification (Admin)
**`PATCH`** `/api/v1/admin/partner-verifications/{id}`

**Request Body:**
```json
{
  "organizationNameSnapshot": "Tech Corp Updated",
  "contactEmailSnapshot": "new@techcorp.com",
  "decisionNotes": "Updated after additional review",
  "rejectionReason": null,
  "reviewedByUserId": "uuid-of-reviewer",
  "actingUserId": "uuid-of-admin"
}
```

---

#### Claim Verification (Assign Reviewer)
**`POST`** `/api/v1/admin/partner-verifications/{id}/claim`

Assigns the verification to a specific reviewer and moves it to `UNDER_REVIEW`.

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | UUID (string) | ✅ |
| `reviewerId` (query) | UUID (string) | ✅ |

---

#### Make Decision on Verification
**`POST`** `/api/v1/admin/partner-verifications/{id}/decision`

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | UUID (string) | ✅ |

**Request Body:**
```json
{
  "decision": "APPROVED",
  "decisionNotes": "All documents verified and valid.",
  "rejectionReason": null,
  "actingUserId": "uuid-of-admin"
}
```

| Field | Type | Required | Allowed Values |
|---|---|---|---|
| `decision` | string (enum) | ✅ | `APPROVED`, `REJECTED`, `MORE_INFO_REQUIRED` |
| `decisionNotes` | string | ❌ | — |
| `rejectionReason` | string | ❌ | Required if `decision` is `REJECTED` |
| `actingUserId` | UUID (string) | ✅ | — |

---

#### Get Verification History
**`GET`** `/api/v1/admin/partner-verifications/{id}/history`

Returns the full status change history (approval status history log).

---

#### List Vacancy Approvals
**`GET`** `/api/v1/admin/vacancy-approvals`

| Parameter | Type | Required |
|---|---|---|
| `status` (query) | string (enum) | ✅ |
| Pageable params | — | ✅ |

---

#### Get Vacancy Approval by ID
**`GET`** `/api/v1/admin/vacancy-approvals/{id}`

---

#### Claim Vacancy Approval
**`POST`** `/api/v1/admin/vacancy-approvals/{id}/claim`

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | UUID | ✅ |
| `reviewerId` (query) | UUID | ✅ |

---

#### Make Decision on Vacancy Approval
**`POST`** `/api/v1/admin/vacancy-approvals/{id}/decision`

**Request Body:**
```json
{
  "decision": "APPROVED",
  "decisionNotes": "Vacancy meets all platform guidelines.",
  "rejectionReason": null,
  "actingUserId": "uuid-of-admin"
}
```

---

#### Get Vacancy Approval History
**`GET`** `/api/v1/admin/vacancy-approvals/{id}/history`

---

#### Edit Vacancy Approval (Admin)
**`PATCH`** `/api/v1/admin/vacancy-approvals/{id}`

**Request Body:**
```json
{
  "vacancyTitleSnapshot": "Senior Engineer",
  "companyNameSnapshot": "Tech Corp",
  "decisionNotes": "...",
  "rejectionReason": null,
  "assignedReviewerId": "uuid",
  "actingUserId": "uuid-of-admin"
}
```
