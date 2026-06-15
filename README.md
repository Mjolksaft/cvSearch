# CV Search — Job Application Tracker

Spring Boot 3.4.4 REST API for browsing, bookmarking, and tracking job applications from Arbetsförmedlingen (JobTechDev). Includes AI-powered CV and cover letter generation.

## Tech Stack

- **Java 21** + **Spring Boot 3.4.4**
- **PostgreSQL 17** (production), **H2** (tests)
- **Spring Data JPA**, **Spring Security**, **Spring Validation**
- **MapStruct** (DTO mapping)
- **Testcontainers** (PostgreSQL integration tests)
- **Mockito** / **JUnit 5** (unit tests)
- **Thymeleaf** + **Flying Saucer** (PDF generation)
- **JWT** (jjwt 0.12.6 — wired, not yet enforced)
- **OpenAPI** (springdoc-openapi 2.8.6)

## How to Run

```bash
# Build and run all tests
mvn clean verify

# Build JAR and start the app
mvn clean package -DskipTests
java -jar target/cv-search-1.0.0.jar
```

Or with Docker:

```bash
docker compose up --build
```

> **Note:** Always use `mvn clean` before running. Incremental compilation has a known issue with MapStruct annotation processing.

## API Endpoints

### Jobs

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/jobs` | List jobs with filters, pagination, and sorting |
| `GET` | `/api/jobs/{id}` | Get job by ID |
| `POST` | `/api/jobs` | Create a job |
| `PUT` | `/api/jobs/{id}` | Full update |
| `PATCH` | `/api/jobs/{id}` | Partial update |
| `DELETE` | `/api/jobs/{id}` | Delete a job |
| `GET` | `/api/jobs/fetch?q=java` | Fetch jobs from Arbetsförmedlingen API |

### Job Filters, Pagination & Sorting

All optional, combine freely:

```
GET /api/jobs?company=google
GET /api/jobs?title=developer
GET /api/jobs?status=Applied
GET /api/jobs?location=stockholm
GET /api/jobs?saved=true
GET /api/jobs?appliedBefore=2026-06-01
GET /api/jobs?appliedAfter=2026-01-01
GET /api/jobs?page=0&size=20
GET /api/jobs?sort=appliedDate,desc
GET /api/jobs?company=spotify&saved=true&page=0&size=10&sort=appliedDate,desc
```

Default: page 0, 20 items, sorted by `appliedDate,desc` (newest first).

### Companies

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/companies` | List all companies |
| `GET` | `/api/companies/{id}` | Get company by ID |
| `POST` | `/api/companies` | Create a company |

### Users

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/users` | List all users |
| `GET` | `/api/users/{id}` | Get user by ID |
| `POST` | `/api/users` | Create a user |

### Profile

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/profile?userId=1` | Get user profile |
| `PUT` | `/api/profile?userId=1` | Create or replace profile |

### CV & Cover Letter Generation

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/jobs/{id}/cv-prompt?userId=1` | Get AI prompt for CV |
| `GET` | `/api/jobs/{id}/cover-letter-prompt?userId=1` | Get AI prompt for cover letter |
| `GET` | `/api/jobs/{id}/cv.pdf?userId=1` | Download CV as PDF |
| `POST` | `/api/jobs/{id}/cv?userId=1` | Download tailored CV PDF (with custom profile) |

## Example Workflow

```bash
# 1. Fetch jobs from Arbetsförmedlingen
GET /api/jobs/fetch?q=java

# 2. Bookmark interesting ones
PATCH /api/jobs/42
{ "saved": true }

# 3. View only bookmarks
GET /api/jobs?saved=true

# 4. Generate a tailored CV for a job
GET /api/jobs/42/cv.pdf?userId=1
```

## Project Structure

```
com.cvsearch/
├── CvSearchApplication.java
├── config/
│   ├── GlobalExceptionHandler.java
│   └── SecurityConfig.java              (permit-all, ready for auth)
├── company/
│   ├── Company.java
│   ├── CompanyController.java
│   ├── CompanyRepository.java
│   └── CompanyService.java
├── generation/
│   ├── CvPdfService.java               (Thymeleaf → PDF)
│   ├── CvPromptService.java            (AI prompt builder)
│   ├── GenerationController.java
│   └── dto/
│       ├── PromptResponse.java
│       └── TailoredProfileRequest.java
├── job/
│   ├── Job.java
│   ├── JobController.java
│   ├── JobFetcherService.java          (calls JobTechDev API)
│   ├── JobMapper.java
│   ├── JobRepository.java
│   ├── JobService.java
│   ├── JobNotFoundException.java
│   └── dto/
│       ├── JobAd.java
│       ├── JobPatchRequest.java
│       ├── JobRequest.java
│       ├── JobResponse.java
│       └── SearchResponse.java
├── user/
│   ├── User.java
│   ├── UserController.java
│   ├── UserRepository.java
│   └── UserService.java
└── userProfile/
    ├── UserProfile.java
    ├── UserProfileController.java
    ├── UserProfileRepository.java
    ├── UserProfileService.java
    ├── ProfileMapper.java
    └── dto/
        ├── ProfileRequest.java
        └── ProfileResponse.java
```

## Job Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `externalId` | Long | ID from Arbetsförmedlingen (unique) |
| `title` | String | Job title |
| `company` | Company | Many-to-one relationship |
| `description` | Text | Job description |
| `status` | String | e.g. `"Applied"`, `"Fetched"`, `"Interview"` |
| `location` | String | City |
| `deadline` | LocalDate | Application deadline |
| `appliedDate` | LocalDate | When you applied |
| `saved` | boolean | Bookmark (default `false`) |
| `employmentType` | String | e.g. `"Full-time"`, `"Part-time"` |

## Testing

```bash
mvn clean verify
```

- **Testcontainers** (PostgreSQL) for `@SpringBootTest` integration tests
- **H2** in-memory for `@DataJpaTest` repository tests
- **Mockito** for service-layer unit tests
- **Docker Desktop** must be running for integration tests

## What's Next

- Authentication (JWT or Basic Auth)
- Swagger/OpenAPI docs
- Web UI (Thymeleaf or SPA)
