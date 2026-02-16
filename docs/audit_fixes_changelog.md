# Audit Fixes Changelog

Fixes applied based on `docs/codebase_audit_report.md`.

---

| # | Issue | Severity | Fix |
|---|-------|----------|-----|
| 1 | **5.1** Blocked users bypass JWT filter | HIGH | Added `LoadUserPort.loadById()` check in `JwtAuthenticationFilter` — rejects tokens for disabled users |
| 2 | **F4.1** Missing 401 axios interceptor | HIGH | Added response interceptor to shared axios instance — redirects to `/login` on 401 |
| 3 | **2.4** SyncOrdersService missing SKU resolution | HIGH | Injected `LoadSkuPort` into `SyncOrdersService` — resolves `erpItemCode` → `Sku.id` before persisting |
| 4 | **2.7** UpdateListingService bypasses hex architecture | HIGH | Refactored to use domain objects via ports. Added `topSelling`/`featured`/`removeImage` to domain `ListingVariant`. Created `SaveListingVariantPort` |
| 5 | **6.3** SyncOrdersService silently skips orders | HIGH | Changed `execute()` to return `SyncResult` (SYNCED/SKIPPED). Controller returns 422 with skip reason |
| 6 | **F1.1** Duplicate User type definitions | HIGH | Removed duplicates from `features/auth` and `features/profile` — both now re-export from `@/entities/user` |
| 7 | **F5.1** No error boundary | MEDIUM | Created `app/error.tsx` with recovery UI |
| 8 | **2.5** OtpAuthService logs PII in plain text | MEDIUM | Added `mask()` helper — all phone log statements now show `+992***4567` format |
| 9 | **2.2 / 2.3** Sync services missing concurrency protection | MEDIUM | Set `Isolation.SERIALIZABLE` on `SyncProductHierarchyService` and `SyncCategoriesService` |
| 10 | **F9.1** No phone format validation in LoginForm | MEDIUM | Added regex check for Tajik phone format (`+992XXXXXXXXX`) with user-friendly error message |
| 11 | **4.3** UserController invalid role returns 500 | MEDIUM | Catch `IllegalArgumentException` on `UserRole.valueOf()` — returns 400 instead of 500 |
| 12 | **F1.2 / F1.3** Missing page metadata + admin noindex | MEDIUM | Added `metadata` to all pages. Admin layout has `robots: noindex`. Dynamic product page uses `generateMetadata` |
| 13 | **6.2** Sync endpoints missing caller identity | MEDIUM | Added `@AuthenticationPrincipal` to all 4 sync endpoints — caller phone logged in every sync event |
| 14 | **5.2** Single long-lived JWT allows stale roles | HIGH | Replaced 24h token with 15min access + 7-day refresh token pair. Refresh loads role fresh from DB. Tokens differentiated by `type` claim. Frontend interceptor silently refreshes before redirecting to login |
| 15 | **6.4** Missing idempotency keys on sync endpoints | MEDIUM | Added `Idempotency-Key` header requirement on `/sync/orders` and `/sync/loyalty`. New `erp_sync_idempotency` table with unique constraint. Returns 409 on duplicate, 400 if header missing. Product/category syncs unchanged (naturally idempotent) |
| 16 | **2.1** Deprecated `CreateOrderService` still wired | MEDIUM | Deleted `CreateOrderService` and `CreateOrderUseCase` — dead code with no references |
| 17 | **9.1** Slug generation duplicated in 2 places | LOW | Extracted `SlugUtils.slugify()` in `tj.radolfa.domain.util` — replaced private methods in `SyncCategoriesService` and `ProductHierarchyAdapter` |
| 18 | **7.1 / 7.2** No profile-specific configs, ES URI hardcoded | LOW | Split into `application.yml` (shared), `application-dev.yml` (verbose SQL, localhost), `application-prod.yml` (tightened logging). All infra URIs now env-var driven with dev defaults |
| 19 | **8.1** No search reindex endpoint | LOW | Already exists at `POST /api/v1/search/reindex` (`SYSTEM` role only) — no change needed |
| 20 | **3.3** OrderItemEntity missing SKU foreign key | HIGH | Replaced raw `Long skuId` with `@ManyToOne SkuEntity sku` and FK constraint `fk_order_item_sku`. Updated `OrderMapper` mappings and `OrderRepositoryAdapter` to set SKU reference via `em.getReference()` |
| 21 | **1.2** OrderItem missing quantity validation | MEDIUM | Added `if (quantity <= 0)` guard in `OrderItem` constructor — throws `IllegalArgumentException` for zero or negative values |
| 22 | **1.3** Order record items list mutability | MEDIUM | Added compact constructor wrapping `items` with `Collections.unmodifiableList()` — null defaults to `List.of()` |
| 23 | **3.1** ProductBaseEntity lossy category mapping | MEDIUM | Added denormalized `category_name` column to `ProductBaseEntity`. Adapter keeps it in sync when setting category FK. Mapper reads from denormalized column — survives category deletion |
| 24 | **3.2** ListingVariantEntity N+1 query risk | MEDIUM | Added explicit `fetch = FetchType.LAZY` on all `@OneToMany` relationships in `ListingVariantEntity` and `OrderEntity` — prevents accidental eager loading via default `findById()`/`findAll()` |
| 25 | **3.4** ProductHierarchyAdapter auto-creates placeholder categories | MEDIUM | Replaced `orElseGet` placeholder creation with `orElseThrow(IllegalStateException)` — enforces "sync categories first" contract. Removed unused `SlugUtils` import |
| 26 | **4.2 / 4.1** ListingController missing image URL and request validation | MEDIUM | Added `@NotBlank`, `@Size(max=2048)`, `@Pattern(https)` on `ImageUrlRequest.url`. Added `@Valid` on all three request body parameters (update, addImage, removeImage) |
| 27 | **4.5** CSRF vs cookie-based auth tension | MEDIUM | Reviewed — no change needed. Cookies already use `SameSite=Strict` in prod, `HttpOnly=true`, and API is REST-only. CSRF protection would add complexity with no practical benefit |
| 28 | **F2.1** Race condition in useAuth logout | MEDIUM | Replaced `window.location.href` with `router.push("/")` for proper React cleanup. Added `queryClient.clear()` to wipe stale user-specific cached data on logout |
| 29 | **F3.1** Unsafe `err: any` error casting (5 locations) | MEDIUM | Created `getErrorMessage(err: unknown)` utility in `shared/lib/utils.ts` — safely extracts message from `AxiosError`, `Error`, or unknown. Replaced all 5 `err: any` usages across `manage/page.tsx`, `profile/page.tsx`, and `UserManagementTable.tsx` |
| 30 | **F5.2** Missing 404 handling for product detail | MEDIUM | Replaced inline error message with `notFound()` from `next/navigation`. Created `app/not-found.tsx` with "Back to home" link as the global 404 boundary |
| 31 | **F6.2** Color-only status indicators | MEDIUM | Reviewed — no change needed. All badges already include text labels (`MANAGER`, `Top Seller`, `Featured`) and icons alongside colors — not color-only |
| 32 | **F8.1** Division by zero in image carousel | MEDIUM | Added `if (imageCount <= 0) return` guard in `goToImage` callback — prevents modulo-by-zero when listing has no images |
