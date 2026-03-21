# External Integrations

**Analysis Date:** 2026-03-21

## APIs & External Services

**Object Storage:**
- Timeweb Cloud S3-compatible storage (`s3.twcstorage.ru`) - Product image storage
  - SDK/Client: `software.amazon.awssdk:s3` v2.28.15 with custom endpoint override
  - Adapter: `backend/src/main/java/tj/radolfa/infrastructure/s3/S3ImageUploader.java` (active on `dev` + `prod` profiles)
  - Stub: `backend/src/main/java/tj/radolfa/infrastructure/s3/S3ImageUploaderStub.java` (test profile)
  - Auth env vars: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
  - Config env vars: `AWS_S3_BUCKET`, `AWS_S3_REGION` (default: `ru-1`), `AWS_S3_ENDPOINT` (default: `https://s3.twcstorage.ru`)
  - Public URL pattern: `https://{bucket}.s3.twcstorage.ru/{objectKey}`
  - Image sizes generated before upload: 150×150, 400×400, 800×800 (via Thumbnailator)

**Payment Gateway:**
- Provider: Not yet integrated (stub-only in dev/test; real adapter planned but not implemented)
  - Port interface: `tj.radolfa.application.ports.out.PaymentPort`
  - Dev stub: `backend/src/main/java/tj/radolfa/infrastructure/payment/PaymentPortStub.java`
  - Webhook receiver: `backend/src/main/java/tj/radolfa/infrastructure/web/PaymentWebhookController.java` at `POST /api/v1/webhooks/payment`
  - The webhook endpoint is public (no JWT); signature validation is delegated to `ConfirmPaymentUseCase`

**Notification Service:**
- SMS/Push: Not yet integrated (stub-only in dev/test)
  - Port interface: `tj.radolfa.application.ports.out.NotificationPort`
  - Dev stub: `backend/src/main/java/tj/radolfa/infrastructure/notification/NotificationPortStub.java`
  - Sends: order confirmation and order status update notifications

**Catalog Importer (Machine-to-Machine):**
- External importer service calling the backend API
  - Auth: long-lived API key sent as `X-Api-Key` header
  - Config env vars: `IMPORTER_BASE_URL`, `IMPORTER_API_KEY`, `IMPORTER_API_SECRET`, `SYSTEM_API_KEY`
  - Security filter: `backend/src/main/java/tj/radolfa/infrastructure/security/ServiceApiKeyFilter.java`

## Data Storage

**Databases:**
- PostgreSQL 16 (primary relational store)
  - Docker image: `postgres:16-alpine`
  - Connection env vars: `DB_URL` (default: `jdbc:postgresql://localhost:5432/radolfa_db`), `DB_USERNAME`, `DB_PASSWORD`
  - Spring datasource driver: `org.postgresql.Driver`
  - ORM/client: Spring Data JPA + Hibernate with `PostgreSQLDialect`
  - DDL management: Flyway (migrations in `backend/src/main/resources/db/migration/`, dev seeds in `db/migration-dev/`)
  - Hibernate DDL: `validate` (Flyway owns all schema changes)
  - Production: internal Docker network only, no port exposed externally

- Elasticsearch 8.12.2 (search index)
  - Docker image: `docker.elastic.co/elasticsearch/elasticsearch:8.12.2`
  - Connection env var: `ELASTICSEARCH_URIS` (default: `http://localhost:9200`)
  - Client: Spring Data Elasticsearch
  - Index document: `backend/src/main/java/tj/radolfa/infrastructure/search/ListingDocument.java`
  - Repository: `backend/src/main/java/tj/radolfa/infrastructure/search/ListingSearchRepository.java`
  - Adapter: `backend/src/main/java/tj/radolfa/infrastructure/search/ListingSearchAdapter.java`
  - Stub: `backend/src/main/java/tj/radolfa/infrastructure/search/ListingSearchStub.java` (test/disconnected)
  - Index settings: `backend/src/main/resources/elasticsearch/listing-settings.json`
  - Security disabled in Docker (`xpack.security.enabled=false`); single-node mode

**File Storage:**
- AWS S3-compatible (Timeweb Cloud) — see Object Storage above
- No local filesystem file storage for assets

**Caching:**
- None — no Redis or in-memory cache integration detected

## Authentication & Identity

**Auth Provider: Custom (self-hosted)**
- Implementation: Phone + OTP flow
  - User enters phone number → backend generates 4-digit OTP (valid 5 min)
  - OTP stored in `backend/src/main/java/tj/radolfa/infrastructure/security/OtpStore.java` (in-memory)
  - OTP verification issues JWT access token + refresh token as HTTP-only cookies
  - Auth controller: `backend/src/main/java/tj/radolfa/infrastructure/web/AuthController.java`

- JWT Implementation (JJWT 0.12.5):
  - Access token: configurable expiry via `JWT_EXPIRATION_MS` (default: 15 minutes)
  - Refresh token: configurable via `JWT_REFRESH_EXPIRATION_MS` (default: 7 days)
  - Secret: `JWT_SECRET` env var (min 32 chars for HS256)
  - Filter: `backend/src/main/java/tj/radolfa/infrastructure/security/JwtAuthenticationFilter.java`
  - Cookie manager: `backend/src/main/java/tj/radolfa/infrastructure/security/AuthCookieManager.java`

- Frontend token handling:
  - Axios instance at `frontend/src/shared/api/axios.ts` uses `withCredentials: true`
  - Interceptor handles 401 → calls `POST /api/v1/auth/refresh` → retries queued requests automatically

- Rate limiting (in-memory):
  - OTP requests: max 5 per phone per 60 minutes
  - OTP verify attempts: max 5 per phone per 15 minutes
  - IP-based: max 20 per hour
  - Config: `backend/src/main/java/tj/radolfa/infrastructure/security/RateLimiterService.java`

**Roles:** `USER`, `MANAGER`, `ADMIN`
- Security config: `backend/src/main/java/tj/radolfa/infrastructure/web/SecurityConfig.java`
- Protected route component (frontend): `frontend/src/shared/components/ProtectedRoute.tsx`

## Monitoring & Observability

**Health Checks:**
- Spring Boot Actuator exposes: `health`, `info`, `metrics` at `/actuator/*`
- Production: `show-details: when-authorized`
- Docker healthchecks poll `/actuator/health` on the backend container

**Error Tracking:**
- None — no Sentry, Datadog, or equivalent detected

**Logs:**
- Backend: SLF4J via Lombok `@Slf4j`; JSON file driver in Docker (`max-size: 50m`, `max-file: 5`)
- Dev profile: DEBUG for `tj.radolfa` and Spring Web + Hibernate SQL
- Prod profile: WARN root, INFO for `tj.radolfa`
- Frontend: console-based; no structured logging service

**API Documentation:**
- Swagger UI via springdoc-openapi at `/swagger-ui.html`
- OpenAPI config: `backend/src/main/java/tj/radolfa/infrastructure/web/OpenApiConfig.java`

## CI/CD & Deployment

**Hosting:**
- Single VPS (production target: `radolfa.site`)
- Deployment path: `/opt/radolfa/` on VPS

**CI Pipeline:**
- GitHub Actions (`.github/workflows/ci.yml`)
  - Triggers: push to any branch, PR to main
  - Backend: Java 21 Temurin, Maven test with PostgreSQL 16 service container
  - Frontend: Node.js 20, `npm ci`, lint, build check

**CD Pipeline:**
- GitHub Actions (`.github/workflows/deploy.yml`)
  - Triggers: push to `main` or manual dispatch
  - Builds Docker images → pushes to GHCR (`ghcr.io/idibek-alimov/claude-radolfa-backend` and `ghcr.io/idibek-alimov/claude-radolfa-frontend`)
  - Deploys via SSH to VPS using `appleboy/ssh-action` and `appleboy/scp-action`
  - Auto-rollback to `:latest` tag on failure
  - Smoke test: `curl http://localhost/nginx-health`
- Required GitHub Secrets: `VPS_HOST`, `VPS_DEPLOY_USER`, `VPS_SSH_PRIVATE_KEY`, `VPS_SSH_PORT`, `DOCKER_USERNAME`, `DOCKER_PASSWORD`, `GHCR_TOKEN`, `NEXT_PUBLIC_API_BASE_URL`

**Reverse Proxy:**
- Nginx Alpine (`.../nginx/nginx.conf` and `nginx/conf.d/`)
- Routes: `/api/*` → backend (port 8080), `/` → frontend (port 3000)
- SSL: Let's Encrypt certificates mounted at `/etc/letsencrypt`
- Production also proxies to ERPNext (gunicorn on host port 8000) via `host.docker.internal`

## Webhooks & Callbacks

**Incoming:**
- `POST /api/v1/webhooks/payment` - Payment provider callback to confirm completed payments
  - Public endpoint (no JWT required)
  - Payload: raw string body + `transactionId` query param
  - Handler: `backend/src/main/java/tj/radolfa/infrastructure/web/PaymentWebhookController.java`

**Outgoing:**
- None — no outgoing webhooks to external services detected

## Internationalization

**i18n Provider:** next-intl 4.8.3
- Config: `frontend/src/shared/i18n/config.ts`
- Locales: `en`, `ru`, `tj` (Tajik); default: `ru`
- Message files: `frontend/src/shared/i18n/locales/{en,ru,tj}.json`
- Request config: `frontend/src/shared/i18n/request.ts`

---

*Integration audit: 2026-03-21*
