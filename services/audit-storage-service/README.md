# Audit & Storage Service

Centralised file storage and audit logging service. Handles file uploads to an **S3-compatible object store** (AWS S3 or MinIO) and maintains a tamper-evident audit log of all significant platform actions.

---

## Technical Details

| Property | Value |
|---|---|
| **Port** | `8089` |
| **Database** | `audit_storage_db` (PostgreSQL) |
| **Swagger UI** | `http://localhost:8089/swagger-ui.html` |
| **OpenAPI Spec** | `http://localhost:8089/v3/api-docs` |
| **Object Storage** | S3-compatible (AWS S3 or MinIO) |
| **Max File Upload Size** | `50 MB` per file, `55 MB` per request |
| **ID Type** | UUID for all primary keys |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/audit_storage_db` | PostgreSQL connection |
| `SPRING_DATASOURCE_USERNAME` | `user` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | DB password |
| `STORAGE_BUCKET_NAME` | `icu-platform-files` | S3/MinIO bucket name |
| `STORAGE_REGION` | `us-east-1` | S3 region |
| `STORAGE_ENDPOINT` | `http://localhost:9000` | S3/MinIO endpoint URL (use AWS endpoint for production) |
| `STORAGE_ACCESS_KEY` | `minioadmin` | S3/MinIO access key |
| `STORAGE_SECRET_KEY` | `minioadmin` | S3/MinIO secret key |
| `STORAGE_PATH_STYLE_ACCESS` | `true` | Use path-style access (required for MinIO) |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka discovery URL |

> **MinIO for local development:** Run MinIO locally at `http://localhost:9000`. The default credentials `minioadmin/minioadmin` are used. For AWS S3 production, set `STORAGE_PATH_STYLE_ACCESS=false` and provide the correct region/endpoint.

---

## File Types

The `fileType` parameter must be one of these enum values:

| Value | Description |
|---|---|
| `RESUME` | CV/Resume document |
| `SLIDE` | Presentation slide deck |
| `CERTIFICATE` | Certificate document |
| `OTHER` | Any other file |

---

## API Endpoints

### File Storage

---

#### Upload File
**`POST`** `/api/v1/storage/upload`

Uploads a file to S3/MinIO. Requires `multipart/form-data` request.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uploaderId` (query) | string | ✅ | UUID of the user uploading the file |
| `fileType` (query) | string (enum) | ❌ | `RESUME`, `SLIDE`, `CERTIFICATE`, `OTHER` (defaults to `OTHER`) |
| `file` (form-data) | binary | ✅ | The file to upload |

**Example (curl):**
```bash
curl -X POST "http://localhost:8089/api/v1/storage/upload?uploaderId=<user-uuid>&fileType=RESUME" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/resume.pdf"
```

**Response (200 OK):**
```json
{
  "fileId": "123e4567-e89b-12d3-a456-426614174001",
  "originalFilename": "resume.pdf",
  "storageKey": "resumes/resume-123.pdf",
  "storageUrl": "http://localhost:9000/icu-platform-files/resumes/resume-123.pdf",
  "contentType": "application/pdf",
  "fileSizeBytes": 204800,
  "fileType": "RESUME",
  "uploaderId": "user-uuid",
  "uploadTimestamp": "2026-08-17T10:00:00Z",
  "version": 1
}
```

**Response (400 Bad Request):** Missing file or invalid file type.

---

#### List Files by Uploader
**`GET`** `/api/v1/storage`

| Parameter | Type | Required |
|---|---|---|
| `uploaderId` (query) | string | ✅ |

**Response (200 OK):** Array of file metadata objects.

---

#### Get File Metadata
**`GET`** `/api/v1/storage/{id}`

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | UUID (string) | ✅ |

**Response (200 OK):** File metadata object (same structure as Upload response).

**Response (404):** File not found.

---

#### Download File
**`GET`** `/api/v1/storage/download/{id}`

Returns the actual file binary content (or a redirect to the S3 pre-signed URL).

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | UUID (string) | ✅ |

**Response (200 OK):** File binary stream.

**Response (404):** File not found.

---

#### Delete File
**`DELETE`** `/api/v1/storage/{id}`

Removes the file record from the database and deletes the object from S3/MinIO.

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | UUID (string) | ✅ |

**Response (200 OK):** File deleted.

**Response (404):** File not found.

---

### Audit Logging

---

#### Log an Action
**`POST`** `/api/v1/audit/log`

Records an audit event. Other services should call this endpoint to log significant user actions (e.g., login, profile update, approval decision).

**Request Body:**
```json
{
  "userId": "uuid-of-user",
  "action": "PARTNER_APPROVED",
  "ipAddress": "192.168.1.100",
  "resourceType": "PARTNER_VERIFICATION",
  "resourceId": "uuid-of-verification",
  "details": "Partner approved by admin. Organization: Tech Corp Ltd."
}
```

| Field | Type | Required |
|---|---|---|
| `userId` | string | ✅ |
| `action` | string | ✅ |
| `ipAddress` | string (max 45 chars) | ✅ |
| `resourceType` | string (max 100 chars) | ❌ |
| `resourceId` | string (max 100 chars) | ❌ |
| `details` | string (text) | ❌ |

**Response (202 Accepted):** Log recorded asynchronously.

---

#### Get Audit Logs
**`GET`** `/api/v1/audit/logs`

Retrieve paginated audit logs with optional filters.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` (query) | string | ❌ | Filter by user ID |
| `action` (query) | string | ❌ | Filter by action type |
| `from` (query) | ISO datetime string | ❌ | Filter from timestamp |
| `to` (query) | ISO datetime string | ❌ | Filter to timestamp |
| `page` (query) | integer | ❌ | Page number (0-based) |
| `size` (query) | integer | ❌ | Page size |
| `sort` (query) | string | ❌ | Sort field and direction (e.g., `timestamp,desc`) |

> **Note:** The `from` and `to` parameters expect ISO 8601 datetime strings (e.g., `2026-01-01T00:00:00Z`).

**Response (200 OK):** Paginated list of audit log entries.
```json
{
  "content": [
    {
      "id": "123e4567-...",
      "userId": "user-uuid",
      "action": "LOGIN",
      "ipAddress": "192.168.1.1",
      "resourceType": "USER",
      "resourceId": "user-uuid",
      "details": "User logged in",
      "timestamp": "2026-08-17T10:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```
