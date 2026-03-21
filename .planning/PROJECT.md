# Radolfa — Product Management Improvement

## What This Is

Radolfa is a standalone e-commerce platform where Radolfa itself is the authoritative source of all catalog data. This project improves the admin product management experience: replacing the limited product creation popup with a full dedicated page, and fixing the product edit page layout to be consistent with the rest of the admin interface.

## Core Value

Admins can create and edit products with all catalog data (description, images, SKUs) in one cohesive place — no multi-step workarounds.

## Requirements

### Validated

- ✓ Product creation via popup (name, category, color, SKUs) — existing
- ✓ Product edit page with cards (GeneralInfo, SKUs, Enrichment, Images) — existing
- ✓ Image upload to S3 via backend — existing
- ✓ Role-based access: MANAGER+ can create, ADMIN-only for price/stock — existing

### Active

- [ ] Backend: `POST /admin/products` accepts optional `webDescription` field
- [ ] Frontend: Product creation becomes a full dedicated page at `/manage/products/new`
- [ ] Frontend: Create page includes name, category, color, description (rich text), image upload, SKUs
- [ ] Frontend: On create submit — create product (with description) then upload images transparently, redirect to edit
- [ ] Frontend: Product edit page is full-width (matching `max-w-[1600px]` of manage dashboard)
- [ ] Frontend: Description field on edit page upgraded to rich text editor

### Out of Scope

- Multi-variant product creation (multiple colors per product) — separate concern
- Bulk product import — separate feature
- Draft/publish workflow — not requested

## Context

- Admin layout uses `max-w-[1600px]` container; edit page currently constrains to `max-w-3xl`
- Backend `CreateProductRequestDto` is a Java record — adding `webDescription` as optional field is a small change
- Image upload is via `POST /api/v1/listings/{slug}/images` (multipart); Java handles resize and S3 upload
- Rich text description currently exists as a plain textarea in `EnrichmentCard.tsx` — needs a proper editor
- The `updateListing` API already accepts `webDescription` — only the create endpoint needs updating

## Constraints

- **Architecture:** Hexagonal — any backend change flows through Domain → Application → Infrastructure layers
- **Frontend:** FSD — new create page lives in `features/product-creation/` or as a new page composition
- **Images:** No frontend processing. Upload files → backend resizes → S3. Frontend reads S3 URLs only.
- **Role model:** MANAGER and ADMIN can create products. Price/stock fields remain ADMIN-only.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Full page for create (not drawer) | Room for images + rich text; consistent with industry standard | — Pending |
| Description in create request (backend change) | Avoid frontend multi-step workaround post-creation | — Pending |
| Images uploaded transparently after create | Multipart upload requires slug (returned after creation); transparent to user | — Pending |
| Rich text editor for description | User explicitly requested; plain textarea insufficient for product descriptions | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition:**
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone:**
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-03-21 after initialization*
