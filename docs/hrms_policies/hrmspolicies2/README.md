# HRMS Policies 2 — MySQL

A Spring Boot + MySQL backend (with a Next.js frontend) for managing HR
policies: authentication, full CRUD, advanced search/filtering,
pagination, global exception handling, Swagger docs, logging, and
environment-based configuration.

## Stack
- Java 17, Spring Boot 3.5.4, Maven
- Spring Data JPA / Hibernate, Spring Security, JWT
- MySQL 8
- springdoc-openapi (Swagger UI)
- Next.js (frontend)

## Project structure
```
backend/    Spring Boot API (controller -> service -> repository)
frontend/   Next.js UI (login, signup, policies page)
database/   schema.sql - MySQL schema
postman/    Postman collection (CRUD + search + negative scenarios)
```

## What this module does
The **Policy** module lets an authenticated HR user (or admin) create,
read, update, delete, and search HR policies — e.g. a "Work From Home"
or "Leave" policy — including who's affected, whether it's mandatory,
and its lifecycle status (`DRAFT` -> `ACTIVE` -> `ARCHIVED`).

## Entities

### User
| Field | Type | Notes |
|---|---|---|
| id | Long | PK, auto-increment |
| name | String | |
| email | String | unique |
| password | String | BCrypt-hashed, never returned in API responses |
| role | String | `ADMIN` / `USER` |
| createdPolicies | List\<Policy\> | inverse side of the relationship below |

### Policy
| Field | Type | Notes |
|---|---|---|
| id | Long | PK, auto-increment |
| name | String | required |
| code | String | required, unique (e.g. `WFH-001`) |
| category | String | required (e.g. Leave, Conduct, Security) |
| content | String (TEXT) | policy body |
| applicability | String | required (e.g. `ALL`, `MANAGERS`, `INTERNS`) |
| mandatory | Boolean | required |
| status | String | `DRAFT` / `ACTIVE` / `ARCHIVED` |
| createdBy | User | **Many-to-One**, who authored the policy |
| createdAt / updatedAt | LocalDateTime | audit timestamps |

### Relationship
- `Policy.createdBy` is **Many-to-One** to `User`, `FetchType.LAZY`, no
  cascade — deleting a `Policy` never deletes its author, and loading
  a `Policy` never eagerly pulls the whole `User` unless accessed.
- `User.createdPolicies` is the inverse **One-to-Many** side
  (`mappedBy = "createdBy"`), also `LAZY`.
- The FK (`policies.created_by -> users.id`) is `ON DELETE SET NULL`,
  so removing a user doesn't orphan-delete their policies.

## APIs

Base URL: `http://localhost:8080`

Every response — success or error — is a JSON envelope:
```json
// success
{ "success": true, "message": "...", "data": { }, "timestamp": "..." }

// error
{ "success": false, "message": "...", "status": 404, "path": "/api/policies/99", "errors": null, "timestamp": "..." }
```
(`errors` is populated with a list of field-level messages on `400`
validation failures.)

### Auth
| Method | Endpoint | Auth required | Description |
|---|---|---|---|
| POST | `/api/auth/signup` | No | Register a new user |
| POST | `/api/auth/login` | No | Returns a JWT |
| POST | `/api/auth/forgot-password` | No | Verify an email is registered |
| POST | `/api/auth/reset-password` | No | Set a new password |

### Policies (all require `Authorization: Bearer <token>`)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/policies` | List all policies (unpaginated) |
| GET | `/api/policies/{id}` | Get one policy |
| GET | `/api/policies/search` | Advanced search — see below |
| POST | `/api/policies` | Create |
| PUT | `/api/policies/{id}` | Full update |
| PATCH | `/api/policies/{id}` | Partial update |
| DELETE | `/api/policies/{id}` | Delete |

### Advanced search — `GET /api/policies/search`
Every parameter is optional and combinable:

| Param | Example | Meaning |
|---|---|---|
| `keyword` | `leave` | free-text match on name/code/content |
| `category` | `Leave` | exact match |
| `status` | `ACTIVE` | exact match |
| `applicability` | `ALL` | exact match |
| `mandatory` | `true` | boolean filter |
| `page` | `0` | zero-based page index (default `0`) |
| `size` | `10` | page size (default `10`) |
| `sortBy` | `name` | one of `id,name,code,category,applicability,mandatory,status,createdAt,updatedAt` |
| `direction` | `desc` | `asc` (default) or `desc` |

Example:
```
GET /api/policies/search?category=Leave&status=ACTIVE&sortBy=createdAt&direction=desc&page=0&size=5
```
returns:
```json
{
  "success": true,
  "message": "Policy search completed",
  "data": {
    "content": [ { "id": 3, "name": "Leave Policy" } ],
    "pageNumber": 0,
    "pageSize": 5,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

An unrecognised `sortBy` value returns `400` with a message listing the
allowed fields, instead of a raw Hibernate error.

## Error handling
A `@RestControllerAdvice` (`GlobalExceptionHandler`) turns exceptions
into consistent JSON:

| Exception | HTTP status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `DuplicateResourceException` | 409 |
| `UnauthorizedException` | 401 |
| `BadRequestException` | 400 |
| `MethodArgumentNotValidException` (`@Valid` failures) | 400 (with per-field `errors`) |
| `MissingServletRequestParameterException` / type mismatch | 400 |
| anything else | 500 (message hidden, full stack trace logged server-side) |

## Validation
`PolicyRequest` uses bean validation (`@NotBlank`, `@NotNull`,
`@Size`, `@Pattern`) — invalid input is rejected before it reaches the
service layer, with a field-by-field error list in the response.

## Swagger / OpenAPI
Once the backend is running:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Raw spec: `http://localhost:8080/v3/api-docs`

Click **Authorize** and paste a JWT (from `/api/auth/login`) to call
protected endpoints directly from the browser.

## Logging
SLF4J logging is used throughout the service layer:
- Successful operations (signup, login, policy create/update/delete,
  searches) are logged at `INFO`.
- Failed lookups / validation problems are logged at `WARN`.
- Unhandled exceptions are logged at `ERROR` with the full stack trace.
- **Passwords are never logged**, raw or encoded — only non-sensitive
  identifiers such as email addresses.

Log verbosity is controlled per-profile (see below): `dev` is verbose
(includes SQL), `prod` is quiet.

## Configuration & profiles
Configuration is externalized via environment variables with dev-only
defaults, spread across three files:

- `application.properties` — shared config (server port, JWT, CORS,
  Swagger paths), reads `SPRING_PROFILES_ACTIVE` (defaults to `dev`)
- `application-dev.properties` — local MySQL, `ddl-auto=update`,
  verbose SQL + DEBUG logging
- `application-prod.properties` — `ddl-auto=validate` (schema must
  already exist via `database/schema.sql`), quiet logging, all
  datasource values required from environment variables (no defaults)

Switch profiles with:
```bash
java -jar app.jar --spring.profiles.active=prod
# or
export SPRING_PROFILES_ACTIVE=prod
```

Relevant environment variables: `SERVER_PORT`, `SPRING_PROFILES_ACTIVE`,
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`,
`CORS_ALLOWED_ORIGINS`.

## MySQL setup
1. Start MySQL.
2. Either let Hibernate create the schema in `dev` (`ddl-auto=update`),
   or run `database/schema.sql` directly (required for `prod`, where
   `ddl-auto=validate` only checks the schema, never alters it):
```sql
CREATE DATABASE IF NOT EXISTS hrmspolicies2;
```
3. Set your credentials (env vars, or edit
   `backend/src/main/resources/application-dev.properties` for local
   development only — never commit real passwords):
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

## Run backend
```bash
cd backend
mvn clean spring-boot:run
```
Backend: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

## Run frontend
```bash
cd frontend
cp .env.local.example .env.local
npm install
npm run dev
```
Frontend: `http://localhost:3000`

## Default admin account
`DataInitializer` seeds one admin account on first startup (dev/demo
only — remove or replace this for a real deployment):
- email: `admin@gmail.com`
- password: `admin123`

## Sample API requests

**Login**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@gmail.com","password":"admin123"}'
```

**Create a policy**
```bash
curl -X POST http://localhost:8080/api/policies \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
        "name":"Leave Policy",
        "code":"POL-LEAVE-001",
        "category":"Leave",
        "content":"Employees are entitled to annual leave.",
        "applicability":"ALL",
        "mandatory":true,
        "status":"DRAFT"
      }'
```

**Search policies**
```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/policies/search?category=Leave&sortBy=name&direction=asc&page=0&size=10"
```

**Delete a policy**
```bash
curl -X DELETE http://localhost:8080/api/policies/1 \
  -H "Authorization: Bearer <token>"
```

A ready-to-import Postman collection (`postman/hrmspolicies2_mysql.postman_collection.json`)
covers every endpoint above, plus positive and negative scenarios:
not-found (404), missing required fields (400), duplicate code (409),
invalid login (401), invalid sort field (400), and missing token
(401/403).

## Testing checklist (manual, via Postman)
- [x] Signup / Login (positive + wrong password)
- [x] Create policy (positive + missing fields + duplicate code)
- [x] Get all / Get by id (positive + not-found id)
- [x] Update (PUT full, PATCH partial)
- [x] Delete (positive + already-deleted id)
- [x] Search (keyword, category, status, mandatory, pagination, sorting, invalid sortBy)
- [x] Unauthenticated request to a protected endpoint (401/403)

## Git workflow used for this phase
Each enhancement was developed on its own feature branch and merged
into `main` via a merge commit (simulating a pull-request review),
with focused, descriptive commit messages:

```
feature/global-exception-handling    -> custom exceptions, @ControllerAdvice, ApiResponse/ApiError
feature/entity-relationships         -> Policy<->User Many-to-One/One-to-Many, audit fields
feature/advanced-search              -> dynamic filtering, sorting, pagination
feature/swagger-docs                 -> springdoc-openapi, JWT bearer scheme
feature/logging-and-profiles         -> SLF4J logging, dev/prod profiles
feature/frontend-api-response-compat -> frontend updated for the new response envelope
docs/readme-update                   -> this README
```
Run `git log --oneline --graph` to see the full history.

### Pushing to GitHub
```bash
git remote add origin https://github.com/YOUR_USERNAME/hrmspolicies2.git
git push -u origin main
```

## Important
Do not commit `.env.local`, real MySQL passwords, JWT secrets,
`.next/`, `target/`, or `node_modules/` — see `.gitignore`.

## Progression after this phase
Advanced APIs -> Exception Handling -> Relationships -> Swagger ->
Testing -> Logging -> Profiles -> **Security/JWT (done)** -> RBAC ->
Docker -> Deployment -> CI/CD
