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
