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

A Firefox extension (`browser-extension/`, Manifest V2) that scrapes LinkedIn on demand. It opens LinkedIn in a background tab, scrapes 3 job cards, and saves them to the backend — all triggered from the web app or the extension popup.

### How it works

The flow is a pipeline across 4 script layers:

```
Web App (list.html)
  └─ postMessage({ type: "CRAWL_LINKEDIN" })
      └─ content-app.js (on localhost)
          └─ browser.runtime.sendMessage({ action: "crawlLinkedIn" })
              └─ background.js
                  ├─ Opens LinkedIn in background tab (active: false)
                  ├─ Waits for page load + 3s for cards to render
                  ├─ Sends "crawlJobs" message to content.js
                  │   └─ content.js scrapes up to 3 job cards
                  ├─ POST /api/jobs/bulk ← saves to backend
                  ├─ Closes LinkedIn tab
                  └─ Returns { status: "ok" } → triggers page reload
```

### What each script does

| Script | Runs on | Role |
|--------|---------|------|
| `content.js` | `linkedin.com/jobs/*` | Listens for `"crawlJobs"` message, scrapes 3 job cards using `data-occludable-job-id` |
| `content-app.js` | `http://localhost/*` | Listens for `CRAWL_LINKEDIN` postMessage from web app, forwards to background |
| `background.js` | extension background | Orchestrates the whole crawl — opens tab, scrapes, saves, closes, returns result |
| `popup/popup.js` | extension popup | Button to trigger the same crawl directly from the toolbar |

### Web app integration

The job list page (`jobs/list.html`) has a **"Find jobs on LinkedIn"** button that:
1. Shows a Bootstrap spinner and disables itself while crawling
2. Sends `postMessage({ type: "CRAWL_LINKEDIN" }, "*")`
3. Auto-reloads the page when the crawl completes

### Why it's fast

- **Already logged in** — uses your existing Firefox session
- **Background tab** — stays out of your way, no tab switching
- **Direct fetch** — no intermediate server, tab → localhost
- **Simple selectors** — relies on `data-occludable-job-id` (stable attribute)

### Setup

1. Open `about:debugging#/runtime/this-firefox`
2. Click **"Load Temporary Add-on…"**
3. Select the `browser-extension/manifest.json` file
4. Make sure CV Search is running on `http://localhost:8080`
5. Click **"Find jobs on LinkedIn"** on the web app (or the extension popup)

> **Note:** Firefox requires the content script match pattern for localhost without a port. The manifest uses `"http://localhost/*"` instead of `"http://localhost:8080/*"`. The `"http://localhost:8080/*"` permission is still listed separately so background.js can `fetch()` the backend.

## Example Workflow

```bash
# 1. Fetch jobs from Arbetsförmedlingen
GET /api/jobs/fetch?q=java

# 2. Or import from LinkedIn — click "Find jobs on LinkedIn" in the web app
#    (or use the extension popup button)

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
├── manifest.json                              (Firefox Manifest V2)
├── content.js                                 (runs on linkedin.com/jobs/*, scrapes job cards)
├── content-app.js                             (runs on localhost, forwards postMessage to background)
├── background.js                              (tab management, scraping orchestration)
├── popup/
│   ├── popup.html                             (extension popup UI)
│   └── popup.js                               (trigger crawl from popup button)

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
- ✅ LinkedIn Controller Extension — Firefox MV2 extension with background-tab scraping
  - `content.js` extracts 3 job cards using `data-occludable-job-id` on `linkedin.com/jobs/*`
  - `content-app.js` bridges from web app `postMessage` to extension `runtime.sendMessage`
  - `background.js` opens LinkedIn in background tab, orchestrates scrape, saves to backend, closes tab
  - Web app "Find jobs on LinkedIn" button shows loading spinner and auto-reloads on completion
  - Popup button for manual trigger from the toolbar

### Planned
- [ ] **Authentication** — JWT login/signup or Basic Auth
- [ ] **Swagger/OpenAPI** — Interactive API docs at `/swagger-ui.html`
- [ ] **Role-based access** — Lock down endpoints with Spring Security
- [ ] **PDF/Markdown download** — For generated CVs and cover letters
- [ ] **Enhanced frontend** — More filters, bulk actions, status management
