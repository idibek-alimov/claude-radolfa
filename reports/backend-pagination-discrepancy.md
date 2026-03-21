# Backend Pagination: 1-Based, Not 0-Based

**Date:** 2026-03-20
**Status:** Frontend fixed. Backend note only.

---

## Observed Behavior

Sending `GET /api/v1/listings?page=0&size=12` returns:

```
400 BAD_REQUEST — Page index must not be less than zero
```

The backend rejects `page=0`, which means it internally subtracts 1 from the incoming
`page` parameter before constructing the Spring `PageRequest`. The effective constraint
is `page >= 1`.

## Impact

The `FRONTEND_INTEGRATION_PLAN.md` (and initial frontend implementation) assumed the
backend was 0-based and subtracted 1 before sending (`page - 1`). This caused the
first-page request to always send `page=0`, breaking all paginated listing views.

## Frontend Fix Applied

Removed `- 1` from all paginated API calls:

| File | Change |
|---|---|
| `entities/product/api/index.ts` — `fetchListings` | `page - 1` → `page` |
| `entities/product/api/index.ts` — `searchListings` | `page - 1` → `page` |
| `entities/product/api/index.ts` — `fetchCategoryProducts` | `page - 1` → `page` |
| `features/user-management/api.ts` — `fetchUsers` | `page - 1` → `page` |

Frontend page state remains 1-based (starts at 1). The value is now sent as-is.

## Backend Note (Do Not Change Frontend Again)

The backend `ListingController` likely calls `PageRequest.of(page - 1, size)` internally,
making its external API 1-based. This is non-standard (Spring convention is 0-based), but
since it is the actual behavior, the frontend must send 1-based page numbers.

If the backend is ever refactored to accept 0-based pages, the frontend will need to
re-add `page - 1`.
