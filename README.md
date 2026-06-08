# CV Search — Job Application Tracker API

Spring Boot 3.4.4 REST API for tracking job applications. PostgreSQL 17.

## Tech Stack

- **Java 21** + **Spring Boot 3.4.4**
- **PostgreSQL 17** (production), **H2** (tests)
- **Spring Data JPA**, **Spring Security**, **Spring Validation**
- **MapStruct** (DTO mapping)
- **Testcontainers** (PostgreSQL integration tests)
- **Mockito** (unit tests)
- **JWT** (jjwt 0.12.6), **OpenAPI** (springdoc-openapi 2.8.6)

## How to Run

```bash
# Build and run all tests
mvn clean verify

# Build JAR and start the app
mvn clean package -DskipTests
java -jar target/cv-search-1.0.0.jar
```

> **Note:** Always use `mvn clean` before running. Incremental compilation has a known issue with MapStruct annotation processing on this setup.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/jobs` | List all jobs (supports `?company=`, `?title=`, `?status=` filters) |
| `GET` | `/api/jobs/{id}` | Get job by ID |
| `POST` | `/api/jobs` | Create a job (requires `companyId`) |
| `PUT` | `/api/jobs/{id}` | Update a job |
| `PATCH` | `/api/jobs/{id}` | Partial update a job |
| `DELETE` | `/api/jobs/{id}` | Delete a job |
| `GET` | `/api/companies` | List all companies |
| `GET` | `/api/companies/{id}` | Get company by ID |
| `POST` | `/api/companies` | Create a company |
| `GET` | `/api/users` | List all users |
| `GET` | `/api/users/{id}` | Get user by ID |
| `POST` | `/api/users` | Create a user |

### Creating a job (two-step)

```json
POST /api/companies
{ "name": "Google", "website": "https://google.com/", "location": "Kristianstad" }
```

```json
POST /api/jobs
{ "title": "Software Engineer", "companyId": 1, "description": "Java backend role", "status": "Applied", "appliedDate": "2026-05-29" }
```

## Project Structure

```
com.cvsearch/
├── CvSearchApplication.java
├── config/
│   ├── GlobalExceptionHandler.java
│   └── SecurityConfig.java          (permit-all, ready for JWT)
├── company/
│   ├── Company.java
│   ├── CompanyController.java
│   ├── CompanyRepository.java
│   └── CompanyService.java
├── job/
│   ├── Job.java
│   ├── JobController.java
│   ├── JobMapper.java
│   ├── JobRepository.java
│   ├── JobService.java
│   ├── JobNotFoundException.java
│   └── dto/
│       ├── JobPatchRequest.java
│       ├── JobRequest.java
│       └── JobResponse.java
└── user/
    ├── User.java
    ├── UserController.java
    ├── UserRepository.java
    └── UserService.java
```

## Testing

```bash
mvn clean verify
```

Tests use:
- **Testcontainers** (PostgreSQL) for `@SpringBootTest` integration tests
- **H2** in-memory for `@DataJpaTest` repository tests
- **Mockito** for service-layer unit tests
- **Docker Desktop** must be running for integration tests

## Dependencies (added so far)

- `spring-boot-starter-security` + `jjwt` (API/impl/jackson 0.12.6)
- `springdoc-openapi-starter-webmvc-ui` 2.8.6
- `mapstruct` 1.6.3
- `testcontainers` (postgresql, junit-jupiter) 1.21.4

## Job fields
JobAd {
    String id;
    String headline;
    String employerName;
    String workplace;
    String description;
    String municipality;
    String region;
    String employmentType;
    String occupation;
    String occupationGroup;
    String occupationField;
    boolean experienceRequired;
    LocalDateTime publicationDate;
    LocalDateTime applicationDeadline;
    String webpageUrl;
    String applicationEmail;
    String applicationUrl;
}