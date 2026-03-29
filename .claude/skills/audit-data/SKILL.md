---
name: audit-data
description: Validate that current changes respect Hexagonal Architecture, FSD, and security rules across the full stack.
---

**ultrathink**

# Architecture & Security Audit

Run this audit against the current diff or a set of changed files to catch architectural violations before they land.

## 1. Backend — Hexagonal Architecture Rules

### Domain Layer Purity
Scan all files in `backend/src/main/java/tj/radolfa/domain/`:

```bash
grep -rn "import org.springframework\|import jakarta.persistence\|import com.fasterxml.jackson\|import lombok" backend/src/main/java/tj/radolfa/domain/
```

**Expected result:** Empty. The domain package must have **zero** framework imports.

### Constructor Injection Only
Scan services for field injection:

```bash
grep -rn "@Autowired" backend/src/main/java/tj/radolfa/application/services/
```

**Expected result:** Empty. All dependencies must be constructor-injected.

### @Transactional Placement
Verify `@Transactional` is only on service methods, not on adapters:

```bash
grep -rn "@Transactional" backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/
```

**Expected result:** Empty (except `SaveCartPort` and `SaveProductHierarchyPort` adapters which are explicitly exempted in the project rules).

### JPA Entity Isolation
Verify no controller imports from `persistence.entity`:

```bash
grep -rn "import tj.radolfa.infrastructure.persistence.entity" backend/src/main/java/tj/radolfa/infrastructure/web/
```

**Expected result:** Empty. Controllers must never reference JPA entities.

## 2. Frontend — FSD Rules

### No Cross-Slice Imports
For each feature slice, verify it doesn't import from other feature slices:

```bash
grep -rn "from \"@/features/" frontend/src/features/ --include="*.ts" --include="*.tsx" | grep -v "index.ts"
```

Flag any line where `features/X` imports from `features/Y` (X ≠ Y).

### No Legacy Component Imports
```bash
grep -rn "from \"@/components" frontend/src/
```

**Expected result:** Empty. All UI components must come from `@/shared/ui/`.

### No useEffect for Data Fetching
```bash
grep -rn "useEffect.*fetch\|useEffect.*api\|useEffect.*axios" frontend/src/
```

**Expected result:** Empty. TanStack Query must be used for all data fetching.

### Correct Directive Usage
Check that `"use client"` is only on files that use hooks or event handlers, and is not on pure server components or layout files.

## 3. Security Rules

### Price/Stock ADMIN-Only
Verify that price and stock mutation endpoints are restricted to `ADMIN`:

```bash
grep -B5 "price\|stock" backend/src/main/java/tj/radolfa/infrastructure/web/ProductManagementController.java | grep -i "PreAuthorize\|hasRole"
```

Mutations on `PUT /admin/skus/{id}/price` and `PUT /admin/skus/{id}/stock` must require `ADMIN` role.

### No ERP References
```bash
grep -ri "erp" frontend/src/ backend/src/main/java/ --include="*.java" --include="*.ts" --include="*.tsx" | grep -v "test" | grep -v ".class"
```

Flag any UI labels or user-facing strings that reference "ERP". Use "Catalog Data" or "System-managed" instead.

### Manager Read-Only Fields
In admin product edit UI, verify price and stock fields render with a `Lock` icon for MANAGER role — not disabled inputs that look editable.

## Output

Print a violation report in this format:

| # | Category | Severity | File | Line | Violation |
|---|----------|----------|------|------|-----------|
| 1 | Domain Purity | 🔴 CRITICAL | ... | ... | Spring import in domain |
| 2 | FSD | 🟡 WARNING | ... | ... | Cross-slice import |

If all checks pass, print: `✅ All architectural and security rules are satisfied.`