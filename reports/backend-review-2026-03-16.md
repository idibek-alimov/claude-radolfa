# Backend Code Review — 2026-03-16

## CRITICAL

### 1. Hexagonal Boundary Violation — Application Ports Import Infrastructure DTOs
- **Files:**
  - `application/ports/in/GetListingUseCase.java` (imports `infrastructure.web.dto.ListingVariantDto`)
  - `application/ports/in/GetHomeCollectionsUseCase.java` (imports `infrastructure.web.dto.CollectionPageDto`, `HomeSectionDto`)
  - `application/ports/out/LoadListingPort.java`
  - `application/ports/out/SearchListingPort.java`
  - `application/ports/out/LoadHomeCollectionsPort.java`
  - `application/services/GetListingService.java`
  - `application/services/GetHomeCollectionsService.java`
- **Impact:** Couples business logic to HTTP transport; makes use cases untestable without infra classpath.
- **Fix:** Move these DTOs into `application/` as read-model records. Controllers map the result to HTTP DTOs via MapStruct.
- [ ] Fixed

### 2. Hexagonal Boundary Violation — OtpAuthService Imports Infrastructure Security
- **File:** `application/services/OtpAuthService.java:15-16`
- **Impact:** Security infrastructure (`JwtUtil`, `OtpStore`) leaked into application layer.
- **Fix:** Define `TokenIssuerPort` and `OtpPort` out-port interfaces. `JwtUtil` and `OtpStore` become implementing adapters.
- [ ] Fixed

### 3. N+1 Query — Order History Load
- **Files:** `persistence/repository/OrderRepository.java:10`, `persistence/adapter/OrderRepositoryAdapter.java:33`
- **Impact:** `findByUser_Id` lazy-loads `items` + `items.sku` per order. ~31 queries for 10 orders of 3 items.
- **Fix:** Add `@EntityGraph(attributePaths = {"items", "items.sku"})` to the repository method.
- [ ] Fixed

### 4. N+1 Query — Discount Enrichment Lazy Proxy
- **File:** `persistence/adapter/DiscountEnrichmentAdapter.java:58`
- **Impact:** `SkuEntity.listingVariant` is LAZY; accessing `.getListingVariant().getId()` in a stream triggers 1 SELECT per SKU.
- **Fix:** Use a JPQL projection that returns `listingVariant.id` directly, or add `@BatchSize(size = 50)` on the association.
- [ ] Fixed

### 5. Recursive N+1 — Category Descendants
- **File:** `persistence/adapter/CategoryAdapter.java:63-77`
- **Impact:** `collectDescendants` fires 1 query per category node. Unbounded queries on every category-filtered page load.
- **Fix:** Replace with a single recursive CTE query:
  ```sql
  WITH RECURSIVE descendants AS (
    SELECT id FROM categories WHERE id = :rootId
    UNION ALL
    SELECT c.id FROM categories c JOIN descendants d ON c.parent_id = d.id
  )
  SELECT id FROM descendants
  ```
- [ ] Fixed

### 6. Missing @Transactional on Delete
- **Files:** `persistence/adapter/DiscountAdapter.java:67`, `persistence/repository/DiscountRepository.java:16`
- **Impact:** `deleteByErpPricingRuleId` derived query may silently not commit without an active transaction.
- **Fix:** Add `@Transactional` to the repository method.
- [ ] Fixed

### 7. HTTP 200 Returned on Total Sync Failure
- **File:** `web/ErpSyncController.java:125-138`
- **Impact:** Catches exception, returns `ok()` with error body. Breaks ERPNext retry logic and alerting.
- **Fix:** Return 500 when `synced == 0 && errors > 0`, 207 for partial success.
- [ ] Fixed

### 8. Unpaginated `findAll()` on Reindex
- **File:** `web/SearchController.java:51`
- **Impact:** Loads every `ListingVariantEntity` into heap. OOM risk on large catalogues.
- **Fix:** Iterate with `findAll(Pageable)` in a loop with a fixed page size (e.g. 200).
- [ ] Fixed

### 9. Non-Constant-Time API Key Comparison
- **File:** `security/ApiKeyAuthenticationFilter.java:55`
- **Impact:** `String.equals()` short-circuits on first mismatch — timing side-channel on SYSTEM API key.
- **Fix:** Use `MessageDigest.isEqual(key.getBytes(UTF_8), systemKey.getBytes(UTF_8))`.
- [ ] Fixed

### 10. No Size Constraint on UpdateListingRequest
- **File:** `web/ListingController.java:133`
- **Impact:** `webDescription` has no `@Size` — unbounded string written directly to DB.
- **Fix:** Add `@Size(max = 5000)` to the field.
- [ ] Fixed

---

## IMPORTANT

### 11. Null skuId Silently Persisted in OrderItem
- **File:** `application/services/SyncOrdersService.java:57-72`
- **Impact:** When SKU not found for an ERP item code, `OrderItem` is saved with `skuId = null` — broken foreign key.
- **Fix:** Either fail the order sync or skip the unresolved line item. Do not persist with null `skuId`.
- [ ] Fixed

### 12. Misleading SyncResult Due to Transaction Rollback
- **File:** `application/services/SyncUsersService.java:52-59`
- **Impact:** `@Transactional` on outer method + per-item catch = rolled-back rows counted as success.
- **Fix:** Use `@Transactional(propagation = REQUIRES_NEW)` on `upsert()`, or remove the catch and fail atomically.
- [ ] Fixed

### 13. NPE on Null Phone in OtpAuthService
- **File:** `application/services/OtpAuthService.java:62`
- **Impact:** `PhoneNumber.of(null)` returns null, next line throws NPE with no diagnostic info.
- **Fix:** Use `new PhoneNumber(phone)` (which has `requireNonNull`) or add an explicit null guard.
- [ ] Fixed

### 14. NPE in SlugUtils on Null Input
- **File:** `domain/util/SlugUtils.java:16`
- **Impact:** `name.toLowerCase()` throws NPE if ERP sends null name.
- **Fix:** Add `Objects.requireNonNull(name, "slug input must not be null")`.
- [ ] Fixed

### 15. saveAll Bypasses Update Path — Detached Entity Risk
- **File:** `persistence/adapter/LoyaltyTierRepositoryAdapter.java:63-70`
- **Impact:** `saveAll` creates detached entities with stale `version`, risking `OptimisticLockException`.
- **Fix:** Implement as `tiers.stream().map(this::save).toList()` to reuse the find-then-update logic.
- [ ] Fixed

### 16. Missing Unique Constraint on Idempotency Table
- **File:** `persistence/entity/IdempotencyRecordEntity.java:25`
- **Impact:** No DB-level unique constraint on `(idempotency_key, event_type)`. Race condition: two concurrent requests both pass `exists()` check.
- **Fix:** Add `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"idempotency_key", "event_type"}))`.
- [ ] Fixed

### 17. Eager Fetch on UserEntity.tier
- **File:** `persistence/entity/UserEntity.java:45`
- **Impact:** `FetchType.EAGER` on `tier` causes unnecessary JOIN on every user load (auth, admin, search).
- **Fix:** Change to `FetchType.LAZY`.
- [ ] Fixed

### 18. CascadeType.ALL Without orphanRemoval on Category Children
- **File:** `persistence/entity/CategoryEntity.java:34`
- **Impact:** Removing a child from the collection does not delete the row. Trap for future `deleteCategory` operations.
- **Fix:** Add `orphanRemoval = true`, or restrict to `cascade = {PERSIST, MERGE}`.
- [ ] Fixed

### 19. Duplicated Grid Row Mapping Logic
- **Files:** `persistence/adapter/ListingReadAdapter.java:102-130,263-288`, `persistence/adapter/HomeCollectionsAdapter.java:107-163`
- **Impact:** `toGridDto`, `loadImageMap`, `toBigDecimal`, `toInteger` are copy-pasted verbatim. Column index changes must be applied in two places.
- **Fix:** Extract a package-private `ListingGridRowMapper` utility class.
- [ ] Fixed

### 20. No Range Validation on discountValue
- **File:** `web/dto/SyncDiscountPayload.java:13`
- **Impact:** Negative or >100% discounts accepted — produces inflated or negative prices shown to customers.
- **Fix:** Add `@DecimalMin("0.00") @DecimalMax("100.00")`.
- [ ] Fixed

### 21. X-Forwarded-For Spoofing Bypasses Rate Limit
- **File:** `web/AuthController.java:259`
- **Impact:** Clients can forge `X-Forwarded-For` to bypass per-IP rate limiting.
- **Fix:** Trust only `X-Real-IP` set by Nginx, or use `request.getRemoteAddr()`.
- [ ] Fixed

### 22. Ambiguous Auth on GET /loyalty-tiers
- **File:** `web/LoyaltyController.java:35`
- **Impact:** Not in `SecurityConfig` permit list, no `@PreAuthorize` — falls under `.anyRequest().authenticated()` by default. Intent unclear.
- **Fix:** Explicitly add to permit list (if public) or add `@PreAuthorize` (if authenticated).
- [ ] Fixed

### 23. Spring Batch Reader Not Restartable
- **File:** `erp/batch/ErpProductReader.java`
- **Impact:** `currentPage` resets to 1 on restart — entire import replays from the beginning.
- **Fix:** Override `open(ExecutionContext)` and `update(ExecutionContext)` to checkpoint the page counter.
- [ ] Fixed

### 24. No App-Layer Defense for Disabled ERP Products
- **File:** `erp/ErpProductClientHttp.java:99`
- **Impact:** `disabled` field not deserialized into `ErpItemRecord`. If ERPNext filter is bypassed, disabled products sync through.
- **Fix:** Add `boolean disabled` to `ErpItemRecord` and `ErpProductSnapshot`, skip in `ErpProductProcessor`.
- [ ] Fixed
