# 🚀 ASCEND Enterprise Performance Management Module

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue.svg)](https://www.postgresql.org/)
[![Next.js](https://img.shields.io/badge/Next.js-16.3-black.svg)](https://nextjs.org/)
[![React](https://img.shields.io/badge/React-19-cyan.svg)](https://react.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-v4-38bdf8.svg)](https://tailwindcss.com/)


An enterprise-grade **Performance Appraisal, OKR/KPI Tracking, and HR Governance System** built with **Spring Boot 4 / Java 21**, **Spring Security (Stateless JWT)**, **PostgreSQL 18**, **Hibernate JPA Criteria Specifications**, and **Next.js 16 (React 19)**.

---

## 📑 Table of Contents
1. [Overview & Core Value](#-overview--core-value)
2. [System Architecture](#-system-architecture)
3. [Role-Based Access Control (RBAC)](#-role-based-access-control-rbac)
4. [Security & JWT Authentication Flow](#-security--jwt-authentication-flow)
5. [Structured Logging & Correlation IDs](#-structured-logging--correlation-ids)
6. [Monitoring & Spring Boot Actuator](#-monitoring--spring-boot-actuator)
7. [Environment Profiles & Externalized Config](#-environment-profiles--externalized-config)
8. [Tech Stack](#-tech-stack)
9. [API Endpoints Reference](#-api-endpoints-reference)
10. [Unit Testing & Test Suites (100 Tests)](#-unit-testing--test-suites-100-tests)
11. [Local Setup & Running Instructions](#-local-setup--running-instructions)
12. [Default Seeded Test Accounts](#-default-seeded-test-accounts)
13. [Google OAuth 2.0 Authentication](#-google-oauth-20-third-party-authentication)

---

## 🌟 Overview & Core Value

The **ASCEND Performance Management Module** manages the complete corporate performance evaluation lifecycle:
- **HR Governance**: Performance review cycle initiation, department management, provisioning manager hierarchy, and dynamic talent discovery.
- **Manager Oversight**: Direct reports monitoring, assigning measurable OKRs and KPIs with strict weight validations ($\le 100\%$), target tracking, and reviewing change requests.
- **Employee Empowerment**: Transparent goal visibility, milestone acknowledgment, granular progress updates (0–100%), and formal adjustment requests.

---

## 🏛️ System Architecture

```
[ Next.js 16 Client Portal (React 19 / Tailwind CSS) ]
                        │
                        ▼ (HTTP REST + Bearer Token)
[ Spring Security Filter Chain (JwtAuthenticationFilter) ]
                        │
                        ▼ (SecurityContext & RBAC @PreAuthorize)
[ REST Controller Layer (@RestController) ]
                        │
                        ▼ (DTO Validation @Valid)
[ Service Layer (@Service & @Transactional) ]
                        │
                        ▼ (JPA Criteria Specifications)
[ Repository Layer (Spring Data JPA) ]
                        │
                        ▼ (HikariCP Connection Pool)
```

### 📂 Monorepo Repository Structure
```
Advance_Performance_module/
├── backend_ascend/                          # ☕ Spring Boot 4 / Java 21 Enterprise Backend
│   ├── src/main/java/                       # REST Controllers, Business Services, JPA Entities, DTOs
│   ├── src/main/resources/                  # application-dev.properties, application-prod.properties
│   ├── src/test/java/                       # Mockito & Spring Boot Test Suites
│   ├── pom.xml                              # Maven configuration
│   └── mvnw / mvnw.cmd                      # Maven wrapper
│
├── frontend_ascend/                         # ⚛️ Next.js 16 / React 19 Client Application
│   ├── src/app/                             # App Router Pages (HR, Manager, Employee, Login, Signup)
│   ├── src/components/                      # UI Components, Modals, Navbar, StatCards
│   ├── src/services/                        # Axios/Fetch API Clients
│   └── package.json                         # Node dependencies & Turbopack scripts
│
├── docs/                                    # 📚 Documentation & System Design
│   ├── diagrams/                            # SVG Architecture Visualizers & HTML Models
│   ├── hrms_policies/                       # HRMS Business Policies & Acceptance Rules
│   └── API_ENDPOINTS_AND_FIELDS.txt         # Full REST API Reference & Field Contracts
│
└── README.md                                # Workspace Overview & Setup Guide
```

---

## 👥 Role-Based Access Control (RBAC)

| Role | Responsibilities | Key Capabilities |
| :--- | :--- | :--- |
| **`HR`** | System Administration & Governance | Create/Launch/Close Performance Review Cycles, manage departments, provision Managers, assign reporting managers to employees, advanced employee search. |
| **`MANAGER`** | Goal Management & Team Oversight | View assigned direct reports, create/update/delete OKRs & KPIs with weights, review and approve/reject goal modification requests. |
| **`EMPLOYEE`** | Goal Execution & Tracking | View reporting manager and assigned goals, accept assigned goals (`PENDING_ACCEPTANCE` $\rightarrow$ `ACCEPTED`), update progress (0-100%), submit modification requests. |

---

## 🛡️ Security & JWT Authentication Flow

1. Client sends credentials (`email`, `password`) to `/api/auth/login`.
2. Backend authenticates credentials using Spring Security's `AuthenticationManager` and BCrypt.
3. `JwtTokenProvider` signs and returns a cryptographically secure JWT Bearer token containing user ID, role, and expiration.
4. For all subsequent requests, `JwtAuthenticationFilter` intercepts the `Authorization: Bearer <token>` header, extracts authorities, and establishes the `SecurityContext`.

Interactive Swagger Documentation with JWT authorization is accessible at:
👉 `http://localhost:8080/swagger-ui/index.html`

---

## 🪵 Structured Logging & Correlation IDs

ASCEND employs a production-grade, asynchronous structured logging architecture backed by **Logback** and **SLF4J MDC (Mapped Diagnostic Context)**:

### 🔍 Request Correlation & Distributed Tracing
- Every incoming HTTP request is intercepted by [`CorrelationIdFilter`](backend_ascend/src/main/java/com/practice/springbootdemo/advance_performance_module/config/CorrelationIdFilter.java) at `Ordered.HIGHEST_PRECEDENCE`.
- If the client sends an `X-Correlation-ID` header, it is reused; otherwise, a unique UUID (`java.util.UUID.randomUUID()`) is generated.
- The `correlationId` and `clientIp` are injected into SLF4J MDC and returned in the HTTP response header `X-Correlation-ID`.
- Context is strictly cleaned up in a `finally` block to prevent thread pool leakage.

### 🛡️ Zero Sensitive Data Leaks
- Passwords, raw OTPs, JWT tokens, and OAuth secrets are **strictly excluded** from all log statements.
- Email dispatch logs record operational metadata without logging raw verification secrets.

### 📁 Multi-Profile Log Routing
| Profile | Console Output | Log File Location | Retention / Cap |
| :--- | :--- | :--- | :--- |
| **`dev`** | Colorized ANSI (`%highlight`, `%magenta`, `%cyan`) | `logs/ascend-dev.log` | 30 days, 1 GB cap |
| **`prod`** | ISO-8601 Structured Timestamps | `logs/ascend-prod.log` (Async Appender) | 60 days, 5 GB cap, 100MB chunk |
| **`test`** | Minimal console logs | Console only | N/A |

Sample Dev Log:
```
2026-09-07 16:29:42.381 DEBUG [trace-health-check-001] [main] o.s.s.w.a.AnonymousAuthenticationFilter : Set SecurityContextHolder to anonymous SecurityContext
2026-09-07 16:29:42.384 DEBUG [trace-health-check-001] [main] o.s.security.web.FilterChainProxy : Secured GET /actuator/health
```

---

## 📊 Monitoring & Spring Boot Actuator

ASCEND integrates **Spring Boot Actuator** for cloud/container health probes, subsystem diagnostics, and application monitoring.

### 🩺 Endpoints & Security Policy
| Endpoint | Method | Security Policy | Purpose |
| :--- | :--- | :--- | :--- |
| `/actuator/health` | `GET` | **Public (`permitAll()`)** | Liveness, readiness, and composite database health probes |
| `/actuator/info` | `GET` | **Public (`permitAll()`)** | Application metadata, Java runtime, and OS diagnostics |
| `/actuator/metrics` | `GET` | **Secured (`ROLE_HR`)** | JVM heap, garbage collection, and thread metrics |
| `/actuator/metrics/{metric}` | `GET` | **Secured (`ROLE_HR`)** | Granular metric inspection (e.g. `jvm.memory.used`) |

### 🛠️ Custom Health Indicator: `AscendHealthIndicator`
Registered as bean `ascendApp`, providing real-time diagnostics:
```json
{
  "status": "UP",
  "components": {
    "ascendApp": {
      "status": "UP",
      "details": {
        "database.status": "CONNECTED",
        "database.product": "PostgreSQL",
        "database.version": "18.0",
        "subsystem": "ASCEND Performance & OKR Tracking Engine",
        "statusDescription": "Operational - Ready to accept traffic",
        "jvm.freeMemoryMb": 245,
        "jvm.usedMemoryMb": 150,
        "jvm.totalMemoryMb": 395,
        "jvm.maxMemoryMb": 4096
      }
    },
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

---

## ⚙️ Environment Profiles & Externalized Config

All environment settings, database credentials, secrets, and connection pools are fully externalized:

| Profile | Target Database | DDL Auto | Hikari Pool | Logging Level |
| :--- | :--- | :--- | :--- | :--- |
| **`dev`** (Default) | `jdbc:postgresql://localhost:5432/advance_ascend_performance` | `update` | 10 conns | `DEBUG` (com.practice.springbootdemo) |
| **`test`** | `jdbc:h2:mem:ascend_test;MODE=PostgreSQL` | `create-drop` | In-memory | `WARN` / `INFO` |
| **`prod`** | Strict fail-fast via `${PROD_DB_URL}` | `validate` | 30 conns (high-concurrency) | `WARN` |

### 🔑 Environment Variables Reference Template (`.env.example`)
To configure local or production environments, copy `.env.example` to `.env`:
```bash
# Profile selection
SPRING_PROFILES_ACTIVE=dev
PORT=8080

# JWT Secrets (Minimum 256-bit HMAC-SHA256)
JWT_SECRET=ASCEND_PERFORMANCE_SUPER_SECURE_JWT_SECRET_KEY_2026_PROD
JWT_EXPIRATION_MS=86400000

# Database
DB_URL=jdbc:postgresql://localhost:5432/advance_ascend_performance
DB_USERNAME=postgres
DB_PASSWORD=1234

# Mail Dispatch (Gmail / SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Google OAuth 2.0
GOOGLE_CLIENT_ID=your-google-oauth-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-oauth-client-secret
FRONTEND_OAUTH_REDIRECT_URI=http://localhost:3000/login
```

---

## 💻 Tech Stack

### Backend
- **Java 21 (LTS)** & **Spring Boot 4.1.0**
- **Spring Boot Actuator** & **Micrometer** (Health & telemetry)
- **Spring Data JPA** & **Hibernate ORM** (Dynamic JPA Criteria Specifications)
- **Spring Security** & **jjwt 0.12.6** (Stateless JWT Authentication & OAuth 2.0)
- **PostgreSQL 18** with **HikariCP** connection pooling
- **H2 Database** (Zero-dependency test suite execution)
- **Jakarta Bean Validation** (`@Valid`, `@NotNull`, `@Min`, `@Max`)
- **springdoc-openapi 2.8.9** (Swagger UI documentation)
- **JUnit 5**, **Mockito**, and **AssertJ** (Automated test suites)
- **Lombok** & **SLF4J / Logback** (Structured multi-profile logging & MDC correlation IDs)

### Frontend
- **Next.js 16 (App Router)** & **React 19**
- **TypeScript 5**
- **Tailwind CSS v4**
- **Axios API Services** with centralized token interceptors

---

## 📡 API Endpoints Reference

### 1. Authentication & Profile (`/api/auth`)
* `POST /api/auth/signup` — Register new employee (Public)
* `POST /api/auth/login` — Authenticate and receive JWT Bearer token (Public)
* `GET /api/auth/me` — Get current logged-in user profile (Authenticated)
* `GET /api/auth/departments` — List departments for registration (Public)

### 2. HR Administration (`/api/hr`)
* `POST /api/hr/performance-cycles` — Create review cycle in `DRAFT` status (`HR`)
* `GET /api/hr/performance-cycles` — List all performance review cycles (`HR`, `MANAGER`, `EMPLOYEE`)
* `GET /api/hr/performance-cycles/active` — Fetch current active cycle (`HR`, `MANAGER`, `EMPLOYEE`)
* `GET /api/hr/performance-cycles/{id}` — Fetch cycle by ID (`HR`, `MANAGER`, `EMPLOYEE`)
* `PUT /api/hr/performance-cycles/{id}` — Update cycle details (`HR`)
* `PATCH /api/hr/performance-cycles/{id}/launch` — Activate cycle `DRAFT` $\rightarrow$ `ACTIVE` (`HR`)
* `PATCH /api/hr/performance-cycles/{id}/close` — Close cycle `ACTIVE` $\rightarrow$ `CLOSED` (`HR`)
* `GET /api/hr/performance-cycles/search` — Search cycles with dynamic filtering & pagination (`HR`)
* `GET /api/hr/departments` — List all organization departments (`HR`)
* `POST /api/hr/departments` — Create new department (`HR`)
* `GET /api/hr/employees` — List all employees (`HR`)
* `GET /api/hr/employees/managers` — List all provisioned managers (`HR`)
* `POST /api/hr/employees/managers` — Provision new Manager account (`HR`)
* `POST /api/hr/manager-assignments` — Assign employee to reporting manager (`HR`)
* `GET /api/hr/manager-assignments` — List reporting relationships (`HR`)
* `PUT /api/hr/manager-assignments/{id}` — Update reporting assignment (`HR`)
* `PATCH /api/hr/manager-assignments/{id}/set-default` — Set primary reporting manager (`HR`)
* `PATCH /api/hr/manager-assignments/{id}/deactivate` — Deactivate reporting assignment (`HR`)
* `GET /api/hr/manager-assignments/search` — Filter assignments by manager/employee (`HR`)

### 3. Manager Goal Operations (`/api/manager`)
* `GET /api/manager/goals/team` — List assigned direct reports (`MANAGER`)
* `GET /api/manager/goals` — List all goals managed by the manager (`MANAGER`)
* `GET /api/manager/goals/employee/{employeeId}?cycleId={cycleId}` — Get direct report's goals (`MANAGER`)
* `POST /api/manager/goals` — Assign OKR/KPI to employee (weight validation $\le 100\%$) (`MANAGER`)
* `PUT /api/manager/goals/{id}` — Update goal target, metric, weight, deadline (`MANAGER`)
* `DELETE /api/manager/goals/{id}` — Delete unstarted goal (`MANAGER`)
* `GET /api/manager/goals/search` — Multi-criteria search on managed goals (`MANAGER`)
* `GET /api/manager/goal-modification-requests` — View pending employee modification requests (`MANAGER`)
* `PATCH /api/manager/goal-modification-requests/{id}/approve` — Approve change request (`MANAGER`)
* `PATCH /api/manager/goal-modification-requests/{id}/reject` — Reject change request (`MANAGER`)

### 4. Employee Workflow (`/api/employee`)
* `GET /api/employee/goals/my-manager` — View direct reporting manager details (`EMPLOYEE`)
* `GET /api/employee/goals` — View assigned goals for active cycle (`EMPLOYEE`)
* `PATCH /api/employee/goals/{goalId}/accept` — Formally accept assigned goal (`EMPLOYEE`)
* `PATCH /api/employee/goals/{goalId}/progress` — Update completion percentage (0–100%) (`EMPLOYEE`)
* `PATCH /api/employee/goals/{goalId}/modification-request` — Request goal adjustments (`EMPLOYEE`)

### 5. Advanced Dynamic Search (`/api/employees/search`)
* `GET /api/employees/search` — Multi-field filtering (Name, Code, Department, Role, Skill, Location, Experience) with dynamic sorting & pagination.

---

## 🚀 Local Setup & Running Instructions

### Prerequisites
- **JDK 21** installed
- **Node.js 18+** & **npm**
- **PostgreSQL 18** running locally on port `5432` with database `advance_ascend_performance`

### 1. Start Backend (Spring Boot 4)
```bash
cd backend_ascend
./mvnw.cmd spring-boot:run
```
- **Backend Base URL**: `http://localhost:8080`
- **Swagger Documentation**: `http://localhost:8080/swagger-ui/index.html`

### 2. Start Frontend (Next.js 16)
```bash
cd frontend_ascend
npm install
npm run dev
```
- **Frontend URL**: `http://localhost:3000`

---

## 🔑 Default Seeded Test Accounts

| Role | Email | Password | Employee Code |
| :--- | :--- | :--- | :--- |
| **HR Admin** | `hr@ascend.local` | `Password1` | `HR001` |
| **Manager** | `manager1@ascend.local` | `Password1` | `MGR001` |
| **Employee** | `employee1_1@ascend.local` | `Password1` | `EMP001` |

---

## 🌐 Google OAuth 2.0 Third-Party Authentication

The ASCEND module integrates Google Sign-In / OAuth 2.0 alongside traditional credential-based login.

### 🔄 Authentication vs. Authorization Flow
1. **User Request**: The user clicks **Continue with Google** on `/login` or `/signup`.
2. **Delegation (Authentication)**: The browser is redirected to `http://localhost:8080/oauth2/authorization/google`. Google handles user authentication and prompts for consent (`openid`, `profile`, `email`).
3. **Callback & Code Exchange**: Google redirects to `http://localhost:8080/login/oauth2/code/google`. Spring Security exchanges the authorization code for the user's verified identity.
4. **User Synchronization & Deduplication**:
   - If a user with that email already exists in PostgreSQL, their Google Provider ID is linked, and their existing role (`HR`, `MANAGER`, or `EMPLOYEE`) is preserved without creating duplicate accounts.
   - If the user is new, an Employee account is automatically provisioned with a generated `employee_code` (`EMPxxx`), assigned to the default department, and given the `EMPLOYEE` role.
5. **Session & Protected APIs (Authorization)**:
   - An authenticated session is established in Spring Security.
   - The user is redirected to the Next.js frontend with their profile and role, granting access to their designated workspace (`/hr`, `/manager`, or `/employee`).
   - Protected endpoints require an authenticated user in the `SecurityContext`.

### 🔑 Required Environment Variables (Do not commit secrets to Git)
Before running the backend with live Google OAuth:
```powershell
$env:GOOGLE_CLIENT_ID="your-client-id.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET="GOCSPX-your-client-secret"
$env:FRONTEND_OAUTH_REDIRECT_URI="http://localhost:3000/login"
```
Or copy `backend_ascend/.env.example` to your local environment file.

