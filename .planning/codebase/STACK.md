# Technology Stack

**Analysis Date:** 2026-03-21

## Languages

**Primary:**
- Java 21 - Backend application (`backend/src/main/java/`)
- TypeScript 5 (strict mode) - Frontend application (`frontend/src/`)

**Secondary:**
- SQL - Database migrations (`backend/src/main/resources/db/migration/`)

## Runtime

**Backend Environment:**
- JVM (Java 21, Temurin distribution in CI)
- Spring Boot Maven Wrapper: `./mvnw`

**Frontend Environment:**
- Node.js 20 (CI target; runtime in Docker containers)
- NPM (lockfile: `frontend/package-lock.json` present)

**Package Manager:**
- Backend: Maven (wrapper at `backend/mvnw`)
- Frontend: NPM (`frontend/package-lock.json`)

## Frameworks

**Backend Core:**
- Spring Boot 3.4.4 - Application framework
- Spring Web (MVC) - REST API layer
- Spring Data JPA + Hibernate - ORM / persistence
- Spring Security - Authentication and authorization
- Spring Data Elasticsearch - Search integration
- Spring Boot Actuator - Health checks and metrics

**Frontend Core:**
- Next.js 15 (App Router, Turbopack in dev) - Framework (`frontend/next.config.mjs`)
- React 19 - UI library
- TanStack Query v5 - Server state management (`frontend/src/shared/providers/QueryProvider.tsx`)
- next-intl 4.8.3 - Internationalization (en, ru, tj locales)
- Framer Motion 12 - Animation library

**UI Components:**
- Tailwind CSS 3.4 - Utility-first styling (`frontend/tailwind.config.ts`)
- Shadcn UI (via Radix UI primitives) - Component library, stored in `frontend/src/shared/ui/`
- Radix UI packages: alert-dialog, avatar, dialog, dropdown-menu, slot, tabs
- lucide-react - Icon set
- sonner - Toast notifications
- class-variance-authority + clsx + tailwind-merge - Class utilities

**HTTP Client (Frontend):**
- Axios 1.x - HTTP client, single instance at `frontend/src/shared/api/axios.ts`

**Build/Dev:**
- Turbopack - Dev server bundler (Next.js 15 default)
- PostCSS 8 - CSS processing
- ESLint 9 + eslint-config-next - Linting
- Maven Compiler Plugin with annotation processors (Lombok + MapStruct binding)

## Key Dependencies (Backend)

**Critical:**
- Lombok 1.18.36 - Boilerplate reduction (`@Data`, `@Slf4j`, `@Builder`, etc.)
- MapStruct 1.5.5.Final - Compile-time mapping between Domain ↔ DTO ↔ JPA Entity
- JJWT 0.12.5 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) - JWT creation and validation
- AWS SDK v2 2.28.15 (`software.amazon.awssdk:s3`) - S3-compatible object storage
- Thumbnailator 0.4.21 - Server-side image resizing (150×150, 400×400, 800×800)
- Flyway Core + flyway-database-postgresql - Database migrations (owns all DDL)
- springdoc-openapi-starter-webmvc-ui 2.7.0 - Swagger UI at `/swagger-ui.html`

**Infrastructure:**
- postgresql (runtime) - PostgreSQL JDBC driver

**Testing:**
- spring-boot-starter-test - JUnit 5, Mockito, AssertJ
- spring-boot-testcontainers - Testcontainers integration
- testcontainers:junit-jupiter - JUnit 5 lifecycle support
- testcontainers:postgresql - PostgreSQL container for tests
- spring-security-test - Security test support

## Configuration

**Backend Environment:**
- Profiles: `dev` (default), `prod`, `test`, `local` (gitignored overlay)
- Config files:
  - `backend/src/main/resources/application.yml` - Shared base config
  - `backend/src/main/resources/application-dev.yml` - Dev overrides (verbose SQL, relaxed CORS)
  - `backend/src/main/resources/application-prod.yml` - Prod overrides (restricted CORS, quiet logging)
  - `backend/src/main/resources/application-local.yml.example` - Template for local S3 credentials
- Activation: `SPRING_PROFILES_ACTIVE` environment variable
- Required env vars in production:
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - PostgreSQL connection
  - `ELASTICSEARCH_URIS` - Elasticsearch endpoint
  - `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_S3_BUCKET`, `AWS_S3_REGION`, `AWS_S3_ENDPOINT`
  - `JWT_SECRET`, `JWT_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`
  - `SYSTEM_API_KEY` - Machine-to-machine API key

**Frontend Environment:**
- `NEXT_PUBLIC_API_BASE_URL` - Backend base URL (baked in at build time; empty string in Docker, proxied via nginx)
- `BACKEND_INTERNAL_URL` - Internal Docker URL for Next.js API rewrites (default: `http://localhost:8080`)
- TypeScript strict mode: `noImplicitAny: true`, `strict: true` in `frontend/tsconfig.json`
- Path alias: `@/*` maps to `frontend/src/*`
- Image optimization disabled (`unoptimized: true`) — all images are S3 URLs served externally

**Build:**
- Backend: `backend/Dockerfile` (Maven build → JVM image)
- Frontend: `frontend/Dockerfile` (Next.js standalone output for minimal image size)
- Images pushed to GHCR: `ghcr.io/idibek-alimov/claude-radolfa-backend` and `ghcr.io/idibek-alimov/claude-radolfa-frontend`

## Platform Requirements

**Development:**
- Docker + Docker Compose (for PostgreSQL 16 and Elasticsearch 8.12.2)
- Java 21 (Temurin)
- Node.js 20+
- Dev command: `docker-compose up -d db elasticsearch` then `./mvnw spring-boot:run` and `npm run dev --prefix frontend`

**Production:**
- Single VPS running Docker Compose (`docker-compose.prod.yml`)
- Nginx (alpine) as reverse proxy, SSL termination via Let's Encrypt
- PostgreSQL 16 Alpine (512 MB memory limit)
- Elasticsearch 8.12.2 (512 MB–1 GB memory limit)
- Spring Boot container (768 MB limit)
- Next.js standalone container (512 MB limit)
- Nginx (128 MB limit)
- Deployment target: `radolfa.site` / `www.radolfa.site`

---

*Stack analysis: 2026-03-21*
