# Codebase Concerns

**Analysis Date:** 2026-03-21

---

## Tech Debt

**Hardcoded OTP Value (Critical):**
- Issue: `generateRandomOtp()` always returns `"1234"` — random OTP logic is commented out
- Files: `backend/src/main/java/tj/radolfa/infrastructure/security/OtpStore.java` (line 88)
- Impact: Any phone number can be verified with OTP `1234` in any environment where this code runs. This is a complete authentication bypass.
- Fix approach: Integrate a real SMS provider (e.g., Twilio, Eskiz.uz), restore the `SecureRandom` logic, and use env-profile conditional to keep `"1234"` only for the `test` profile

**Startup Re-index Hack:**
- Issue: `RadolfaApplication` contains a `CommandLineRunner` bean labeled "TEMPORARY HACK" that manually injects a fake `ROLE_ADMIN` auth into `SecurityContextHolder` and calls `SearchController.reindex()` directly on startup
- Files: `backend/src/main/java/tj/radolfa/RadolfaApplication.java` (lines 26-49)
- Impact: Couples infrastructure startup to a controller bean; bypasses security context properly; using `System.out.println` instead of logger; brittle — any exception is swallowed silently
- Fix approach: Create a proper `ElasticsearchIndexInitializerService` in the infrastructure layer that calls `ListingIndexPort` directly, triggered via `ApplicationStartedEvent`; remove the controller dependency from the main class

**`ProductBase.externalRef` as Orphan Field:**
- Issue: `ProductBase` domain model requires a non-null `externalRef` field, but natively-created products get a random `INTERNAL-XXXXXXXX` UUID placeholder. The field represents an external ERP concept that no longer applies.
- Files: `backend/src/main/java/tj/radolfa/domain/model/ProductBase.java`, `backend/src/main/java/tj/radolfa/application/services/CreateProductService.java` (line 60)
- Impact: Domain invariant enforces a field that has no business meaning; confusion for future developers; `applyExternalUpdate()` method is also vestigial
- Fix approach: Remove `externalRef` constraint from `ProductBase` domain model; make it nullable or remove it; clean up `applyExternalUpdate()` if no importer is active

**Dead Test Files (Commented-Out Tests):**
- Issue: Two test files are entirely commented out — they were for the now-deleted ERP sync feature
- Files:
  - `backend/src/test/java/tj/radolfa/application/services/SyncProductHierarchyServiceTest.java` — 313 lines, 100% commented
  - `backend/src/test/java/tj/radolfa/infrastructure/web/ErpSyncControllerTest.java` — 170 lines, 100% commented
- Impact: Dead code in the test suite; misleads test coverage counts; clutters the codebase
- Fix approach: Delete both files; they reference classes that no longer exist

**Orphaned `importer` Config Block:**
- Issue: `application.yml` defines `importer.base-url`, `importer.api-key`, and `importer.api-secret` config keys. No `ImporterProperties` class or importer client exists in the codebase — these properties are not bound to anything.
- Files: `backend/src/main/resources/application.yml` (lines 67-70)
- Impact: Confusing config; stale documentation about an "external catalogue importer"
- Fix approach: Remove the `importer:` block from `application.yml`

**`ServiceApiKeyFilter` Uses Timing-Safe Comparison but Exposes ADMIN Access Broadly:**
- Issue: The `X-Api-Key` header grants `ROLE_ADMIN` to any request that presents the correct key. The key is machine-to-machine auth originally designed for ERPNext. No ERPNext integration exists anymore, but the filter and config (`SYSTEM_API_KEY`) remain active in production.
- Files: `backend/src/main/java/tj/radolfa/infrastructure/security/ServiceApiKeyFilter.java`, `backend/src/main/resources/application.yml` (line 104)
- Impact: A leaked `SYSTEM_API_KEY` gives full admin access to the platform without any OTP or user account
- Fix approach: Assess whether any current service still uses this key; if not, remove the filter and config key; if needed, scope the key to specific endpoints rather than granting blanket ADMIN access

**`S3ImageUploaderStub` Only Active for `test` Profile:**
- Issue: `S3ImageUploaderStub` is annotated `@Profile("test")` but the comment says it was intended to work for `dev` too. The `dev` profile therefore requires a working AWS/S3 connection for image upload to function.
- Files: `backend/src/main/java/tj/radolfa/infrastructure/s3/S3ImageUploaderStub.java` (line 29)
- Impact: Developers running the `dev` profile need S3 credentials configured to test image uploads
- Fix approach: Add `"dev"` to the `@Profile` annotation if local dev without S3 is desired

---

## Security Considerations

**OTP Always `1234` (Also a Security Issue):**
- Risk: Authentication bypass — any account can be accessed with OTP `1234`
- Files: `backend/src/main/java/tj/radolfa/infrastructure/security/OtpStore.java`
- Current mitigation: None
- Recommendations: Never deploy to production until SMS provider is integrated and random OTP is restored

**Actuator Endpoints Publicly Exposed:**
- Risk: `/actuator/health`, `/actuator/info`, `/actuator/metrics` are publicly accessible without authentication
- Files: `backend/src/main/resources/application.yml` (lines 56-64), `backend/src/main/java/tj/radolfa/infrastructure/web/SecurityConfig.java` (line 98)
- Current mitigation: Exposure is limited to `health`, `info`, `metrics` (not `env`, `beans`, `shutdown`)
- Recommendations: Restrict actuator endpoints to internal network or require auth in production; at minimum remove `metrics` from public exposure

**In-Memory OTP Store — Multi-Instance Risk:**
- Risk: If the backend scales to multiple instances, OTPs generated on one node cannot be verified on another
- Files: `backend/src/main/java/tj/radolfa/infrastructure/security/OtpStore.java`
- Current mitigation: Single VPS deployment (documented constraint) — currently safe
- Recommendations: Document this limitation explicitly; plan Redis-backed OTP store before any horizontal scaling

**In-Memory Rate Limiter — Multi-Instance Risk:**
- Risk: Same as OTP store — rate limit state is per-instance, not shared
- Files: `backend/src/main/java/tj/radolfa/infrastructure/security/RateLimiterService.java`
- Current mitigation: Single VPS deployment
- Recommendations: Same Redis migration path as OTP store

**JWT Default Secret in Config:**
- Risk: `JWT_SECRET` falls back to `dev-secret-key-must-be-at-least-32-characters-long-for-hs256` if env var is not set
- Files: `backend/src/main/resources/application.yml` (line 88)
- Current mitigation: Comment warns this is a dev value; production must override
- Recommendations: Add startup validation that rejects the default secret when `SPRING_PROFILES_ACTIVE=prod`

**IP Extraction Trusts `X-Real-IP` Without Restriction:**
- Risk: `X-Real-IP` header in `AuthController.extractClientIp()` is trusted blindly — a client not behind nginx could spoof this header to bypass IP rate limiting
- Files: `backend/src/main/java/tj/radolfa/infrastructure/web/AuthController.java` (lines 258-264)
- Current mitigation: Production runs behind nginx, which sets this header correctly
- Recommendations: Restrict trust to known proxy IPs or use `X-Forwarded-For` with proper parsing

---

## Performance Bottlenecks

**Checkout N+1 SKU Loads:**
- Problem: `CheckoutService.bestOfUnitPrice()` calls `loadSkuPort.findSkuById()` and `loadBestActiveDiscountPort.findBestActiveForItemCode()` per cart item, inside the same `@Transactional` method that also validates stock per item
- Files: `backend/src/main/java/tj/radolfa/application/services/CheckoutService.java` (lines 90-98, 159-179, 183-196)
- Cause: Each cart item triggers at minimum 3 separate DB round-trips (stock check, SKU load for pricing, discount load); a cart with 5 items = 15+ queries
- Improvement path: Batch-load all SKU IDs and discount codes before the loop; use `IN` queries

**Elasticsearch Index Out of Sync at Startup:**
- Problem: The startup re-index blocks application readiness and can fail silently (exception is caught and printed to stderr)
- Files: `backend/src/main/java/tj/radolfa/RadolfaApplication.java`
- Cause: The hack uses a try/catch that swallows failures; if ES is unavailable the app starts with a stale or empty index
- Improvement path: Use a proper health check / readiness probe; index asynchronously post-startup with retry

**Large Admin Page Component (899 lines):**
- Problem: `manage/page.tsx` is a single 899-line "use client" component handling products, users, categories, colors, loyalty tiers, and search reindex in one file
- Files: `frontend/src/app/(admin)/manage/page.tsx`
- Cause: All admin tab content is co-located in one file rather than decomposed into separate widget/feature components
- Improvement path: Extract each tab (Products, Users, Categories, Colors, Loyalty) into dedicated widget components under `frontend/src/widgets/`

---

## Fragile Areas

**Stock Decrement Race Condition:**
- Files: `backend/src/main/java/tj/radolfa/application/services/UpdateProductStockService.java`
- Why fragile: `decrement()` does a read-then-write: loads `Sku`, checks stock, computes new value, saves. Two concurrent checkouts for the same SKU could both read `stock=1`, both compute `newStock=0`, and both succeed — resulting in stock going to `-1` in practice (optimistic locking via `@Version` on `SkuEntity` mitigates this, but the error surfaced would be a confusing `OptimisticLockException` rather than an `out of stock` error)
- Safe modification: The `@Version` field on `BaseAuditEntity` does provide protection against lost updates, but the user experience on conflict is poor. Consider adding a DB-level `CHECK (stock_quantity >= 0)` constraint and a retry mechanism in `CheckoutService`
- Test coverage: No test covers concurrent checkout of the same SKU

**Elasticsearch Data Divergence:**
- Files: `backend/src/main/java/tj/radolfa/infrastructure/search/ListingSearchAdapter.java`
- Why fragile: ES indexing is fire-and-forget — write failures are logged at `WARN` but silently dropped. The ES index can diverge from the PostgreSQL source of truth any time indexing fails. The startup re-index mitigates this on restart but not during uptime.
- Safe modification: Monitor `WARN` logs for index failures; the `/api/v1/search/reindex` admin endpoint exists to force a full re-sync
- Test coverage: ES adapter has no unit or integration tests

**`CheckoutService` Step Ordering Risk:**
- Files: `backend/src/main/java/tj/radolfa/application/services/CheckoutService.java`
- Why fragile: Loyalty points are redeemed (step 6) before the order is saved (step 9). If `saveOrderPort.save()` throws after points are already deducted, points are lost permanently (within the same `@Transactional` it would rollback, but the `redeemLoyaltyPointsUseCase` itself is also transactional — if it commits a nested transaction, rollback of the outer may not undo it depending on propagation)
- Safe modification: Verify `@Transactional` propagation on `RedeemLoyaltyPointsService` — it should use `Propagation.REQUIRED` (join outer) not `REQUIRES_NEW`
- Test coverage: Not tested with transaction rollback scenarios

**Object[] Row Mapping in Repositories:**
- Files: `backend/src/main/java/tj/radolfa/infrastructure/persistence/repository/ListingVariantRepository.java`, `backend/src/main/java/tj/radolfa/infrastructure/search/ListingSearchAdapter.java`
- Why fragile: Grid queries return `Object[]` arrays with positional column indices (e.g., `row[0]`, `row[7]`, `row[12]`). Column positions are documented in comments but any JPQL query change that adds/reorders columns will silently break mapping at runtime
- Safe modification: When modifying any `@Query` that returns `Object[]`, update column index comments in all consuming mappers; consider migrating to JPA projections or result-set DTOs
- Test coverage: `ListingControllerTest` provides some coverage but only as a mock MVC test

---

## Test Coverage Gaps

**Checkout Flow Untested:**
- What's not tested: `CheckoutService` — the most business-critical path in the application
- Files: `backend/src/main/java/tj/radolfa/application/services/CheckoutService.java`
- Risk: Stock decrement errors, discount calculation bugs, loyalty point deduction failures could ship undetected
- Priority: High

**Authentication Flow Untested:**
- What's not tested: `AuthController`, `OtpStore`, `JwtUtil`, `JwtAuthenticationFilter`
- Files: `backend/src/main/java/tj/radolfa/infrastructure/web/AuthController.java`, `backend/src/main/java/tj/radolfa/infrastructure/security/`
- Risk: OTP bypass issues, JWT token refresh bugs, cookie handling errors
- Priority: High

**Cart Operations Untested:**
- What's not tested: `AddToCartService`, `RemoveFromCartService`, `UpdateCartItemQuantityService`
- Files: `backend/src/main/java/tj/radolfa/application/services/` (cart services)
- Risk: Cart state corruption, quantity validation bugs
- Priority: Medium

**Payment Flow Untested:**
- What's not tested: `InitiatePaymentService`, `ConfirmPaymentService`, `RefundPaymentService`
- Files: `backend/src/main/java/tj/radolfa/application/services/` (payment services)
- Risk: Payment confirmation bugs, refund logic errors
- Priority: High

**Frontend Has No Tests:**
- What's not tested: All frontend components, hooks, API layer
- Files: `frontend/src/` (entire directory)
- Risk: UI regressions go undetected; API contract changes break silently
- Priority: Medium

---

## Missing Critical Features

**SMS Provider Not Integrated:**
- Problem: OTP authentication is entirely non-functional in production — `generateRandomOtp()` hardcodes `"1234"` and no SMS is sent
- Blocks: User registration, login — the entire authentication system
- Expected: Integration with an SMS provider (Eskiz.uz for Tajikistan, Twilio as fallback)

**No Payment Webhook Validation:**
- Problem: `/api/v1/webhooks/**` is publicly accessible (no auth required per `SecurityConfig`). No code in the codebase validates webhook signatures — any request to these endpoints would be processed
- Files: `backend/src/main/java/tj/radolfa/infrastructure/web/SecurityConfig.java` (line 104)
- Blocks: Secure payment confirmation from the payment gateway
- Expected: HMAC signature verification on incoming webhook payloads

---

## Dependencies at Risk

**Spring Batch Configured but Unused:**
- Risk: `spring-batch` is in the dependency tree (Spring Boot auto-configures it), and `spring.batch.jdbc.initialize-schema: never` and `spring.batch.job.enabled: false` are set to suppress it — this suggests Batch was added for an ERP sync pipeline that was deleted
- Impact: Adds unnecessary classpath weight; potential confusion about purpose
- Migration plan: Remove `spring-batch` from `pom.xml` if no batch jobs are planned

---

*Concerns audit: 2026-03-21*
