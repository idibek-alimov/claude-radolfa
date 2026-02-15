# Radolfa Codebase Audit Report

**Date:** 2026-02-15
**Scope:** Full backend + frontend scan for logic issues, unimplemented features, and improvements
**Auditor:** Claude Opus 4.6

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Backend Findings](#backend-findings)
   - [Domain Layer](#1-domain-layer-issues)
   - [Application Layer](#2-application-layer-issues)
   - [Persistence Layer](#3-persistence-layer-issues)
   - [Web Layer](#4-web-layer-issues)
   - [Security](#5-security-issues)
   - [ERP Sync](#6-erp-sync-issues)
   - [Configuration](#7-configuration-issues)
   - [Missing Features](#8-missing-backend-features--gaps)
   - [Code Quality](#9-minor-inconsistencies--code-quality)
3. [Frontend Findings](#frontend-findings)
   - [Architecture & FSD Violations](#f1-architecture--fsd-violations)
   - [State Management & Hooks](#f2-state-management--hooks)
   - [Type Safety](#f3-type-safety-issues)
   - [Data Fetching & Queries](#f4-data-fetching--query-issues)
   - [Error Handling](#f5-missing-features--error-handling)
   - [Accessibility](#f6-accessibility-issues)
   - [Performance](#f7-performance-issues)
   - [Logic Bugs & Edge Cases](#f8-logic-bugs--edge-cases)
   - [Missing Validations](#f9-missing-validations)
   - [Missing Features](#f10-missing-frontend-features)
4. [Summary Tables](#summary-tables)
5. [Recommended Action Plan](#recommended-action-plan)

---

## Executive Summary

This audit identified **53 findings** across backend and frontend. The most critical issues are:

- **Blocked users can still use existing JWT tokens** (security gap)
- **SyncOrdersService silently skips orders for missing users** (data loss risk)
- **UpdateListingService bypasses hexagonal architecture** (architecture violation)
- **Missing 401 axios interceptor** (frontend UX gap)
- **Duplicate User type definitions** across 3 frontend features

| Severity | Backend | Frontend | Total |
|----------|---------|----------|-------|
| HIGH     | 5       | 2        | 7     |
| MEDIUM   | 13      | 7        | 20    |
| LOW      | 12      | 13       | 25    |
| **Total**| **30**  | **22**   | **53**|

---

## Backend Findings

### 1. Domain Layer Issues

#### 1.1 Missing Validation in ProductBase
- **File:** `backend/src/main/java/tj/radolfa/domain/model/ProductBase.java:21-26`
- **Severity:** LOW
- **Issue:** Constructor accepts `null` for `name` and `category` fields without validation. While acceptable for optional enrichment, there's no documentation clarifying the contract.

#### 1.2 OrderItem Missing Quantity Validation
- **File:** `backend/src/main/java/tj/radolfa/domain/model/OrderItem.java:3-48`
- **Severity:** MEDIUM
- **Issue:** No validation for negative quantity. Constructor accepts `quantity` without bounds checking.
- **Fix:** Add validation: `if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive")`

#### 1.3 Order Record Items List Mutability
- **File:** `backend/src/main/java/tj/radolfa/domain/model/Order.java:6-14`
- **Severity:** MEDIUM
- **Issue:** The `items` list passed to the record is mutable. External code could mutate the list after creation, breaking the immutability guarantee of the record.
- **Fix:** Wrap with `Collections.unmodifiableList()` in a compact constructor.

#### 1.4 ListingVariant.addImage() Unbounded
- **File:** `backend/src/main/java/tj/radolfa/domain/model/ListingVariant.java:75-77`
- **Severity:** LOW
- **Issue:** No validation on image URL format or maximum image count. Could lead to bloated records.

---

### 2. Application Layer Issues

#### 2.1 CreateOrderService Deprecated but Still Wired
- **File:** `backend/src/main/java/tj/radolfa/application/services/CreateOrderService.java:17-22`
- **Severity:** MEDIUM
- **Issue:** The service is marked `@Deprecated` and `@Service` but is still registered as a Spring bean. While no endpoint calls it, the class could be accidentally injected and used. It also still has `CreateOrderUseCase` which pollutes the port layer.
- **Recommendation:** Remove entirely or extract to a separate "legacy" package.

#### 2.2 SyncProductHierarchyService Missing Concurrency Protection
- **File:** `backend/src/main/java/tj/radolfa/application/services/SyncProductHierarchyService.java:57-124`
- **Severity:** MEDIUM
- **Issue:** No explicit isolation level on `@Transactional`. If two concurrent sync calls arrive for the same template, race conditions could occur on variant slug generation and marking as synced.
- **Fix:** Add `@Transactional(isolation = Isolation.SERIALIZABLE)` or use optimistic locking.

#### 2.3 SyncCategoriesService TOCTOU Race Condition
- **File:** `backend/src/main/java/tj/radolfa/application/services/SyncCategoriesService.java:35-74`
- **Severity:** MEDIUM
- **Issue:** The in-memory `existingByName` map is updated as categories are created, but between lookup and creation, another thread could create the same category (Time-Of-Check-Time-Of-Use bug).
- **Fix:** Use database-level unique constraint and handle `DataIntegrityViolationException`.

#### 2.4 SyncOrdersService Missing SKU Resolution
- **File:** `backend/src/main/java/tj/radolfa/application/services/SyncOrdersService.java:50-58`
- **Severity:** HIGH
- **Issue:** OrderItems are created with `null` skuId. The `erpItemCode` is stored, but there's no resolution to the actual `Sku` entity ID during sync. This breaks the relationship and prevents queries like "which SKU was ordered?"
- **Fix:** Resolve `erpItemCode` to `Sku.id` using `LoadSkuPort` before persisting.

#### 2.5 OtpAuthService Logs Phone Numbers in Plain Text
- **File:** `backend/src/main/java/tj/radolfa/application/services/OtpAuthService.java:63,76,85,91`
- **Severity:** MEDIUM
- **Issue:** Phone numbers are logged in full, violating PII logging best practices. The `AuthController` masks them, but the service does not.
- **Fix:** Mask phone numbers in all log statements (e.g., `+992***4567`).

#### 2.6 ChangeUserRoleService Redundant Ternary
- **File:** `backend/src/main/java/tj/radolfa/application/services/ChangeUserRoleService.java:39`
- **Severity:** LOW
- **Issue:** `user.role() == newRole ? user.role() : newRole` always evaluates to `newRole`. The ternary is a no-op.

#### 2.7 UpdateListingService Bypasses Hexagonal Architecture
- **File:** `backend/src/main/java/tj/radolfa/application/services/UpdateListingService.java:1-71`
- **Severity:** HIGH (Architecture Violation)
- **Issue:** This service directly mutates JPA entities (`ListingVariantEntity`) instead of working through domain objects. It calls `repository.findBySlug()` which returns a JPA entity, not a domain `ListingVariant`. The domain's `updateWebDescription()` method is never used.
- **Fix:** Refactor to load domain `ListingVariant` via port, mutate it, and save via port.

---

### 3. Persistence Layer Issues

#### 3.1 ProductBaseEntity Missing Denormalized Category Name
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/persistence/entity/ProductBaseEntity.java:30-32`
- **Severity:** MEDIUM
- **Issue:** The domain `ProductBase` stores category as a `String` (name), but the entity stores a FK to `CategoryEntity`. If the category is deleted, the product loses category information. The mapping is lossy.

#### 3.2 ListingVariantEntity N+1 Query Risk
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/persistence/entity/ListingVariantEntity.java:26-55`
- **Severity:** MEDIUM
- **Issue:** `@ManyToOne` and `@OneToMany` relationships without explicit fetch strategies. Custom queries use `JOIN FETCH`, but default `findById()` or `findAll()` would trigger N+1 queries.
- **Fix:** Set `fetch = FetchType.LAZY` explicitly on all relationships.

#### 3.3 OrderItemEntity Missing SKU Foreign Key Constraint
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/persistence/entity/OrderItemEntity.java`
- **Severity:** HIGH (Data Integrity)
- **Issue:** The `skuId` field is nullable (for ERP sync), but there's no FK constraint to ensure data integrity when it IS present. Combined with finding 2.4 (missing SKU resolution), order items are orphaned from SKUs.

#### 3.4 ProductHierarchyAdapter Auto-Creates Placeholder Categories
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/ProductHierarchyAdapter.java:96-104`
- **Severity:** MEDIUM
- **Issue:** If a category name from ERP doesn't exist in the database, a placeholder is auto-created. The CLAUDE.md states "Categories are synced BEFORE products" but this code doesn't enforce that ordering.
- **Fix:** Throw an exception instead: `throw new IllegalStateException("Category not found. Sync categories first.")`

#### 3.5 ProductHierarchyAdapter Auto-Creates Placeholder Colors
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/ProductHierarchyAdapter.java:133-141`
- **Severity:** LOW
- **Issue:** Colors are auto-created if missing, potentially leading to duplicate entries or colors without proper hex codes.

#### 3.6 UserMapper Missing Null Safety
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/persistence/mappers/UserMapper.java:16-25`
- **Severity:** LOW
- **Issue:** `toEntity()` doesn't validate that required fields (phone, role) are present. A `User` record with `null` phone would pass through silently.

#### 3.7 ProductHierarchyMapper Fragile Color Key Mapping
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/persistence/mappers/ProductHierarchyMapper.java:55-72`
- **Severity:** LOW
- **Issue:** `colorKey` is extracted from the entity's color relationship, not stored directly. If the color entity's key changes, the domain object becomes stale.

---

### 4. Web Layer Issues

#### 4.1 Missing @Valid on UpdateListingRequest
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/web/ListingController.java:113-114`
- **Severity:** LOW
- **Issue:** The `UpdateListingRequest` record has no `@Valid` annotation and no field validations.

#### 4.2 ListingController Lacks Image URL Validation
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/web/ListingController.java:93-101`
- **Severity:** MEDIUM
- **Issue:** No validation that the image URL is valid or points to an accessible resource. No size limits on the URL string. Potential for SSRF if URLs are later fetched server-side.

#### 4.3 UserController Missing Role String Validation
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/web/UserController.java:79-88`
- **Severity:** MEDIUM
- **Issue:** The `changeUserRole` endpoint accepts a role string from request parameter. If an invalid string is provided, `UserRole.valueOf()` throws an uncaught `IllegalArgumentException` resulting in a 500 instead of 400.
- **Fix:** Catch `IllegalArgumentException` and return 400 Bad Request.

#### 4.4 AuthController Leaks OTP Generation Details
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/web/AuthController.java:102-104`
- **Severity:** LOW
- **Issue:** The login response says "Check console logs in DEV mode," revealing that OTP is logged to console in dev mode. Minor information leak.

#### 4.5 CSRF vs Cookie-Based Auth Design Tension
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/web/SecurityConfig.java:70-72`
- **Severity:** MEDIUM
- **Issue:** CSRF is disabled for "stateless JWT auth," but authentication uses cookies (`AuthCookieManager`). This creates a design tension: cookies are automatically sent by browsers (vulnerable to CSRF), but CSRF protection is disabled. The `SameSite=Strict/Lax` mitigates this, but the approach is fragile.
- **Fix:** Either switch to Authorization header-based auth OR enable CSRF protection for cookie-based flows.

#### 4.6 GlobalExceptionHandler Missing Domain Exception Handlers
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/web/GlobalExceptionHandler.java:105-110`
- **Severity:** LOW
- **Issue:** The generic exception handler catches all exceptions at ERROR level. No specific handlers for `OptimisticLockException`, `ErpLockViolationException`, or other domain-specific exceptions that should return specific HTTP codes.

---

### 5. Security Issues

#### 5.1 Blocked Users Can Still Use Existing JWT Tokens
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/security/JwtAuthenticationFilter.java:87-107`
- **Severity:** HIGH (Security Gap)
- **Issue:** When a user is blocked (`enabled = false`), `OtpAuthService` prevents new logins, but `JwtAuthenticationFilter` does NOT check the `enabled` flag. A blocked user with a valid JWT can continue making requests until token expiry (24 hours).
- **Fix:** Add a user lookup in `authenticateFromToken()` to verify `user.enabled()` is true.

#### 5.2 JWT Token Has No Refresh Mechanism
- **File:** `backend/src/main/resources/application.yml:64-65`
- **Severity:** MEDIUM
- **Issue:** JWT expiration is 24 hours with no refresh token mechanism. If a user's role changes (e.g., promoted to MANAGER), the old role is encoded in the JWT and won't update until re-authentication. This also means blocked users have a 24-hour window.
- **Fix:** Implement refresh tokens or reduce token expiry to 1 hour.

#### 5.3 RateLimiterService Uses Wall Clock Time
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/security/RateLimiterService.java:52,74`
- **Severity:** LOW
- **Issue:** Uses `System.currentTimeMillis()` which can be affected by system clock adjustments. Rate limits could be bypassed if the clock is set backward.
- **Fix:** Use `System.nanoTime()` for monotonic timing.

#### 5.4 OTP Store Uses In-Memory Storage
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/security/OtpStore.java:95-99`
- **Severity:** LOW
- **Issue:** OTPs are stored in a `ConcurrentHashMap`. In a multi-instance deployment, users could receive an OTP on one instance but verify on another, causing verification failure. Also, server restart clears all OTPs.
- **Fix:** Move to Redis or database-backed OTP storage when scaling beyond single instance.

---

### 6. ERP Sync Issues

#### 6.1 SyncProductHierarchyService Doesn't Log Sync Events
- **File:** `backend/src/main/java/tj/radolfa/application/services/SyncProductHierarchyService.java:57-124`
- **Severity:** LOW
- **Issue:** Unlike the controller which calls `logSyncEvent.log()`, the service itself doesn't log sync events. Audit trail depends on the controller layer, not the business logic.

#### 6.2 ErpSyncController Missing Caller Identity in Audit Logs
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/web/ErpSyncController.java:73-98`
- **Severity:** MEDIUM
- **Issue:** Sync endpoints log WHAT was synced but not WHO initiated it. No `@AuthenticationPrincipal` to extract and log the caller's identity for audit.

#### 6.3 SyncOrdersService Silently Skips Orders for Missing Users
- **File:** `backend/src/main/java/tj/radolfa/application/services/SyncOrdersService.java:39-44`
- **Severity:** HIGH (Data Loss Risk)
- **Issue:** If a user doesn't exist (phone not in system), the order sync is silently skipped with a warning log. There's no mechanism to track skipped orders or retry later. Orders can be permanently lost if user creation is delayed.
- **Fix:** Return sync result indicating skipped orders, or create a "pending sync" table for reconciliation.

#### 6.4 Missing Idempotency Keys on Sync Endpoints
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/web/ErpSyncController.java`
- **Severity:** MEDIUM
- **Issue:** Sync endpoints don't require an `Idempotency-Key` header. Product upserts are naturally idempotent, but loyalty point syncs could double-count points on network retries.
- **Fix:** Add idempotency key tracking for loyalty and order syncs.

---

### 7. Configuration Issues

#### 7.1 No Profile-Specific Configurations
- **File:** `backend/src/main/resources/application.yml`
- **Severity:** LOW
- **Issue:** Only one `application.yml` exists. Production should have different settings for `jpa.hibernate.ddl-auto`, logging levels, and security flags.
- **Fix:** Create `application-dev.yml` and `application-prod.yml`.

#### 7.2 Elasticsearch URI Not Env-Var Driven
- **File:** `backend/src/main/resources/application.yml:40-43`
- **Severity:** LOW
- **Issue:** Elasticsearch is hardcoded to `http://localhost:9200` with no env-var override.
- **Fix:** Use `${ELASTICSEARCH_URIS:http://localhost:9200}`.

#### 7.3 JWT Secret Lacks Length Validation
- **File:** `backend/src/main/resources/application.yml:64`
- **Severity:** LOW
- **Issue:** If someone sets a JWT secret shorter than 32 characters, `JwtUtil` will fail with a cryptic error. No startup validation.
- **Fix:** Add `@PostConstruct` validation in `JwtProperties`.

---

### 8. Missing Backend Features & Gaps

#### 8.1 No Search Reindex Endpoint
- **Severity:** LOW
- **Issue:** Products are indexed into Elasticsearch during sync, but there's no admin endpoint to trigger a full reindex (useful after ES migration or schema changes).
- **Fix:** Add `POST /api/v1/sync/search/reindex` (SYSTEM role only).

#### 8.2 No Idempotency Key Storage
- **Severity:** MEDIUM
- **Issue:** No table to track idempotency keys for preventing duplicate sync processing.
- **Fix:** Add `erp_sync_idempotency` table with unique (event_type, event_id).

---

### 9. Minor Inconsistencies & Code Quality

#### 9.1 Slug Generation Duplicated
- **Files:**
  - `backend/src/main/java/tj/radolfa/application/services/SyncCategoriesService.java:76-81`
  - `backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/ProductHierarchyAdapter.java:147-152`
- **Severity:** LOW
- **Issue:** The `slugify()` function is defined in two places.
- **Fix:** Extract to a `SlugUtils` utility class.

#### 9.2 Money.multiply() Only Accepts int
- **File:** `backend/src/main/java/tj/radolfa/domain/model/Money.java:44-46`
- **Severity:** LOW
- **Issue:** The `multiply()` method only accepts `int`. If fractional quantities are ever needed, this will break.

#### 9.3 Redundant Null Checks in Mappers
- **File:** `backend/src/main/java/tj/radolfa/infrastructure/persistence/mappers/ProductHierarchyMapper.java:31-96`
- **Severity:** LOW
- **Issue:** Custom mapper methods manually check `if (entity == null) return null;` but MapStruct generates null checks automatically. Redundant but harmless.

---

## Frontend Findings

### F1. Architecture & FSD Violations

#### F1.1 Duplicate User Type Definitions
- **Severity:** HIGH
- **Files:**
  - `frontend/src/features/auth/model/types.ts:12`
  - `frontend/src/features/profile/types.ts:20`
  - `frontend/src/entities/user/model/types.ts:39`
- **Issue:** All three define similar User interfaces with identical fields. Violates FSD single source of truth. The authoritative definition should be in `entities/user` and other features should import from there.
- **Impact:** Type misalignment risk, maintenance burden, potential field divergence.

#### F1.2 Missing Page Metadata / SEO
- **Severity:** MEDIUM
- **Issue:** Only root `layout.tsx` exports metadata. Individual pages (`/products/[slug]`, `/search`, `/manage`) lack their own `metadata` exports.
- **Impact:** Poor SEO, missing page titles in browser tabs.

#### F1.3 Admin Pages Missing `robots: noindex`
- **Severity:** LOW
- **Issue:** Admin pages (e.g., `/manage`) should prevent search engine indexing.

---

### F2. State Management & Hooks

#### F2.1 Potential Race Condition in useAuth Logout
- **Severity:** MEDIUM
- **File:** `frontend/src/features/auth/model/useAuth.ts:70`
- **Issue:** The `logout` callback does a hard redirect via `window.location.href = "/"` which may happen before React cleanup completes. No cross-tab state synchronization exists (if user logs in on another tab).
- **Fix:** Use `router.push()` and consider BroadcastChannel API for cross-tab auth sync.

---

### F3. Type Safety Issues

#### F3.1 Unsafe `err: any` Error Casting
- **Severity:** MEDIUM
- **Files:** (5 locations)
  - `frontend/src/app/(admin)/manage/page.tsx:149,161,177`
  - `frontend/src/app/(storefront)/profile/page.tsx:46`
  - `frontend/src/features/user-management/ui/UserManagementTable.tsx:72`
- **Issue:** Using `err: any` in error handlers prevents TypeScript type checking. Assumes `err.response.data.message` exists without type safety.
- **Fix:** Create a shared `ApiError` type:
  ```typescript
  export interface ApiError {
    response?: { data?: { message?: string } };
    message?: string;
  }
  ```

---

### F4. Data Fetching & Query Issues

#### F4.1 Missing 401 Axios Interceptor
- **Severity:** HIGH
- **File:** `frontend/src/shared/api/axios.ts`
- **Issue:** The axios instance has no response interceptor for 401 responses. When a JWT token expires, API calls fail silently or show raw error messages instead of redirecting to login.
- **Fix:** Add response interceptor:
  ```typescript
  apiClient.interceptors.response.use(
    response => response,
    error => {
      if (error.response?.status === 401) {
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }
  );
  ```

#### F4.2 Inconsistent Error Toast Usage
- **Severity:** LOW
- **Issue:** Some places use `toast.error()`, others use `console.error()`. User-facing errors are inconsistently handled.

---

### F5. Missing Features & Error Handling

#### F5.1 No Error Boundary Component
- **Severity:** MEDIUM
- **File:** `frontend/src/app/layout.tsx`
- **Issue:** No `<ErrorBoundary>` wrapper exists. If a component crashes, the entire app becomes blank with no recovery UI.
- **Fix:** Create `error.tsx` in the app directory or wrap children in a React ErrorBoundary.

#### F5.2 Missing 404 Handling for Product Detail
- **Severity:** MEDIUM
- **File:** `frontend/src/entities/product/ui/ProductDetail.tsx:159`
- **Issue:** Shows inline error message "Product not found" but no proper 404 page or redirect to `/products`.

---

### F6. Accessibility Issues

#### F6.1 Missing aria-labels on Icon Buttons
- **Severity:** LOW
- **File:** `frontend/src/app/(admin)/manage/page.tsx:574`
- **Issue:** Remove button inside image carousel uses an `<X>` icon without descriptive `aria-label`.

#### F6.2 Color-Only Status Indicators
- **Severity:** MEDIUM
- **Files:**
  - `frontend/src/widgets/Navbar/ui/Navbar.tsx:82-89`
  - `frontend/src/app/(admin)/manage/page.tsx:329-339`
- **Issue:** Status badges rely on background color for meaning. While text labels exist, the color-only approach is problematic for colorblind users.

---

### F7. Performance Issues

#### F7.1 Image Optimization Disabled
- **Severity:** LOW
- **Files:** `ProductCard.tsx:47`, `ProductDetail.tsx:227,292,556`, `manage/page.tsx:290`
- **Issue:** All images use `unoptimized` in Next.js Image components. Required for S3 URLs but disables all Next.js image optimization (resizing, format conversion, lazy loading optimization).

#### F7.2 MegaMenu Short Stale Time
- **Severity:** LOW
- **File:** `frontend/src/widgets/MegaMenu/ui/MegaMenu.tsx:17`
- **Issue:** `staleTime: 5 * 60 * 1000` (5 minutes) for categories that rarely change. Could increase to 30+ minutes for better performance.

---

### F8. Logic Bugs & Edge Cases

#### F8.1 Division by Zero in Image Carousel
- **Severity:** MEDIUM
- **File:** `frontend/src/entities/product/ui/ProductDetail.tsx:104`
- **Issue:** `(prev + dir + imageCount) % imageCount` will cause division by zero if `imageCount` is 0. While unlikely in practice (checked before rendering), the guard is missing.

#### F8.2 UserManagementTable Unsafe Role Fallback
- **Severity:** LOW
- **File:** `frontend/src/features/user-management/ui/UserManagementTable.tsx:156`
- **Issue:** `currentUser?.role ?? ""` uses empty string as role fallback. This masks a missing user condition and could allow unintended access.

---

### F9. Missing Validations

#### F9.1 No Phone Format Validation in LoginForm
- **Severity:** MEDIUM
- **File:** `frontend/src/features/auth/ui/LoginForm.tsx:45`
- **Issue:** Only checks `!phone.trim()` (empty check). No format validation for Tajik phone numbers (+992...). Backend validates, but frontend should give better UX feedback before making the API call.

---

### F10. Missing Frontend Features

#### F10.1 No Logout Confirmation
- **Severity:** LOW
- **File:** `frontend/src/widgets/Navbar/ui/Navbar.tsx:107`
- **Issue:** One-click logout with no confirmation dialog. Accidental clicks lose session.

#### F10.2 No Search History Persistence
- **Severity:** LOW
- **File:** `frontend/src/features/search/ui/SearchBar.tsx`
- **Issue:** SearchBar doesn't remember recent searches. Could enhance UX with `localStorage`.

---

## Summary Tables

### Backend Summary

| Severity | Count | Key Examples |
|----------|-------|-------------|
| **HIGH** | 5 | JWT blocked-user bypass, SyncOrdersService silent failures, UpdateListingService hex violation, Missing SKU resolution, OrderItem FK missing |
| **MEDIUM** | 13 | Race conditions in sync, PII logging, CSRF design tension, Missing idempotency, Role string validation |
| **LOW** | 12 | Deprecated service, Slug duplication, Money API, Configuration gaps |

### Frontend Summary

| Severity | Count | Key Examples |
|----------|-------|-------------|
| **HIGH** | 2 | Missing 401 interceptor, Duplicate User types |
| **MEDIUM** | 7 | Missing metadata/SEO, Error boundary, Phone validation, Accessibility, Image carousel edge case |
| **LOW** | 13 | Performance, Missing confirmations, Search history, Toast consistency |

---

## Recommended Action Plan

### Immediate (This Sprint)

| # | Finding | Severity | Effort |
|---|---------|----------|--------|
| 1 | Add `enabled` check in JwtAuthenticationFilter | HIGH | 30 min |
| 2 | Add 401 response interceptor in axios | HIGH | 15 min |
| 3 | Add SKU resolution in SyncOrdersService | HIGH | 1 hour |
| 4 | Refactor UpdateListingService to use domain layer | HIGH | 2 hours |
| 5 | Handle silent order skip (return result or queue) | HIGH | 1 hour |

### Short-Term (Next Sprint)

| # | Finding | Severity | Effort |
|---|---------|----------|--------|
| 6 | Consolidate User types to single source in entities/ | HIGH | 30 min |
| 7 | Add ErrorBoundary / error.tsx to frontend | MEDIUM | 30 min |
| 8 | Mask PII in OtpAuthService logs | MEDIUM | 15 min |
| 9 | Add serializable transactions to sync services | MEDIUM | 30 min |
| 10 | Add phone format validation in LoginForm | MEDIUM | 15 min |
| 11 | Add role string validation in UserController | MEDIUM | 15 min |
| 12 | Add page metadata to all routes | MEDIUM | 30 min |
| 13 | Add sync caller identity to audit logs | MEDIUM | 15 min |

### Medium-Term (Backlog)

| # | Finding | Severity | Effort |
|---|---------|----------|--------|
| 14 | Implement JWT refresh tokens | MEDIUM | 4 hours |
| 15 | Add idempotency key tracking for syncs | MEDIUM | 2 hours |
| 16 | Remove CreateOrderService and CreateOrderUseCase | MEDIUM | 30 min |
| 17 | Extract SlugUtils utility | LOW | 15 min |
| 18 | Add profile-specific application.yml files | LOW | 30 min |
| 19 | Add search reindex endpoint | LOW | 1 hour |
| 20 | Replace `err: any` with typed ApiError | MEDIUM | 30 min |
