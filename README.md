# CV Search — Job Application Tracker

Spring Boot 3.4.4 REST API for browsing, bookmarking, and tracking job applications from Arbetsförmedlingen (JobTechDev) and LinkedIn (via a companion Chrome extension). Includes AI-powered CV and cover letter generation.

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
- **LinkedIn Controller Extension** (Firefox, Manifest V2)

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
> Docker Desktop must be running for integration tests (Testcontainers).

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
| `POST` | `/api/jobs/bulk` | Bulk create/import jobs (used by Chrome extension) |
| `PATCH` | `/api/jobs/external/{externalId}` | Update description by LinkedIn/Arbetsförmedlingen external ID |
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

## LinkedIn Controller Extension

A Chrome extension (`browser-extension/`) that scrapes LinkedIn on demand, controlled by trigger elements in your web app pages.

### How it works

The extension is a **stateless companion** — no popup, no UI, no auto-scraping. It only activates when your web app tells it to.

**Trigger mechanism:** Your Thymeleaf templates inject a hidden element like:
```html
<div id="cvsearch-command"
     data-action="search"
     data-query="Java developer"
     data-location="Stockholm"
     data-max="25">
```

The extension's content script detects this element and tells the background script to:
1. Open a LinkedIn tab (search or job detail)
2. Scrape the data using the same extraction logic
3. POST/PATCH the data to `localhost:8080`
4. Close the LinkedIn tab
5. Focus back on your web app

### Two flows

| Flow | Trigger | What happens |
|------|---------|-------------|
| **Search jobs** | Click "Find jobs" in web app | Opens LinkedIn search, scrapes job cards, bulk-imports via `POST /api/jobs/bulk`, closes tab |
| **Get description** | Click a job title | Opens the LinkedIn job detail, scrapes description using `componentkey="JobDetails_AboutTheJob_<id>"`, PATCHes it to the job, closes tab |

### Why it's fast

- **Already logged in** — uses your existing Chrome session, no separate browser
- **Cached pages** — LinkedIn's SPA is already in your browser cache from normal use
- **No networkidle waits** — scrapes as soon as the componentkey element renders
- **No JSON-RPC overhead** — direct from tab to `localhost`

### Setup

1. Open `chrome://extensions`
2. Enable "Developer mode" (toggle top-right)
3. Click "Load unpacked"
4. Select the `browser-extension/` directory
5. Make sure CV Search is running on `http://localhost:8080`

> **Note:** LinkedIn uses hashed CSS class names that change per deployment. The extension relies on stable attributes:
> - `data-occludable-job-id` / `data-anonymize-*` for finding job cards
> - `componentkey="JobDetails_AboutTheJob_<id>"` for finding descriptions
> - `href*="/company/"` for finding company names

## Example Workflow

```bash
# 1. Fetch jobs from Arbetsförmedlingen
GET /api/jobs/fetch?q=java

# 2. Or import from LinkedIn via the browser extension (click "Find jobs" in the web app)

# 3. Bookmark interesting ones
PATCH /api/jobs/42
{ "saved": true }

# 4. View only bookmarks
GET /api/jobs?saved=true

# 5. Generate a tailored CV for a job
GET /api/jobs/42/cv.pdf?userId=1
```

## Project Structure

```
com.cvsearch/
├── CvSearchApplication.java
├── config/
│   ├── GlobalExceptionHandler.java           (@ControllerAdvice)
│   ├── SecurityConfig.java                   (permit-all, CORS open, ready for JWT)
│   └── WebConfig.java                        (CORS config)
├── company/
│   ├── Company.java
│   ├── CompanyController.java
│   ├── CompanyRepository.java
│   └── CompanyService.java
├── generation/
│   ├── CvPdfService.java                     (Thymeleaf → Flying Saucer PDF)
│   ├── CvPromptService.java                  (AI prompt builder)
│   ├── GenerationController.java
│   └── dto/
│       ├── PromptResponse.java
│       └── TailoredProfileRequest.java
├── job/
│   ├── Job.java
│   ├── JobController.java
│   ├── JobFetcherService.java                (calls JobTechDev API)
│   ├── JobMapper.java                        (MapStruct)
│   ├── JobRepository.java
│   ├── JobService.java
│   ├── JobNotFoundException.java
│   └── dto/
│       ├── BulkJobItem.java                  (bulk import from Chrome extension)
│       ├── DescriptionUpdateRequest.java     (PATCH by external ID)
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
├── userProfile/
│   ├── UserProfile.java
│   ├── UserProfileController.java
│   ├── UserProfileRepository.java
│   ├── UserProfileService.java
│   ├── ProfileMapper.java
│   └── dto/
│       ├── ProfileRequest.java
│       └── ProfileResponse.java
└── web/
    └── PageController.java                    (Thymeleaf frontend pages)

browser-extension/
├── manifest.json                              (Chrome Manifest V3)
├── content.js                                 (trigger detection on web app)
├── background.js                              (tab management, scraping orchestration)
├── scraper-search.js                          (injected into LinkedIn search pages)
├── scraper-description.js                     (injected into LinkedIn job detail pages)
├── generate-icons.mjs                         (icon generator — run with Node.js)
└── icons/                                     (generated PNG icons)

src/main/resources/
├── templates/
│   ├── jobs/
│   │   ├── list.html                          (job list with search/pagination)
│   │   └── detail.html                        (job detail view)
│   ├── profile.html                           (user profile edit)
│   └── index.html                             (home page)
└── static/
    └── js/
        └── app.js                             (bookmark toggle, profile form JS)
```

## Job Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `externalId` | Long | ID from Arbetsförmedlingen / LinkedIn (unique) |
| `title` | String | Job title |
| `company` | Company | Many-to-one relationship |
| `description` | Text | Job description |
| `status` | String | e.g. `"Applied"`, `"Fetched"`, `"Interview"` |
| `location` | String | City |
| `deadline` | LocalDate | Application deadline |
| `appliedDate` | LocalDate | When you applied |
| `saved` | boolean | Bookmark (default `false`) |
| `employmentType` | String | e.g. `"Full-time"`, `"Part-time"` |
| `website` | String | Job posting URL |

## Testing

```bash
mvn clean verify
```

17 tests total:
- **JobControllerTest** (1 test) — `@WebMvcTest` with Mockito
- **JobRepositoryTest** (1 test) — `@DataJpaTest` with H2
- **JobServiceTest** (2 tests) — `@SpringBootTest` with Testcontainers (PostgreSQL 17)
- **JobServiceUnitTest** (13 tests) — pure Mockito unit tests

## Current State & What's Next

### Implemented
- ✅ Job CRUD + partial update (PATCH)
- ✅ Job fetch from Arbetsförmedlingen (JobTechDev) with deduplication
- ✅ Company CRUD
- ✅ User CRUD
- ✅ UserProfile entity + CRUD
- ✅ AI prompt generation for CV / cover letter
- ✅ CV PDF generation (Thymeleaf → Flying Saucer)
- ✅ Job search with filters (company, title, status, location, saved, date range)
- ✅ Pagination & sorting on `GET /api/jobs`
- ✅ Bulk import endpoint (`POST /api/jobs/bulk`) with auto-create company + externalId dedup
- ✅ Description update by external ID (`PATCH /api/jobs/external/{id}`)
- ✅ CORS open for extension (`allowedOrigins("*")`)
- ✅ Thymeleaf frontend (job list, detail, profile pages)
- ✅ All security endpoints set to `permitAll()`
- ✅ LinkedIn Controller Extension — Chrome MV3 extension controlled by trigger elements in web app
  - `content.js` watches for `#cvsearch-command` triggers on web app pages
  - `background.js` opens LinkedIn tabs, injects scrapers, POSTs data, closes tabs, returns focus
  - `scraper-search.js` extracts job cards using `data-occludable-job-id` / `data-anonymize-*` attributes
  - `scraper-description.js` extracts job descriptions using `componentkey` attribute
  - Search flow: "Find jobs" button → modal → triggers extension → scrapes → bulk imports
  - Description flow: auto-fetch on detail page + manual button → triggers extension → scrapes → PATCHes
- ✅ MCP server removed — replaced by faster, simpler browser extension

### Planned
- [ ] **Authentication** — JWT login/signup or Basic Auth
- [ ] **Swagger/OpenAPI** — Interactive API docs at `/swagger-ui.html`
- [ ] **Role-based access** — Lock down endpoints with Spring Security
- [ ] **PDF/Markdown download** — For generated CVs and cover letters
- [ ] **Enhanced frontend** — More filters, bulk actions, status management
