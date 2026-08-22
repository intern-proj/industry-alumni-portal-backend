# Notification Service

Centralized email notification service for the Industry Alumni Portal. It does **not** expose notification-sending APIs directly — all notifications are triggered asynchronously via **RabbitMQ** messages published by other services. It also provides REST endpoints for managing reusable email **templates**.

---

## Technical Details

| Property | Value |
|---|---|
| **Port** | `8088` |
| **Database** | `notification` (PostgreSQL) |
| **Swagger UI** | `http://localhost:8088/swagger-ui.html` |
| **OpenAPI Spec** | `http://localhost:8088/v3/api-docs` |
| **Email Provider** | Gmail SMTP (`smtp.gmail.com:587`) |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/notification` | PostgreSQL connection |
| `SPRING_DATASOURCE_USERNAME` | `user` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | DB password |
| `SPRING_MAIL_HOST` | `smtp.gmail.com` | SMTP host |
| `SPRING_MAIL_PORT` | `587` | SMTP port |
| `SPRING_MAIL_USERNAME` | (dev email) | SMTP sender email |
| `SPRING_MAIL_PASSWORD` | (dev app password) | SMTP app password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `RABBITMQ_USERNAME` | `guest` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka discovery URL |

---

## RabbitMQ Integration

**This service CONSUMES from RabbitMQ and publishes status callbacks back.** All queues are **durable**.

**Exchange:** `notification.exchange` (Type: `Topic`)

### Inbound Queues (Service Consumes)

#### `otp.queue` — Routing Key: `notification.otp`
Triggers an OTP email to a single recipient.

**Message Payload (`OtpEmailDTO`):**
```json
{
  "toEmail": "student@nsbm.ac.lk",
  "otpCode": "123456"
}
```

| Field | Type | Constraints |
|---|---|---|
| `toEmail` | string (email) | Required, valid email |
| `otpCode` | string | Required, exactly 6 digits |

**Status Callback:** Publishes to routing key `notification.status.otp` → `otp.status.queue`

---

#### `invitation.queue` — Routing Key: `notification.invitation`
Sends an event invitation email to a recipient.

**Message Payload (`InvitationEmailDTO`):**
```json
{
  "toEmail": "guest@example.com",
  "inviteeName": "John Doe",
  "eventName": "Annual Tech Summit 2026",
  "eventDate": "2026-09-15",
  "eventLocation": "NSBM Green University",
  "eventDescription": "A summit about the latest tech trends.",
  "rsvpLink": "https://portal.domain.com/rsvp/123",
  "organizerName": "ICU Platform Team"
}
```

| Field | Type | Required |
|---|---|---|
| `toEmail` | string (email) | ✅ |
| `inviteeName` | string | ✅ |
| `eventName` | string | ✅ |
| `eventDate` | string | ✅ |
| `eventLocation` | string | ❌ |
| `eventDescription` | string | ❌ |
| `rsvpLink` | string | ❌ |
| `organizerName` | string | ❌ |

**Status Callback:** Publishes to routing key `notification.status.invitation` → `invitation.status.queue`

---

#### `announcement.queue` — Routing Key: `notification.announcement`
Sends a broadcast announcement email to multiple recipients.

**Message Payload (`AnnouncementEmailDTO`):**
```json
{
  "toEmails": ["user1@nsbm.ac.lk", "user2@nsbm.ac.lk"],
  "announcementTitle": "Portal Maintenance Notice",
  "announcementBody": "The portal will be down for maintenance on Saturday.",
  "senderName": "ICU Admin",
  "portalUrl": "https://portal.domain.com"
}
```

| Field | Type | Required |
|---|---|---|
| `toEmails` | string[] | ✅ (non-empty, all valid emails) |
| `announcementTitle` | string | ✅ |
| `announcementBody` | string | ✅ |
| `senderName` | string | ✅ |
| `portalUrl` | string | ❌ |

**Status Callback:** Publishes to routing key `notification.status.announcement` → `announcement.status.queue`

---

#### `reminders.queue` — Routing Key: `notification.reminder`
Sends a reminder email for events, deadlines, or general notices.

**Message Payload (`ReminderEmailDTO`):**
```json
{
  "toEmail": "student@nsbm.ac.lk",
  "recipientName": "Jane Doe",
  "reminderType": "EVENT",
  "subject": "Reminder: Tech Summit Tomorrow",
  "reminderBody": "Don't forget the Tech Summit is tomorrow at 9 AM.",
  "dueDate": "2026-09-15",
  "actionLink": "https://portal.domain.com/events/1"
}
```

| Field | Type | Required | Allowed Values |
|---|---|---|---|
| `toEmail` | string (email) | ✅ | Valid email |
| `recipientName` | string | ✅ | — |
| `reminderType` | enum | ✅ | `EVENT`, `DEADLINE`, `GENERAL` |
| `subject` | string | ✅ | — |
| `reminderBody` | string | ✅ | — |
| `dueDate` | string | ❌ | — |
| `actionLink` | string | ❌ | — |

**Status Callback:** Publishes to routing key `notification.status.reminder` → `reminder.status.queue`

---

#### `update.queue` — Routing Key: `notification.update`
Sends a status update email (e.g., profile approved, application status changed).

**Message Payload (`UpdateEmailDTO`):**
```json
{
  "toEmail": "partner@company.com",
  "recipientName": "Jane Smith",
  "updateType": "PROFILE_APPROVED",
  "updateBody": "Your partner profile has been approved. You can now post vacancies.",
  "actionLink": "https://portal.domain.com/dashboard"
}
```

| Field | Type | Required | Allowed Values |
|---|---|---|---|
| `toEmail` | string (email) | ✅ | Valid email |
| `recipientName` | string | ✅ | — |
| `updateType` | enum | ✅ | `PROFILE_APPROVED`, `JOB_POSTED`, `APPLICATION_UPDATE`, `GENERAL_UPDATE` |
| `updateBody` | string | ✅ | — |
| `actionLink` | string | ❌ | — |

**Status Callback:** Publishes to routing key `notification.status.update` → `update.status.queue`

---

### Status Callback Queues (Outbound)

After processing each notification, the service publishes a delivery status back to the exchange:

| Routing Key | Queue | Payload |
|---|---|---|
| `notification.status.otp` | `otp.status.queue` | `{ "toEmail": "...", "status": true/false, "error": "..." }` |
| `notification.status.reminder` | `reminder.status.queue` | Same as above |
| `notification.status.invitation` | `invitation.status.queue` | Same as above |
| `notification.status.announcement` | `announcement.status.queue` | Same as above |
| `notification.status.update` | `update.status.queue` | Same as above |

---

## API Endpoints (Template Management)

Templates are reusable email content objects stored in the database.

---

### Get All Templates
**`GET`** `/api/v1/templates`

Returns a list of all notification templates.

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "templateCode": "OTP_EMAIL",
    "name": "OTP Email",
    "subject": "Your OTP Code",
    "body": "Your OTP is {{otpCode}}. It expires in 5 minutes.",
    "description": "Template for OTP emails",
    "createdAt": "2026-08-17T10:00:00",
    "updatedAt": "2026-08-17T10:00:00"
  }
]
```

---

### Get Template by ID
**`GET`** `/api/v1/templates/{id}`

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | integer (int64) | ✅ |

**Response (200 OK):** Single `NotificationTemplateDTO` object (see structure above).

---

### Get Template by Code
**`GET`** `/api/v1/templates/code/{templateCode}`

| Parameter | Type | Required |
|---|---|---|
| `templateCode` (path) | string | ✅ |

**Response (200 OK):** Single `NotificationTemplateDTO` object.

---

### Create Template
**`POST`** `/api/v1/templates`

**Request Body:**
```json
{
  "templateCode": "WELCOME_EMAIL",
  "name": "Welcome Email",
  "subject": "Welcome to the Portal!",
  "body": "Hello {{name}}, welcome to the Industry Alumni Portal.",
  "description": "Sent on successful registration"
}
```

| Field | Type | Required |
|---|---|---|
| `templateCode` | string | ✅ (unique) |
| `name` | string | ✅ |
| `subject` | string | ✅ |
| `body` | string | ✅ |
| `description` | string | ❌ |

**Response (200 OK):** Created `NotificationTemplateDTO`.

---

### Update Template
**`PUT`** `/api/v1/templates/{id}`

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | integer (int64) | ✅ |

**Request Body:** Same fields as Create Template.

**Response (200 OK):** Updated `NotificationTemplateDTO`.

---

### Delete Template
**`DELETE`** `/api/v1/templates/{id}`

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | integer (int64) | ✅ |

**Response (200 OK):** Template deleted.
