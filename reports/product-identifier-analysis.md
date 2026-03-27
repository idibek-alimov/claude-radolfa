# Product Quick-Identifier Feature — Analysis Report

**Date:** 2026-03-18
**Author:** Claude Code
**Branch:** improve/loyalty_card
**Status:** Analysis only — no code changed

---

## 1. The Ask

Every product card should display a short, human-memorable numeric code.
Users type that code into the search box and are taken directly to that product — no scrolling, no guessing by name.
The pattern is well established in retail ERPs and brick-and-mortar catalogues (Ikea article numbers, Zara reference codes, etc.).

---

## 2. Current Identifier Landscape

| Identifier | Where | Format | User-visible? | Stable? |
|---|---|---|---|---|
| `ProductBase.erpTemplateCode` | Domain / JPA | ERP string (e.g. `SHIRT-WOOL-001`) | No — internal | Yes (ERP-managed) |
| `Sku.erpItemCode` | Domain / JPA | ERP string (e.g. `SHIRT-WOOL-001-M`) | Shown in SkuDto | Yes (ERP-managed) |
| `ListingVariant.slug` | Domain / JPA | URL slug string | Yes — in URL | Yes (generated once) |
| `ListingVariant.id` | JPA / DTO | Auto-increment Long | Returned in API | Stable but sparse |
| `ProductBase.id` | JPA | Auto-increment Long | Not in DTO | Stable but sparse |

### Key observation

There is **no short, human-friendly numeric code** today.
`erpItemCode` comes close (it is stable and unique per SKU), but it is a long ERP string, not a short number, and it identifies a size-level variant rather than the product card itself.

The `ListingVariant.id` is a sequential auto-increment Long that **does** uniquely identify a product card (one colour of one base product), but:
- It is currently exposed only at the API level (`ListingVariantDto.id`), not displayed on the card.
- It is not indexed for direct lookup (no `findById`-as-search endpoint exists).
- It looks like `47`, `153`, `2011` — short but arbitrary, not padded, and the gaps from deletions/re-seeding make it feel random.

---

## 3. What Needs to Exist for This Feature

### 3.1 The identifier itself

Two design options:

#### Option A — Use existing `ListingVariant.id` (no schema change)
- Already unique, already in the DTO, already in the DB as a PK.
- Display as zero-padded 6 digits: `id=47` → **#000047**.
- Pros: zero migration cost, available immediately.
- Cons: gaps from deletions look unprofessional; IDs are re-sequenced on fresh deploys (dev vs prod diverge).

#### Option B — Add a dedicated `productCode` field (schema change required)
- A new column `product_code CHAR(6) UNIQUE NOT NULL` on `listing_variant`.
- Generated once on variant creation, never changed (same stability guarantee as `slug`).
- Could be purely numeric (`100001`, `100002`, …) or alphanumeric (`RD-4721`).
- Pros: clean, user-facing, no gaps, can be branded (`RD-XXXXX`).
- Cons: requires a DB migration, a code-generation strategy, and back-fill for existing rows.

**Recommendation: Option B** — a dedicated `productCode` field, prefixed `RD-` + 5 digits (e.g., `RD-10047`). This avoids coupling a user-visible identifier to a DB auto-increment PK.

---

### 3.2 Where in the code each change must land

> The following is a *map of required changes*, not an implementation. No code has been touched.

#### Backend

| Layer | File | Change Needed |
|---|---|---|
| **Domain** | `ListingVariant.java` | Add `productCode: String` (final, set on creation, never mutated) |
| **JPA Entity** | `ListingVariantEntity.java` | Add `product_code CHAR(8) UNIQUE NOT NULL` column |
| **DB Migration** | `resources/db/migration/V*.sql` | `ALTER TABLE listing_variant ADD COLUMN product_code CHAR(8) UNIQUE NOT NULL DEFAULT ''`; backfill strategy needed |
| **Mapper** | `ProductHierarchyMapper.java` | Map `productCode` ↔ between domain and entity |
| **Read Model** | `ListingVariantDto.java` | Add `productCode: String` field |
| **Read Model** | `ListingVariantDetailDto.java` | Same |
| **Read Adapter** | `ListingReadAdapter.java` (or equivalent query assembler) | Include `productCode` in grid/detail queries |
| **Repository** | `ListingVariantRepository.java` | Add `findByProductCode(String productCode): Optional<ListingVariantEntity>` |
| **Search** | `ListingController.java` | Extend `/api/v1/listings/search?q=RD-10047` to detect code-like input and route to `findByProductCode` for instant exact-match redirect |
| **ERP Sync** | `ErpSyncService` (or equivalent) | Generate `productCode` on first-time variant creation (format: `RD-` + left-padded variant DB id, or a sequence) |

#### Frontend

| Layer | File | Change Needed |
|---|---|---|
| **Types** | `entities/product/model/types.ts` | Add `productCode: string` to `ListingVariant` and `ListingVariantDetail` |
| **Product Card** | `entities/product/ui/ProductCard.tsx` | Display the code badge (small mono chip, e.g., `# RD-10047`) |
| **Search Feature** | `features/search/` | Detect pure-code input (regex `/^RD-\d{5}$/`) and redirect directly to product page instead of showing search results |
| **Search Bar Widget** | `widgets/` (Navbar/SearchBar) | Handle the redirect-on-exact-code UX |

---

## 4. Search Flow — Before vs After

### Current flow (text search)
```
User types "wool shirt" → GET /api/v1/listings/search?q=wool+shirt
→ Elasticsearch full-text (or SQL LIKE fallback)
→ Returns ranked list of variants
→ User scrolls and picks
```

### Proposed flow (code search)
```
User types "RD-10047" → frontend detects code pattern
→ GET /api/v1/listings/search?q=RD-10047
  (backend detects code pattern → findByProductCode → exact match)
→ Returns single-item result OR frontend redirects directly to /products/{slug}
```

The backend `searchGrid` query currently searches `name`, `colorKey`, `webDescription` via SQL LIKE.
`productCode` is not in any of those columns, so a new branch is needed in the search handler.

---

## 5. Existing Search Infrastructure Assessment

### Elasticsearch (`SearchController.java`)
- A `/api/v1/search/reindex` endpoint exists (SYSTEM role).
- The ES index is built from PostgreSQL data.
- **The `productCode` field would need to be added to the ES index document** for it to be searchable there.
- For exact-code lookup, Elasticsearch is not strictly necessary — a simple `findByProductCode` DB query is fast enough (indexed UNIQUE column).

### SQL Fallback (`ListingVariantRepository.searchGrid`)
- Currently: `WHERE LOWER(lv.slug) LIKE :q OR LOWER(pb.name) LIKE :q OR ...`
- Would need: `OR lv.product_code = :exactQ` branch (or handled at service level before the LIKE query runs).

---

## 6. Data Model Relationships (Relevant Paths)

```
ProductBase (1)
  └─── ListingVariant (N)   ← productCode lives HERE (card-level)
         └─── Sku (N)       ← erpItemCode lives here (size-level)
```

The `productCode` belongs on `ListingVariant` because:
- One `ListingVariant` = one colour of one base product = one "card" on the grid.
- Users share/memorize a card, not a specific size.
- All siblings (same base product, different colours) will have **different** product codes — allowing precise colour identification.

---

## 7. Risks & Constraints

| Risk | Impact | Mitigation |
|---|---|---|
| ERP does NOT assign product codes | Medium — need to generate in Radolfa | Generate from `ListingVariant.id` on creation; suffix with colour initials if desired |
| Back-fill of existing variants | High — NULL constraint fails until all rows are updated | Use `DEFAULT ''` in migration, run back-fill script, then add NOT NULL constraint in a second migration step |
| Code collision on re-seeding dev DB | Low | Use sequence starting at 10000 to avoid small integers; scope to PROD DB |
| User types code in wrong format | Low | Frontend regex guards the "direct redirect" path; falls through to normal search otherwise |
| ProductCode should survive colour rename | Safe | `productCode` is final (set once, never mutated), same guarantee as `slug` |

---

## 8. Out of Scope

- QR codes or barcodes (separate feature).
- Code on physical labels / printed catalogues (offline concern).
- SKU-level codes (erpItemCode already serves that purpose at size level).

---

## 9. Recommended Implementation Order

1. **DB migration** — Add `product_code` column with backfill.
2. **Domain** — Add `productCode` to `ListingVariant`.
3. **JPA + Mapper** — Wire column through to domain.
4. **DTOs** — Expose in `ListingVariantDto` and `ListingVariantDetailDto`.
5. **Repository** — Add `findByProductCode`.
6. **Controller/Service** — Detect code pattern in search, short-circuit to exact-match.
7. **Frontend types** — Add `productCode` to TS interfaces.
8. **ProductCard UI** — Display code badge.
9. **Search bar** — Add code-pattern detection and redirect.
10. **ES reindex** — Optionally include `productCode` in the search index document.

---

## 10. Summary

The codebase is **well-structured for this addition** — the hexagonal architecture means each layer has a clear, narrow responsibility, and the identifier only needs to be threaded through the existing read-model pipeline. The biggest work item is the DB migration with safe backfill. The frontend change is cosmetic (badge on card) plus one UX routing branch in the search bar. No ERP changes are required.

**Estimated scope:** Small-to-medium. No architectural changes needed — purely additive.
