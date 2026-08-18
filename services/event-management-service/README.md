# Event Management Service

Manages all aspects of event lifecycle including creating and scheduling events, booking venues, managing guest speakers, and building event agendas.

---

## Technical Details

| Property | Value |
|---|---|
| **Port** | `8082` |
| **Database** | `event_management_db` (PostgreSQL) |
| **Swagger UI** | `http://localhost:8082/swagger-ui.html` |
| **OpenAPI Spec** | `http://localhost:8082/v3/api-docs` |
| **DB Schema Management** | Flyway migrations (`classpath:db/migration`) |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `EVENT_MANAGEMENT_SERVICE_PORT` | `8082` | Server port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/event_management_db` | PostgreSQL connection |
| `SPRING_DATASOURCE_USERNAME` | `user` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | DB password |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka discovery URL |

---

## Event Status Lifecycle

Events follow a strict status transition model:

```
DRAFT → SCHEDULED → ONGOING → COMPLETED
         ↓              ↓
       RESCHEDULED   CANCELLED
         ↓
       SCHEDULED / ONGOING / CANCELLED
```

| Transition | Allowed |
|---|---|
| `DRAFT` → `SCHEDULED`, `CANCELLED` | ✅ |
| `SCHEDULED` → `ONGOING`, `RESCHEDULED`, `CANCELLED` | ✅ |
| `RESCHEDULED` → `SCHEDULED`, `ONGOING`, `CANCELLED` | ✅ |
| `ONGOING` → `COMPLETED`, `CANCELLED` | ✅ |
| `COMPLETED` → (any) | ❌ (terminal) |
| `CANCELLED` → (any) | ❌ (terminal) |

---

## API Endpoints

### Venues

---

#### Get All Venues
**`GET`** `/api/v1/venues`

Returns a list of all venues.

**Response (200 OK):** Array of `VenueResponse`.

---

#### Get Venue by ID
**`GET`** `/api/v1/venues/{id}`

| Parameter | Type | Required |
|---|---|---|
| `id` (path) | integer (int64) | ✅ |

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Main Auditorium",
  "address": "NSBM Green University, Pittugala",
  "capacity": 500,
  "venueType": "AUDITORIUM",
  "contactInfo": "venue@nsbm.ac.lk",
  "onlineMeetingLink": null
}
```

**Response (404):** Venue not found.

---

#### Create Venue
**`POST`** `/api/v1/venues`

**Request Body:**
```json
{
  "name": "Conference Room A",
  "address": "Block B, NSBM",
  "capacity": 50,
  "venueType": "CONFERENCE_ROOM",
  "contactInfo": "admin@nsbm.ac.lk",
  "onlineMeetingLink": "https://meet.google.com/abc-def-ghi"
}
```

**Response (200 OK):** Created `VenueResponse`.

---

#### Update Venue
**`PUT`** `/api/v1/venues/{id}`

Same body as Create Venue. `id` is required in path.

**Response (200 OK):** Updated `VenueResponse`. **Response (404):** Venue not found.

---

#### Delete Venue
**`DELETE`** `/api/v1/venues/{id}`

**Response (200 OK):** Venue deleted. **Response (404):** Venue not found.

---

### Guest Speakers

---

#### Get All Speakers
**`GET`** `/api/v1/guest-speakers`

| Parameter | Type | Required |
|---|---|---|
| `organizationId` (query) | integer | ❌ (filter by org) |

**Response (200 OK):** Array of `GuestSpeakerResponse`.

---

#### Get Speaker by ID
**`GET`** `/api/v1/guest-speakers/{id}`

**Response (200 OK):**
```json
{
  "id": 1,
  "fullName": "Dr. Jane Smith",
  "title": "Professor",
  "bio": "Expert in AI and Machine Learning.",
  "email": "jane.smith@example.com",
  "phone": "+94771234567",
  "photoUrl": "https://example.com/photo.jpg",
  "organizationId": 10
}
```

**Response (404):** Speaker not found.

---

#### Create Speaker
**`POST`** `/api/v1/guest-speakers`

**Request Body:**
```json
{
  "fullName": "Dr. Jane Smith",
  "title": "Professor",
  "bio": "Expert in AI and Machine Learning.",
  "email": "jane.smith@example.com",
  "phone": "+94771234567",
  "photoUrl": "https://example.com/photo.jpg",
  "organizationId": 10
}
```

**Response (200 OK):** Created `GuestSpeakerResponse`.

---

#### Update Speaker
**`PUT`** `/api/v1/guest-speakers/{id}`

Same body as Create Speaker.

---

#### Delete Speaker
**`DELETE`** `/api/v1/guest-speakers/{id}`

---

### Events

---

#### Get All Events
**`GET`** `/api/v1/events`

| Parameter | Type | Required | Description |
|---|---|---|---|
| `status` (query) | string (enum) | ❌ | Filter: `DRAFT`, `SCHEDULED`, `ONGOING`, `COMPLETED`, `CANCELLED`, `RESCHEDULED` |
| `venueId` (query) | integer | ❌ | Filter by venue |
| `coordinatorUserId` (query) | integer | ❌ | Filter by coordinator |

---

#### Get Event by ID
**`GET`** `/api/v1/events/{id}`

**Response (200 OK):**
```json
{
  "id": 1,
  "title": "Annual Tech Summit",
  "description": "A summit about tech trends",
  "eventType": "SEMINAR",
  "status": "SCHEDULED",
  "startDateTime": "2026-09-15T09:00:00",
  "endDateTime": "2026-09-15T17:00:00",
  "venueId": 1,
  "organizationId": null,
  "coordinatorUserId": null,
  "coordinatorName": null,
  "coordinatorEmail": null,
  "createdAt": "2026-08-17T10:00:00",
  "updatedAt": "2026-08-17T10:00:00"
}
```

**Response (404):** Event not found.

---

#### Create Event
**`POST`** `/api/v1/events`

**Request Body:**
```json
{
  "title": "Annual Tech Summit",
  "description": "A summit about tech trends",
  "eventType": "SEMINAR",
  "startDateTime": "2026-09-15T09:00:00",
  "endDateTime": "2026-09-15T17:00:00",
  "venueId": 1,
  "organizationId": null
}
```

> New events are always created with status `DRAFT`.

**Response (200 OK):** Created `EventResponse`.

---

#### Update Event
**`PUT`** `/api/v1/events/{id}`

**Request Body:** Same as Create Event (all fields optional — partial updates supported).

---

#### Delete Event
**`DELETE`** `/api/v1/events/{id}`

---

#### Update Event Status
**`PATCH`** `/api/v1/events/{id}/status`

**Request Body:**
```json
{
  "status": "SCHEDULED"
}
```

> Must follow the allowed status transition rules. Returns `409 Conflict` for illegal transitions.

---

#### Reschedule Event
**`PATCH`** `/api/v1/events/{id}/reschedule`

Transitions status to `RESCHEDULED` and updates dates.

**Request Body:**
```json
{
  "newStartDateTime": "2026-10-01T09:00:00",
  "newEndDateTime": "2026-10-01T17:00:00",
  "reason": "Venue unavailable on original date"
}
```

---

#### Cancel Event
**`PATCH`** `/api/v1/events/{id}/cancel`

Transitions status to `CANCELLED`. No request body required.

---

#### Assign Coordinator
**`POST`** `/api/v1/events/{id}/coordinator`

**Request Body:**
```json
{
  "coordinatorUserId": 5,
  "coordinatorName": "John Doe",
  "coordinatorEmail": "john.doe@nsbm.ac.lk"
}
```

---

#### Remove Coordinator
**`DELETE`** `/api/v1/events/{id}/coordinator`

Clears the coordinator fields from the event.

---

### Agendas

---

#### Get Agenda Items
**`GET`** `/api/v1/agendas`

| Parameter | Type | Required |
|---|---|---|
| `eventId` (query) | integer | ❌ |
| `speakerId` (query) | integer | ❌ |

---

#### Get Agenda by Event (alternative)
**`GET`** `/api/v1/agendas/event/{eventId}`

| Parameter | Type | Required |
|---|---|---|
| `eventId` (path) | integer | ✅ |

---

#### Get Agenda Item by ID
**`GET`** `/api/v1/agendas/{id}`

---

#### Create Agenda Item
**`POST`** `/api/v1/agendas`

**Request Body:**
```json
{
  "eventId": 1,
  "title": "Opening Keynote",
  "description": "Welcome address by the dean",
  "speakerId": 1,
  "startTime": "2026-09-15T09:00:00",
  "endTime": "2026-09-15T09:30:00",
  "sequenceOrder": 1
}
```

---

#### Update Agenda Item
**`PUT`** `/api/v1/agendas/{id}`

Same body as Create Agenda Item.

---

#### Delete Agenda Item
**`DELETE`** `/api/v1/agendas/{id}`

---

## Error Response Format

```json
{
  "timestamp": "2026-08-17T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Event with id 99 not found",
  "validationErrors": null
}
```

For validation errors:
```json
{
  "timestamp": "2026-08-17T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "validationErrors": ["title: must not be blank"]
}
```
