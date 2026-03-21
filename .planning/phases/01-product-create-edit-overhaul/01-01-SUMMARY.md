---
phase: 01-product-create-edit-overhaul
plan: "01"
subsystem: backend-api, frontend-editor
tags: [backend, frontend, tiptap, rich-text, create-product, edit-page]
dependency_graph:
  requires: [01-00]
  provides: [CreateProductUseCase.Result, RichTextEditor, widened-edit-page]
  affects: [ProductManagementController, EnrichmentCard, ProductEditPage]
tech_stack:
  added: ["@tiptap/react", "@tiptap/starter-kit"]
  patterns: [MockMvc-standalone-test, in-memory-fakes]
key_files:
  created:
    - backend/src/main/java/tj/radolfa/infrastructure/web/dto/CreateProductResponseDto.java
    - frontend/src/shared/ui/RichTextEditor.tsx
  modified:
    - backend/src/main/java/tj/radolfa/application/ports/in/product/CreateProductUseCase.java
    - backend/src/main/java/tj/radolfa/application/services/CreateProductService.java
    - backend/src/main/java/tj/radolfa/infrastructure/web/dto/CreateProductRequestDto.java
    - backend/src/main/java/tj/radolfa/infrastructure/web/ProductManagementController.java
    - backend/src/test/java/tj/radolfa/application/services/CreateProductServiceTest.java
    - backend/src/test/java/tj/radolfa/infrastructure/web/ProductManagementControllerTest.java
    - frontend/src/features/product-edit/ui/ProductEditPage.tsx
    - frontend/src/features/product-edit/ui/EnrichmentCard.tsx
    - frontend/package.json
decisions:
  - "Use undo() to enforce maxLength in TipTap (not truncation) — preserves HTML structure integrity"
  - "Pass webDescription as constructor arg at position 5 (existing null slot) — no domain model change needed"
metrics:
  duration: "~10 minutes"
  completed: "2026-03-21"
  tasks_completed: 3
  tasks_total: 3
  files_modified: 9
  files_created: 2
---

# Phase 01 Plan 01: Backend API fix + TipTap RichTextEditor + Edit page improvements Summary

**One-liner:** JWT-less backend create API now returns slug+variantId via Result record, with TipTap rich text editor replacing the plain textarea on the edit page.

## What Was Built

Three targeted changes that lay the foundation for the create page (Plan 02):

1. **Backend API contract fix** — `POST /api/v1/admin/products` now:
   - Accepts optional `webDescription` field in request body
   - Returns `{productBaseId, variantId, slug}` instead of just `{productBaseId: Long}`
   - `CreateProductUseCase` now uses a proper `Result` record and `webDescription` in `Command`

2. **Shared RichTextEditor component** — `frontend/src/shared/ui/RichTextEditor.tsx`:
   - TipTap-powered with Bold, Italic, Bullet list, Ordered list toolbar
   - Character counter using plain text (`getText()`) length
   - Warning state at 4500+ chars, hard limit via undo() at 5000
   - Accessible: `type="button"` and `aria-label` on all toolbar buttons

3. **Edit page improvements**:
   - Container widened from `max-w-3xl` to `max-w-[1600px]` (matches dashboard)
   - `EnrichmentCard` replaced the plain `<textarea>` with `<RichTextEditor>`

## Tasks Completed

| Task | Name | Commit | Key Files |
|------|------|--------|-----------|
| 1 | Backend — webDescription + response fix | ac4ed03 | CreateProductUseCase.java, CreateProductService.java, ProductManagementController.java, CreateProductResponseDto.java |
| 2 | TipTap install + RichTextEditor component | 61fd17c | RichTextEditor.tsx, package.json |
| 3 | Edit page widen + textarea → RichTextEditor | 13a1209 | ProductEditPage.tsx, EnrichmentCard.tsx |

## Test Results

- `CreateProductServiceTest`: 3 tests, 0 failures, 0 skipped
- `ProductManagementControllerTest`: 2 tests, 0 failures, 0 skipped
- Frontend build: compiled successfully

## Deviations from Plan

### Auto-fixed Issues

None — plan executed exactly as written.

**Note on test stubs:** Plan 01-00 had already created and partially activated the test files (commits `3c0ebe5`, `859ba8e`, `c582c6f`). The test files existed without `@Disabled` when we started, so Task 1's "enable stubs" was already done. We verified the tests pass with the new production code.

## Known Stubs

None — all artifacts are fully wired.

## Success Criteria Check

- [x] BE-01: POST /api/v1/admin/products accepts optional webDescription and returns {productBaseId, variantId, slug}
- [x] BE-01: CreateProductServiceTest and ProductManagementControllerTest pass with all tests enabled (5 tests, 0 failures)
- [x] EDIT-01: Edit page container is max-w-[1600px]
- [x] EDIT-02: EnrichmentCard uses RichTextEditor instead of textarea
- [x] All code compiles and builds without errors
