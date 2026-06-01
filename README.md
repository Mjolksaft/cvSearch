# CV Search — Job Application Tracker API

Spring Boot 3.4.4 REST API for tracking job applications. PostgreSQL 17.

## Current State

Single entity (`Job`) with a buggy controller that has duplicate `@GetMapping`, no `PUT`/`DELETE`, no service layer, no validation, and no error handling.

---

## Development

```bash
# Format code (auto-fix)
mvn spotless:apply

# Check formatting only
mvn spotless:check

# Lint (Checkstyle)
mvn checkstyle:check

# Build & test
mvn clean verify
```

CI runs on every push/PR to `main` — builds, checks formatting, lints, and runs tests.

---

## Todo List

### Phase 1 — Fix & Complete Core CRUD (High Priority)

- [x] Fix `JobController` — duplicate `@GetMapping` on `getJobById()` causes ambiguous mapping
- [x] Add `@PathVariable` to `getJobById(Long id)` — currently unbound
- [ ] Add `PUT /api/jobs/{id}` — update a job
- [ ] Add `DELETE /api/jobs/{id}` — delete a job
- [ ] Fix null checks in `create()` — return `400` instead of `null`
- [ ] Add `PATCH /api/jobs/{id}` — partial update (optional)

### Phase 2 — Service Layer & Validation (High Priority)

- [ ] Create `JobService` `@Service` class — move business logic out of controller
- [ ] Add `jakarta.validation` constraints to `Job` entity (`@NotBlank`, `@NotNull`, etc.)
- [ ] Add `@Valid` on `@RequestBody` in controller methods
- [ ] Add global exception handler (`@ControllerAdvice`) — structured error responses

### Phase 3 — DTOs & Search (Medium Priority)

- [ ] Create `JobRequest` / `JobResponse` DTOs — decouple API from entity
- [ ] Add custom query methods to `JobRepository` — `findByTitle`, `findByCompany`, `findByStatus`, `findByAppliedDateBetween`
- [ ] Add search/filter endpoint `GET /api/jobs/search?title=&company=&status=`

### Phase 4 — Infra & Quality (Medium Priority)

- [ ] Add `spring-boot-starter-validation` dependency (if missing)
- [ ] Fix `Dockerfile` — multi-stage build with Maven wrapper
- [ ] Add `src/test/java` — unit tests for service + controller
- [ ] Add testcontainers for integration tests with real PostgreSQL

### Phase 5 — Enhancements (Low Priority)

- [ ] Add more entities (`User`, `Company`, `Resume`, `Interview`) with full CRUD
- [ ] Add pagination & sorting to `GET /api/jobs`
- [ ] Add Spring Security / JWT auth
- [ ] Add Swagger/OpenAPI docs (`springdoc-openapi`)
- [ ] Add frontend (React / Vue)
