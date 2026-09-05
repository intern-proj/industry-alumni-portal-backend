# NSBM INDUSTRY & ALUMNI COLLABORATION PORTAL
## Complete System Architecture, Service Flow & Microservices Reference Guide

---

## 1. System Architecture Overview

The **NSBM Industry & Alumni Collaboration Portal** is built on an enterprise-grade, distributed microservices architecture designed to connect university undergraduates, faculty coordinators, corporate partners, and alumni. 

### High-Level Architectural Topology

```
                       [ FRONTEND CLIENT (React 18 + Vite) ]
                                      │
                                      ▼ HTTPS
                 [ API GATEWAY (Spring Cloud Gateway - Port 8080) ]
                                      │
       ┌──────────────────────────────┼──────────────────────────────┐
       │ (Service Discovery)          │ (REST API Routing)           │ (JWT Validation)
       ▼                              ▼                              ▼
[ EUREKA DISCOVERY SERVER ]    [ MICROSERVICES ]              [ COMMON SECURITY ]
   (Port 8761 Registry)               │
                                      ├──────────────────────────────┐
                                      ▼ Synchronous REST             ▼ Asynchronous AMQP
                             [ MICROSERVICES MESH ]         [ RABBITMQ EVENT BROKER ]
                                      │                              │
                                      ▼                              ▼
                             [ POSTGRESQL DATABASES ]       [ NOTIFICATION SERVICE ]
                             (Per-Service Dedicated)         (Email & Cloud Alerts)
```

### Core Communication Principles
1. **Single Entry Point**: All client requests (Web, Mobile, External API) flow through the **API Gateway** on port `8080` (or the Azure Cloud Gateway FQDN). Direct access to internal microservices ports is shielded.
2. **Dynamic Service Registry**: Every Spring Boot microservice and the Python AI Service registers with **Netflix Eureka Discovery Server** (`discovery-server`) on port `8761`. The API Gateway uses client-side load balancing (`lb://service-name`) to dynamically route traffic without hardcoded IP addresses.
3. **Decoupled Asynchronous Events**: High-latency, cross-service operations (such as flyer OCR processing, resume enhancement, AI candidate scoring, PDF certificate generation, and email dispatching) are published as events to a central **RabbitMQ** topic exchange. Services process tasks asynchronously without blocking web requests.
4. **Database-Per-Service Pattern**: Each business capability owns its isolated PostgreSQL schema (or database), ensuring loose coupling and domain boundary isolation.
5. **Stateless JWT Security**: The **Auth Service** generates signed JSON Web Tokens (JWT) containing user ID, username, email, and role permissions. Downstream microservices validate tokens statelessly using the shared **Common Security** library.

---

## 2. End-to-End Website Workflows

The portal organizes all user interactions across distinct end-to-end lifecycle flows:

```
FLOW 1: STUDENT PROFILE & AI ENHANCEMENT
[Student Registers] ──► [Auth Service] ──► [User Service Profile Created]
                                                     │
[Uploads Resume PDF] ──► [Audit Storage]             │
           │                                         ▼
           └──────────► [AI Service] ──► [Skills & Projects Auto-Populated]

FLOW 2: PARTNER ONBOARDING & VERIFICATION
[Partner Registers] ──► [Platform Mgmt (Pending)] ──► [Staff Approves] ──► [Partner Active]

FLOW 3: VACANCY FLYER EXTRACTION & APPROVAL
[Partner Uploads Flyer] ──► [Vacancy Service] ──► [RabbitMQ Event]
                                                         │
                                                         ▼
                                                 [AI Service Pipeline]
                                                 - OCR Text Extraction
                                                 - Gemini Schema Extraction
                                                 - Multi-Faculty Alignment Check
                                                         │
                                                         ▼
[Staff Dashboard] ◄── [Audit & Fit Notes] ◄── [Vacancy Service Updated]
        │
        ▼ (Approve)
[Live on Public Portal] ──► [Notification Service] ──► [Alerts Sent to Matching Students]

FLOW 4: JOB APPLICATION & AI MATCHING
[Student Applies] ──► [Application Service] ──► [RabbitMQ Match Event]
                                                         │
                                                         ▼
                                                 [AI Service Matcher]
                                                 - Project Evidence Scoring
                                                 - Technical Skill Overlap
                                                 - Executive Fit Summary
                                                         │
                                                         ▼
[Partner Recruiter Portal] ◄──────────────────── [AI Insights Persisted]
(View 0-100% Match & Fortes)

FLOW 5: EVENTS, QR ATTENDANCE & CERTIFICATES
[Staff Creates Event] ──► [Event Mgmt Service] ──► [Student Registers]
                                                          │
                                                          ▼
                                            [Event Participation Service]
                                            (Generates Unique QR Ticket)
                                                          │
                                                          ▼
[Event Day: Organizer Scans QR] ──► [Attendance Verified] ──► [RabbitMQ Event]
                                                                     │
                                                                     ▼
[Public Verification URL] ◄── [Downloadable PDF] ◄── [Certificate Service Generates]
```

---

## 3. Detailed Service-by-Service Breakdown

---

### 1. Frontend Client Application (`frontend`)
- **Technology**: React 18, Vite, React Router DOM, TailwindCSS, Lucide Icons, Axios.
- **Role in Workflow**:
  - Serves responsive web interfaces customized for 4 user roles:
    - **Student Portal (`/student/*`)**: Profile management, resume upload, academic records, verified skill tags, job vacancy explorer, AI cover letter generator, event registration, QR tickets, digital certificates.
    - **Industry Partner Portal (`/partner/*`)**: Company profile, job posting form, flyer upload dropzone with AI status tracker, applicant review table, AI matching score inspector.
    - **Staff / Admin Portal (`/staff/*`)**: Institutional vacancy approval queue, partner verification review, student talent search, event creation, certificate generation, system audit logs.
    - **Public Pages (`/*`)**: Authentication (`/login`, `/register`), public vacancy listings, public event schedules, and certificate verification (`/verify-certificate/:code`).
- **How It Connects**:
  - All HTTP requests route exclusively through the API Gateway URL (`/api/v1/*`).
  - Stores the JWT token in browser `localStorage` and attaches `Authorization: Bearer <token>` on all authenticated calls.

---

### 2. API Gateway Service (`api-gateway`)
- **Technology**: Spring Boot 3, Spring Cloud Gateway, Reactive Netty, Eureka Client.
- **Port**: `8080` (External Cloud Gateway FQDN).
- **Role in Workflow**:
  - Acts as the single reverse proxy for the entire platform.
  - Matches route prefixes and routes traffic to registered Eureka services:
    - `/api/v1/auth/**` &rarr; `lb://auth-service`
    - `/api/v1/user-profiles/**` &rarr; `lb://user-service`
    - `/api/v1/vacancies/**` &rarr; `lb://vacancy-service`
    - `/api/v1/ai/**` &rarr; `lb://ai-service`
    - `/api/v1/applications/**` &rarr; `lb://application-service`
    - `/api/v1/events/**` &rarr; `lb://event-management-service`
    - `/api/v1/participations/**` &rarr; `lb://event-participation-service`
    - `/api/v1/certificates/**` &rarr; `lb://certificate-service`
    - `/api/v1/notifications/**` &rarr; `lb://notification-service`
    - `/api/v1/admin/**` &rarr; `lb://platform-management-service`
    - `/api/v1/storage/**` &rarr; `lb://audit-storage-service`
  - Handles global Cross-Origin Resource Sharing (CORS) headers for frontend integration.

---

### 3. Discovery Server (`discovery-server`)
- **Technology**: Spring Cloud Netflix Eureka Server.
- **Port**: `8761`.
- **Role in Workflow**:
  - Maintains the real-time registry of all active microservice instances, their network hostnames, ephemeral container ports, and health status.
  - Enables zero-downtime rolling updates on Azure Container Apps: as new service revisions provision, they register with Eureka and receive traffic seamlessly.

---

### 4. Auth Service (`services/auth-service`)
- **Technology**: Spring Boot 3, Spring Security 6, JJWT (Java JWT), Spring Data JPA, PostgreSQL (`auth_db`).
- **Port**: `8081`.
- **Role in Workflow**:
  - Handles user authentication, credential storage, password encryption (BCrypt with strength 12), and role authorization.
  - Four distinct user roles supported:
    - `STUDENT`: Undergraduate student profile.
    - `INDUSTRY_PARTNER`: Verified corporate recruiter.
    - `STAFF`: University placement coordinator / lecturer.
    - `SYSTEM_ADMIN`: Platform super-administrator.
  - Issues signed JWT access tokens (1-day expiration) and refresh tokens.
  - **Seed Initializer (`AuthDataInitializer.java`)**:
    - Automatically provisions the System Administrator account (`admin`).
    - Automatically provisions 10 default student accounts (`student1` to `student10`).

---

### 5. User Service (`services/user-service`)
- **Technology**: Spring Boot 3, Spring Data JPA, PostgreSQL (`user_db`).
- **Port**: `8085`.
- **Role in Workflow**:
  - Manages rich student profiles, academic standing, and industry skill portfolios.
  - **Academic Records**: Cumulative GPA, degree program, faculty, department, expected graduation year.
  - **Verified Skills**: Technical competencies with proficiency levels and verification source (manual vs. AI-extracted from resume).
  - **Project Portfolios**: Stores completed projects, architectural descriptions, tech stacks, and source links.
  - **Account Sync Engine (`UserAccountSyncService.java`)**: Periodically checks the auth database and auto-provisions profile shells for newly registered students.
  - **Seed Initializer (`UserDataInitializer.java`)**: Pre-seeds all 10 students with comprehensive portfolios, authentic bios, GPAs, degree programs, and real-world project achievements.

---

### 6. Vacancy Service (`services/vacancy-service`)
- **Technology**: Spring Boot 3, Spring Data JPA, RabbitMQ Event Publisher, PostgreSQL (`vacancy_db`).
- **Port**: `8087`.
- **Role in Workflow**:
  - Manages corporate internship and graduate employment postings.
  - **Partner Submission**: Recruiter uploads a job flyer (PNG, JPG, or PDF) along with basic metadata.
  - **Event Trigger**: When a flyer is uploaded, `VacancyEventPublisher` publishes `vacancy.flyer.uploaded` containing the `vacancyId` and `storageFileId` to RabbitMQ.
  - **AI Callback Receiver**: Receives asynchronous HTTP `PUT /api/v1/vacancies/partner/{id}` updates from the AI Service containing parsed job titles, company names, requirements, target faculties, and compliance flags.
  - **Institutional Review**: Staff review the AI suitability notes and compliance score before changing status from `PENDING` to `APPROVED` or `REJECTED`.

---

### 7. AI Service (`services/ai-service`)
- **Technology**: Python 3.10, FastAPI, Google Gemini API (`gemini-3.1-flash-lite`), Sentence Transformers (`all-MiniLM-L6-v2`), PyMuPDF, PostgreSQL.
- **Port**: `8000`.
- **Role in Workflow**:
  - **Flyer OCR & Gemini Extraction**:
    - Downloads uploaded flyer image or PDF from `audit-storage-service`.
    - Performs vision OCR and feeds extracted plain text into Google Gemini.
    - Extracts structured data into `JobVacancySchema`: job title, company name, seniority, workplace type, locations, required skills, preferred skills, responsibilities, salary, and deadlines.
  - **Institutional Curriculum Checker (`institutional_checker.py`)**:
    - Evaluates job vacancies against all four NSBM faculties (Computing, Business, Engineering, Science).
    - Generates curriculum match scores (0–100), recommends specific degree programs, detects intern/graduate suitability, and flags compliance risks (e.g. missing deadlines or undisclosed salaries).
  - **Resume Profile Enhancer (`resume_extractor.py`)**:
    - Reads student primary resumes, parses verified technologies, and extracts completed projects with detailed tech stacks and impact statements directly into the user profile.
  - **Semantic Smart Search (`smart_search_service.py`)**:
    - Embeds natural language queries and candidate profiles using local sentence transformer vectors and computes project-evidence matching scores.
  - **RabbitMQ Background Consumer**:
    - Asynchronously consumes flyer tasks and application matching tasks from RabbitMQ, updating `vacancy-service` and `application-service` without user delay.

---

### 8. Application Service (`services/application-service`)
- **Technology**: Spring Boot 3, Spring Data JPA, RabbitMQ Publisher, PostgreSQL (`application_service_db`).
- **Port**: `8084`.
- **Role in Workflow**:
  - Manages job application lifecycles from student submission to final offer.
  - **Submission**: Student clicks "Apply", optionally generates an AI cover letter, attaches their primary resume, and submits.
  - **Event Trigger**: Publishes `application.submitted.match` event to RabbitMQ with application ID, vacancy requirements, and resume URL.
  - **AI Candidate Insights**: Persists the computed match score (0–100%), match tier (`HIGH_MATCH`, `GOOD_MATCH`, `GROWTH_OPPORTUNITY`), strong fortes, and skills overlap matrix.
  - **Recruiter Workflow**: Partners filter applicants by AI match score and update status (`SHORTLISTED`, `INTERVIEW_SCHEDULED`, `OFFERED`, `REJECTED`).

---

### 9. Event Management Service (`services/event-management-service`)
- **Technology**: Spring Boot 3, Spring Data JPA, RabbitMQ Publisher, PostgreSQL (`event_management_db`).
- **Port**: `8082`.
- **Role in Workflow**:
  - Handles university events: career fairs, company tech talks, industry workshops, and hackathons.
  - Organizers create events with seat capacities, registration deadlines, venue locations, and target faculties.
  - Staff approve partner-submitted events before they become visible on the public student calendar.
  - Emits `event.approved` notifications across RabbitMQ.

---

### 10. Event Participation Service (`services/event-participation-service`)
- **Technology**: Spring Boot 3, Spring Data JPA, ZXing (Zebra Crossing QR Engine), PostgreSQL (`event_participation_db`).
- **Port**: `8083`.
- **Role in Workflow**:
  - Handles student registrations and physical attendance verification.
  - **Registration**: Student registers for an event &rarr; system reserves capacity and generates a unique, cryptographically signed QR code ticket.
  - **Check-In**: At the venue, the organizer scans the student's QR code using the mobile/web scanner (`POST /api/v1/participations/verify-qr`).
  - **Certificate Trigger**: When attendance is marked as `ATTENDED`, publishes `event.attendance.marked` to RabbitMQ to trigger automated certificate generation.

---

### 11. Certificate Service (`services/certificate-service`)
- **Technology**: Spring Boot 3, Spring Data JPA, iText / PDFBox PDF Generation Engine, PostgreSQL (`certificate_db`).
- **Port**: `8090`.
- **Role in Workflow**:
  - Generates verifiable, branded digital PDF certificates of completion or participation.
  - Embeds:
    - Student Full Name, Degree Program, Event/Achievement Title.
    - Cryptographic Verification Code (e.g. `CERT-2026-XXXX`).
    - Embedded verification QR code resolving to the public portal URL (`/verify-certificate/{code}`).
  - Provides a public verification endpoint accessible by external corporate recruiters without needing an account.

---

### 12. Notification Service (`services/notification-service`)
- **Technology**: Spring Boot 3, Spring Mail (JavaMailSender), RabbitMQ Listener, PostgreSQL (`notification`).
- **Port**: `8088`.
- **Role in Workflow**:
  - Decoupled asynchronous messaging engine listening on RabbitMQ topic queues.
  - Dispatches styled, branded HTML notification emails:
    - Welcome and email verification links.
    - Password reset OTPs and confirmation links.
    - Job application status changes (Shortlisted, Interview Call).
    - Event registration tickets with QR codes.
    - Certificate issuance announcements with PDF download links.
  - Dynamically updates URLs based on the active domain (`APP_DOMAIN` / Cloud Gateway) ensuring no hardcoded localhost links.

---

### 13. Platform Management Service (`services/platform-management-service`)
- **Technology**: Spring Boot 3, Spring Data JPA, PostgreSQL (`platform_management_db`).
- **Port**: `8086`.
- **Role in Workflow**:
  - Central operational command center for University Staff and System Administrators.
  - **Partner Verification**: Reviews new corporate partner registrations, verifying business registration documents before granting recruitment permissions.
  - **Faculty & Academic Management**: Manages university faculty catalog, departments, and accredited degree programs.
  - **System Audit Logs**: Records administrative actions (approvals, rejections, user suspensions) for regulatory compliance.
  - **Platform Analytics**: Computes aggregate metrics (total active students, verified partners, job placement rates).

---

### 14. Audit & Storage Service (`services/audit-storage-service`)
- **Technology**: Spring Boot 3, Spring Data JPA, Local/Cloud Object Storage, PostgreSQL (`audit_storage_db`).
- **Port**: `8089`.
- **Role in Workflow**:
  - Centralized file storage microservice for binary assets across the portal.
  - Stores:
    - Student resumes (PDF, DOCX).
    - Vacancy flyer posters (PNG, JPEG, PDF).
    - Corporate logos and partner verification documents.
    - Event banner media and promotional assets.
  - Generates unique GUID-based `storageFileId` keys.
  - Supports both attachment downloads and inline browser rendering (`?inline=true`).

---

### 15. Common Security (`services/common-security`)
- **Technology**: Spring Security 6, JJWT, Shared Maven Library.
- **Role in Workflow**:
  - Shared dependency included across all Spring Boot microservices.
  - Contains standardized JWT authentication filters, token parsers, security context initializers, and role-based access control annotations (`@PreAuthorize("hasRole('STUDENT')")`).
  - Guarantees consistent security policies across every microservice.

---

## 4. Student Accounts & Login Credentials

All 10 student profiles are initialized with official university accounts and complete academic portfolios.

### Student Credentials Table

| Username | Full Name | Email Address | Password | Faculty & Degree Program | GPA | Target Specialization |
|---|---|---|---|---|---|---|
| **`student1`** | John Smith | `student1@students.nsbm.ac.lk` | **`Student@1`** | Faculty of Computing<br>BSc (Hons) in Software Engineering | **3.82** | Full-Stack Software Engineer (React, Spring Boot) |
| **`student2`** | Emma Johnson | `student2@students.nsbm.ac.lk` | **`Student@2`** | Faculty of Computing<br>BSc (Hons) in Computer Networks | **3.75** | Cloud & DevOps Specialist (Kubernetes, AWS) |
| **`student3`** | Michael Brown | `student3@students.nsbm.ac.lk` | **`Student@3`** | Faculty of Computing<br>BSc (Hons) in Software Engineering | **3.91** | Backend Systems & Distributed Architecture (Java, Go, Kafka) |
| **`student4`** | Sophia Taylor | `student4@students.nsbm.ac.lk` | **`Student@4`** | Faculty of Computing<br>BSc (Hons) in Multimedia & Web | **3.84** | Frontend Engineer & UI/UX Craftsman (React, TypeScript) |
| **`student5`** | William Anderson | `student5@students.nsbm.ac.lk` | **`Student@5`** | Faculty of Computing<br>BSc (Hons) in Data Science | **3.88** | AI/ML Engineer & Data Scientist (Python, PyTorch, LLMs) |
| **`student6`** | Olivia Thomas | `student6@students.nsbm.ac.lk` | **`Student@6`** | Faculty of Computing<br>BSc (Hons) in Software Engineering | **3.68** | Mobile App Developer (React Native, Flutter, Kotlin) |
| **`student7`** | James Jackson | `student7@students.nsbm.ac.lk` | **`Student@7`** | Faculty of Computing<br>BSc (Hons) in Computer Security | **3.70** | Cybersecurity & Network Systems Engineer (SecOps, Linux) |
| **`student8`** | Ava White | `student8@students.nsbm.ac.lk` | **`Student@8`** | Faculty of Engineering<br>BSc (Hons) in Computer Systems Engineering | **3.78** | Full-Stack Enterprise Solutions Developer (.NET, C#, React) |
| **`student9`** | Alexander Harris | `student9@students.nsbm.ac.lk` | **`Student@9`** | Faculty of Computing<br>BSc (Hons) in Data Science | **3.65** | Data Platform & Big Data Engineer (Kafka, Spark, ETL) |
| **`student10`** | Mia Martin | `student10@students.nsbm.ac.lk` | **`Student@10`** | Faculty of Engineering<br>BSc (Hons) in Computer Systems Engineering | **3.95** | Site Reliability Engineer (SRE & Go Infrastructure) |

---

### Additional Administrative & Partner Accounts

| Role | Username / Identifier | Email Address | Password | Purpose |
|---|---|---|---|---|
| **System Administrator** | `admin` | `prasadkvithana@gmail.com` | **`Admin@123`** | Super-admin access, partner verification, system configuration |
| **Staff Member** | `staff` | `staff@nsbm.ac.lk` | **`Staff@123`** | Career guidance coordinator, vacancy approval, event creation |
| **Industry Partner** | `synnext` | `contact@synnext.com` | **`Partner@123`** | Verified recruitment partner, job vacancy posting, applicant review |

---

## 5. Summary of Service Port Numbers & Endpoints

| Service Name | Port | Primary Database | Key API Base Path | Primary Responsibility |
|---|---|---|---|---|
| **API Gateway** | `8080` | *None* | `/api/v1/*` | Unified ingress router, CORS, SSL termination |
| **Eureka Discovery Server** | `8761` | *None* | `/eureka/*` | Service registration and heartbeat monitoring |
| **Auth Service** | `8081` | `auth_db` | `/api/v1/auth` | User registration, login, JWT token management |
| **Event Management Service** | `8082` | `event_management_db` | `/api/v1/events` | Career events, workshops, hackathon catalog |
| **Event Participation Service** | `8083` | `event_participation_db` | `/api/v1/participations` | Event registration, QR code attendance verification |
| **Application Service** | `8084` | `application_service_db` | `/api/v1/applications` | Job applications, candidate review, AI match persistence |
| **User Service** | `8085` | `user_db` | `/api/v1/user-profiles` | Student profiles, academic records, skills & projects |
| **Platform Management Service** | `8086` | `platform_management_db` | `/api/v1/admin` | Partner verification, audit logs, university faculties |
| **Vacancy Service** | `8087` | `vacancy_db` | `/api/v1/vacancies` | Job postings, flyer upload intake, approval workflow |
| **Notification Service** | `8088` | `notification` | `/api/v1/notifications` | Asynchronous email dispatch, dynamic notification URLs |
| **Audit & Storage Service** | `8089` | `audit_storage_db` | `/api/v1/storage` | File uploads, download streams, binary storage |
| **Certificate Service** | `8090` | `certificate_db` | `/api/v1/certificates` | PDF certificate issuance and public verification |
| **AI Service** | `8000` | *Internal SQLite / PG* | `/api/v1/ai` | Gemini OCR, institutional fit, smart search, resume parser |
