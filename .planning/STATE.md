# Project State

**Last updated:** 2026-03-21
**Current phase:** Phase 1 — Product Create & Edit Overhaul
**Status:** Ready for planning

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-03-21)

**Core value:** Admins can create and edit products with all catalog data in one cohesive place
**Current focus:** Phase 1 — Product Create & Edit Overhaul

## Phase Status

| Phase | Name | Status |
|-------|------|--------|
| 1 | Product Create & Edit Overhaul | ○ Pending |

## Codebase Map

`.planning/codebase/` — mapped 2026-03-21

Key files for Phase 1:
- `frontend/src/features/product-creation/ui/CreateProductDialog.tsx` — to be replaced by a page
- `frontend/src/features/product-edit/ui/ProductEditPage.tsx` — width fix + rich text
- `frontend/src/features/product-edit/ui/EnrichmentCard.tsx` — plain textarea → rich text editor
- `frontend/src/app/(admin)/manage/page.tsx` — remove dialog, add navigation to new page
- `frontend/src/app/(admin)/manage/products/new/page.tsx` — new route to create
- `backend/.../web/dto/CreateProductRequestDto.java` — add webDescription field
- `backend/.../ports/in/product/CreateProductUseCase.java` — add webDescription to Command
