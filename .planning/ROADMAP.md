# Roadmap: Radolfa Product Management Improvement

**Created:** 2026-03-21
**Milestone:** Product Management UX v1

---

## Phase 1 — Product Create & Edit Overhaul

**Goal:** Replace the product creation popup with a full-page form and fix the edit page layout and description field.

**Plans:** 3 plans

Plans:
- [ ] 01-00-PLAN.md — Wave 0: test stubs for CreateProductService and ProductManagementController (BE-01 Nyquist)
- [ ] 01-01-PLAN.md — Backend webDescription + response fix, TipTap RichTextEditor, edit page width + rich text
- [ ] 01-02-PLAN.md — Create product page with two-column form, image upload, SKU table, navigation wiring

**Delivers:**
- Backend: `webDescription` field added to create product endpoint
- Frontend: New `/manage/products/new` page with all fields (name, category, color, rich text description, image upload, SKUs)
- Frontend: Transparent create->upload->redirect flow
- Frontend: Edit page widened to full admin layout width
- Frontend: Rich text editor replaces plain textarea on edit page

**Requirements covered:** BE-01, CREATE-01, CREATE-02, CREATE-03, CREATE-04, EDIT-01, EDIT-02

**Canonical refs:**
- `.planning/codebase/ARCHITECTURE.md` — hexagonal layer rules
- `.planning/codebase/STACK.md` — tech stack and dependencies
- `frontend/CLAUDE.md` — FSD rules, image upload API, role model
- `backend/CLAUDE.md` — hexagonal guardrails

**Out of scope for this phase:**
- Multi-variant creation
- Bulk import
- Draft/publish workflow

---

## Milestone Complete Criteria

- [ ] Admins can create a product with description and images from a single full page
- [ ] Edit page is visually consistent with the rest of the admin layout
- [ ] Description is editable via rich text on both create and edit pages
- [ ] All 7 v1 requirements verified

---
*Roadmap created: 2026-03-21*
