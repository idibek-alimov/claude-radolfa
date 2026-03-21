# Requirements: Radolfa Product Management Improvement

**Defined:** 2026-03-21
**Core Value:** Admins can create and edit products with all catalog data in one cohesive place

## v1 Requirements

### Backend

- [ ] **BE-01**: `POST /api/v1/admin/products` accepts optional `webDescription` field and persists it on the listing variant

### Product Creation

- [ ] **CREATE-01**: Clicking "New Product" navigates to `/manage/products/new` (full page, not popup)
- [ ] **CREATE-02**: Create page has name, category, color, description (rich text), image upload zone, and SKU table
- [ ] **CREATE-03**: On submit — creates product with description in one backend call, then uploads images if provided, then redirects to edit page
- [ ] **CREATE-04**: Image upload on create is optional — product can be created without images

### Product Edit

- [ ] **EDIT-01**: Edit page container matches admin dashboard width (`max-w-[1600px]`)
- [ ] **EDIT-02**: Description field on edit page is a rich text editor (not plain textarea)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Multi-color variant creation in one form | Separate product management concern |
| Bulk product import | Not requested |
| Draft/publish workflow | Not requested |
| Mobile-specific edit UX | Admin is desktop-first |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| BE-01 | Phase 1 | Pending |
| CREATE-01 | Phase 1 | Pending |
| CREATE-02 | Phase 1 | Pending |
| CREATE-03 | Phase 1 | Pending |
| CREATE-04 | Phase 1 | Pending |
| EDIT-01 | Phase 1 | Pending |
| EDIT-02 | Phase 1 | Pending |

**Coverage:**
- v1 requirements: 7 total
- Mapped to phases: 7
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-21*
*Last updated: 2026-03-21 after initial definition*
