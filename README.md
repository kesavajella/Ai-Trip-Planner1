# IntelliTrip — AI Trip Planner

Java full-stack AI trip planner: a **plain HTML / CSS / JS** frontend (no framework, no build step) with a **Spring Boot + MySQL** backend exposing a REST API.

The backend also serves the static frontend, so the whole app runs from a single Spring Boot process.

## Architecture

```
IntelliTrip/
├── frontend/                       # Vanilla HTML/CSS/JS SPA
│   ├── index.html                  # App shell + hash router
│   ├── public/
│   │   ├── css/styles.css          # Plain CSS (Tailwind-equivalent tokens)
│   │   ├── js/app.js               # Router, auth, API client, all pages
│   │   └── favicon.svg
│   └── package.json                # `npm run serve` for local dev (optional)
│
├── src/main/java/com/intellitrip/  # Spring Boot backend
│   ├── config/                     # Security (CORS), WebMvc (serves SPA), DataInitializer
│   ├── controller/                 # REST controllers (*RestController)
│   ├── dto/                        # Request/Response DTOs
│   ├── model/                      # JPA entities
│   ├── repository/                 # Spring Data JPA repos
│   └── service/                    # Business logic
└── src/main/resources/
    ├── spa/                        # Built frontend (copied from frontend/public by `mvn package`)
    ├── application.properties
    └── db/schema.sql
```

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+
- (Node.js is downloaded automatically by the Maven build; not required locally)

## Setup

### 1. Database

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 2. Configure

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=your_password
google.ai.api-key=your_google_ai_api_key   # optional; falls back to a sample itinerary
```

### 3. Build & Run

`mvn package` copies the frontend assets into `src/main/resources/spa` and packages the Spring Boot jar.

```bash
mvn clean package
java -jar target/intellitrip-backend-1.0.0.jar
```

The app is then available at **http://localhost:8080** (serves both the API and the SPA).

#### During development (no build)

The frontend is static — open `frontend/index.html` directly, or serve the folder:

```bash
cd frontend
npm run serve        # serves frontend/public on http://localhost:5173
```

It talks to the backend's REST API at `/api` (run the backend separately with `mvn spring-boot:run`).

Frontend dev server: http://localhost:5173

## Authentication

Session-cookie based (Spring Security). The SPA reads auth state from `GET /api/auth/me`
and posts credentials to Spring's form login (`/perform_login`) and signup (`/signup`).
CORS is enabled for `localhost:5173` (dev) and `localhost:8080`.

## Default Accounts

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@intellitrip.com | admin123 |
| User | amelia@example.com | password |

## REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/auth/me` | Current user (public) |
| POST | `/api/auth/logout` | Log out |
| GET | `/api/dashboard` | Dashboard stats + notifications |
| POST | `/api/generate-trip` | Generate AI itinerary |
| GET | `/api/trips` | List user trips |
| GET | `/api/trips/{id}` | Trip details |
| POST | `/api/trips` | Create trip |
| PUT | `/api/trips/{id}` | Update trip |
| PATCH | `/api/trips/{id}/status` | Update trip status |
| DELETE | `/api/trips/{id}}` | Delete trip |
| GET | `/api/admin/stats` | Admin analytics (ADMIN only) |

## Tech Stack

- **Frontend**: Vanilla HTML, CSS, JavaScript (hash-based router, fetch API). No framework or build step required.
- **Backend**: Spring Boot 3.4.5, Spring Security, Spring Data JPA
- **Database**: MySQL 8.x
- **AI**: Google Gemini API (REST), with a built-in fallback itinerary
- **Build**: Maven (frontend assets copied via `maven-resources-plugin`)

## License

MIT
