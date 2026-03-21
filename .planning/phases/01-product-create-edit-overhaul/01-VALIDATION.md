---
phase: 1
slug: product-create-edit-overhaul
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-21
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito (Spring Boot 3.4.4) |
| **Config file** | none — Spring Boot auto-configures |
| **Quick run command** | `./mvnw test -pl backend -Dtest=CreateProductServiceTest -q` |
| **Full suite command** | `./mvnw test -pl backend` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./mvnw test -pl backend -Dtest=CreateProductServiceTest -q`
- **After every plan wave:** Run `./mvnw test -pl backend`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| BE-01-service | backend | 0 | BE-01 | unit | `./mvnw test -pl backend -Dtest=CreateProductServiceTest -q` | ❌ W0 | ⬜ pending |
| BE-01-controller | backend | 0 | BE-01 | integration | `./mvnw test -pl backend -Dtest=ProductManagementControllerTest -q` | ❌ W0 | ⬜ pending |
| CREATE-01 | frontend | 1 | CREATE-01 | manual smoke | n/a | manual only | ⬜ pending |
| CREATE-02 | frontend | 1 | CREATE-02 | manual smoke | n/a | manual only | ⬜ pending |
| CREATE-03 | frontend | 1 | CREATE-03 | manual smoke | n/a | manual only | ⬜ pending |
| CREATE-04 | frontend | 1 | CREATE-04 | manual smoke | n/a | manual only | ⬜ pending |
| EDIT-01 | frontend | 1 | EDIT-01 | manual visual | n/a | manual only | ⬜ pending |
| EDIT-02 | frontend | 1 | EDIT-02 | manual smoke | n/a | manual only | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `backend/src/test/java/tj/radolfa/application/services/CreateProductServiceTest.java` — stubs for BE-01 (webDescription propagation to ListingVariant)
- [ ] `backend/src/test/java/tj/radolfa/infrastructure/web/ProductManagementControllerTest.java` — stubs for BE-01 (response contains slug, variantId, productBaseId)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| "New Product" navigates to /manage/products/new | CREATE-01 | No Vitest/Jest in project | Click "New Product" button; verify URL is /manage/products/new, not a dialog |
| Create page has all fields: name, category, color, description (TipTap), image upload, SKU table | CREATE-02 | No Vitest/Jest in project | Open create page; confirm all 6 field types visible |
| Submit creates product, uploads images, redirects to edit page | CREATE-03 | No Vitest/Jest in project | Fill form with 1 image, submit; confirm URL changes to /manage/products/{slug}/edit |
| Submit without images still creates product | CREATE-04 | No Vitest/Jest in project | Fill form with no images, submit; confirm redirect to edit page |
| Edit page container is max-w-[1600px] | EDIT-01 | CSS class change; no visual regression tests | Open any product edit page; confirm it stretches to dashboard width |
| Description field on edit page is TipTap (not textarea) | EDIT-02 | No Vitest/Jest in project | Open product edit page; confirm bold/italic toolbar visible in description field |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
