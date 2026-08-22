# Auth Service

Authentication and authorization governance service for the Industry Alumni Portal. Handles user login (JWT generation), two-factor authentication (2FA/OTP) for staff, staff invitation flows, partner registration, and token validation.

---

## Technical Details

| Property | Value |
|---|---|
| **Port** | `8081` |
| **Database** | `auth_db` (PostgreSQL) |
| **Swagger UI** | `http://localhost:8081/swagger-ui.html` |
| **OpenAPI Spec** | `http://localhost:8081/v3/api-docs` |
| **DB Schema Management** | Hibernate auto-DDL (`ddl-auto: update`) |
| **Auth Mechanism** | Stateless JWT (`Bearer <token>`), 2FA OTP for Staff |

### Key Dependencies
- Spring Boot 3, Spring Security (Stateless + Method Security), Spring Data JPA
- JJWT (JSON Web Token)
- Spring AMQP (RabbitMQ)
- PostgreSQL Driver
- SpringDoc OpenAPI (Swagger)

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8081` | Server port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/auth_db` | PostgreSQL connection |
| `SPRING_DATASOURCE_USERNAME` | `user` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | DB password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `RABBITMQ_USERNAME` | `guest` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `RABBITMQ_EXCHANGE` | `notification.exchange` | Exchange name |
| `RABBITMQ_ROUTING_KEY` | `notification.invitation` | Default routing key |
| `JWT_SECRET_KEY` | (dev key) | JWT HMAC-SHA signing key |
| `ADMIN_DEFAULT_USER` | `admin001` | Seeded admin username |
| `ADMIN_DEFAULT_PASS` | `Admin@123` | Seeded admin password |
| `ADMIN_DEFAULT_EMAIL` | (dev email) | Seeded admin email |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka discovery URL |

---

## Roles

| Role | Description |
|---|---|
| `ADMIN` | Full system administrator |
| `STUDENT` | Student / Alumni user |
| `INDUSTRY_PARTNER` | Company / Industry Partner representative |
| `FACULTY_COORDINATOR` | Faculty coordinator |
| `INTERNSHIP_COORDINATOR` | Internship coordinator |
| `ACADEMIC_STAFF` | Academic staff member |
| `FACULTY_MANAGEMENT` | Faculty management |

---

## RabbitMQ Integration

**This service PUBLISHES to RabbitMQ.**

| Event | Exchange | Routing Key | When Triggered |
|---|---|---|---|
| Staff 2FA OTP | `notification.exchange` | `notification.otp` | `POST /api/v1/auth/staff/login` |
| Staff Invitation Email | `notification.exchange` | `notification.invitation` | `POST /api/v1/auth/staff/invite` |
| Partner Registration Email | `notification.exchange` | `notification.invitation` | `POST /api/v1/auth/partner/pending` |

---

## API Endpoints

### Authentication & Login

---

#### Login (Student / Industry Partner)
**`POST`** `/api/v1/auth/login` *(Public)*

Authenticates a Student or Industry Partner using username/password and issues a JWT token.

**Request Body:**
```json
{
  "username": "john.student",
  "password": "Password123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "userId": "1",
  "username": "john.student",
  "email": "john@students.nsbm.ac.lk",
  "role": "STUDENT"
}
```

**Response (401 Unauthorized):** Invalid credentials.

---

#### Initiate Staff Login (Step 1 - 2FA)
**`POST`** `/api/v1/auth/staff/login` *(Public)*

Validates staff username and password, generates a 6-digit OTP, saves it to `otp_codes`, and sends an OTP notification via RabbitMQ to the staff member's email.

**Request Body:**
```json
{
  "username": "admin001",
  "password": "Admin@123"
}
```

**Response (200 OK):**
```json
{
  "message": "OTP has been sent to your registered email.",
  "email": "k***@students.nsbm.ac.lk",
  "step": "OTP_VERIFICATION_REQUIRED"
}
```

---

#### Verify Staff OTP (Step 2 - 2FA)
**`POST`** `/api/v1/auth/staff/verify-otp` *(Public)*

Verifies the 6-digit OTP code and issues the JWT token upon success.

**Request Body:**
```json
{
  "username": "admin001",
  "otp": "123456"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "userId": "1",
  "username": "admin001",
  "email": "admin@nsbm.ac.lk",
  "role": "ADMIN"
}
```

**Response (400 Bad Request):** Invalid or expired OTP code.

---

#### Validate Token
**`POST`** `/api/v1/auth/validate` *(Public)*

Validates a JWT token (can be passed via `?token=` query param or `Authorization: Bearer <token>` header).

**Response (200 OK):**
```json
{
  "valid": true,
  "userId": "1",
  "username": "admin001",
  "role": "ADMIN",
  "email": "admin@nsbm.ac.lk"
}
```

---

#### Get Current User
**`GET`** `/api/v1/auth/me` *(Authenticated)*

Header: `Authorization: Bearer <token>`

Returns user details from the validated JWT token.

---

### Staff Management & Invitations

---

#### Invite Staff Member
**`POST`** `/api/v1/auth/staff/invite` *(Requires `ADMIN` Role)*

Header: `Authorization: Bearer <token>`

Sends an invitation email to the specified address. Creates a `PendingStaff` record with a unique token and publishes a notification to RabbitMQ.

**Request Body:**
```json
{
  "email": "newstaff@nsbm.ac.lk",
  "role": "FACULTY_COORDINATOR"
}
```

**Response (201 Created):** Invitation created.  
**Response (409 Conflict):** Staff member already registered or invited.

---

#### Complete Staff Registration
**`POST`** `/api/v1/auth/staff/complete-registration` *(Public)*

Completes staff account setup using the token received by email.

**Request Body:**
```json
{
  "invitationToken": "uuid-token-from-email",
  "username": "john.doe",
  "password": "SecurePass123"
}
```

**Response (201 Created):** Account created successfully.  
**Response (400 Bad Request):** Invalid or expired invitation token.  
**Response (409 Conflict):** Username already taken.

---

### Partner Registration

---

#### Apply as Industry Partner (Pending)
**`POST`** `/api/v1/auth/partner/pending` *(Public)*

Submits partner registration request.

**Request Body:**
```json
{
  "representativeFullName": "Jane Smith",
  "email": "jane@company.com",
  "phone": "+94771234567",
  "representativeJobRole": "HR Manager",
  "companyName": "Tech Corp Ltd",
  "companyIndustry": "Information Technology",
  "companyAddress": "123 Main St, Colombo",
  "companyDescription": "A leading IT company..."
}
```

**Response (201 Created):** Application submitted.  
**Response (409 Conflict):** Email already registered or pending.

---

#### Complete Partner Registration
**`POST`** `/api/v1/auth/partner/complete-registration` *(Public)*

Finalises the partner account using the registration token.

**Request Body:**
```json
{
  "registrationToken": "uuid-token-from-email",
  "username": "jane.smith",
  "password": "SecurePass123"
}
```

**Response (201 Created):** Account created successfully.  
**Response (400 Bad Request):** Invalid or expired token.  
**Response (409 Conflict):** Username already taken.

---

## Error Response Format

Uses RFC 7807 Problem Detail format:

```json
{
  "type": "https://portal.domain.com/errors/conflict",
  "title": "Resource Conflict",
  "status": 409,
  "detail": "Staff member with email x@y.com is already registered or invited.",
  "instance": "/api/v1/auth/staff/invite",
  "timestamp": "2026-08-17T10:00:00Z"
}
```
