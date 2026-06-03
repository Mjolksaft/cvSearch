# CV Search — Job Application Tracker API

Spring Boot 3.4.4 REST API for tracking job applications. PostgreSQL 17.

---

## Roadmap

### Step 1 — Service Layer & Validation

**What:** Move business logic out of the controller into a `@Service` class. Add validation so bad data is rejected before it hits the DB.

**Concepts to learn:**
- `@Service` / dependency injection
- `@Valid` + `jakarta.validation` annotations (`@NotBlank`, `@NotNull`)
- Spring Boot validation starter
- `@ControllerAdvice` for global exception handling

---

### Step 2 — DTOs & Search

**What:** Stop exposing your `Job` entity directly in API responses. Create dedicated request/response classes so the API is decoupled from the DB schema. Add search/filter endpoints.

**Concepts to learn:**
- DTO pattern (Data Transfer Object)
- Manual mapping vs MapStruct
- Spring Data JPA custom query methods
- Query parameters with `@RequestParam`

---

### Step 3 — Docker & Testing

**What:** Containerize the app properly. Add automated tests.

**Concepts to learn:**
- Multi-stage Docker builds
- Testcontainers (PostgreSQL in tests)
- `@WebMvcTest`, `@DataJpaTest`
- Mockito for service tests

---

### Step 4 — More Entities & Auth

**What:** Add `User`, `Company`, `Resume`, `Interview` entities with full CRUD. Secure the API.

**Concepts to learn:**
- JPA entity relationships (`@OneToMany`, `@ManyToOne`)
- Spring Security + JWT
- Role-based access
- Pagination & sorting with Spring Data
- OpenAPI / Swagger docs (`springdoc-openapi`)

---

## Development

```bash
# Build & test
mvn clean verify
```
