# Coding Conventions

**Analysis Date:** 2026-03-21

## Naming Patterns

**Files (Frontend):**
- React components: PascalCase `.tsx` files (`ProductCard.tsx`, `CartDrawer.tsx`)
- Hooks: camelCase prefixed with `use` (`useAuth.ts`, `useQuery`)
- API modules: `index.ts` inside `api/` subdirectory per FSD slice
- Barrel files: `index.ts` per FSD slice, exporting public slice API
- Type files: `types.ts` inside `model/` subdirectory per FSD slice
- Utility functions: camelCase in `shared/lib/` (`formatPrice`, `getErrorMessage`, `cn`)

**Files (Backend):**
- Services: `[Verb][Domain]Service.java` (`CreateProductService`, `AssignUserTierService`)
- Use Case Interfaces: `[Verb][Domain]UseCase.java` (`CreateProductUseCase`, `GetListingUseCase`)
- Ports Out: `[Action][Domain]Port.java` (`LoadUserPort`, `SaveProductHierarchyPort`)
- Controllers: `[Domain]Controller.java` (`ListingController`, `ProductManagementController`)
- Mappers: `[Domain]Mapper.java` in `infrastructure/persistence/mappers/`
- Domain Models: noun-based PascalCase (`ListingVariant`, `LoyaltyProfile`, `ProductBase`)
- Domain Exceptions: descriptive PascalCase ending in `Exception` (`FieldLockException`, `ImageProcessingException`)

**Functions (Frontend):**
- React components: PascalCase function declarations (`export default function ProductCard`)
- Hooks: camelCase starting with `use` (`useAuth`, `useQueryClient`)
- API functions: camelCase verb-noun (`fetchListings`, `uploadListingImage`, `removeListingImage`)
- Utility functions: camelCase verb-noun (`formatPrice`, `getErrorMessage`)

**Variables:**
- Frontend: camelCase (`isHovered`, `coverImage`, `hasDiscount`)
- Backend Java: camelCase (`loadUserPort`, `saveUserPort`, `loadLoyaltyTierPort`)
- Constants: SCREAMING_SNAKE_CASE (`LOW_STOCK_THRESHOLD`, `API_BASE`)

**Types / Interfaces (Frontend):**
- Interfaces: PascalCase, no `I` prefix (`ProductCardProps`, `AuthState`, `UseAuthReturn`)
- Props interfaces: `[ComponentName]Props` suffix
- Exported types: PascalCase (`PaginatedResponse<T>`, `ListingVariant`, `Sku`)

**Types (Backend):**
- Domain records: PascalCase (`LoyaltyProfile`, `LoyaltyTier`)
- Command records nested inside Use Case interfaces (`CreateProductUseCase.Command`)
- JPA Entities: `[Domain]Entity.java` suffix (`UserEntity`, `LoyaltyTierEntity`)

## Code Style

**Formatting (Frontend):**
- No dedicated Prettier config detected — relies on Next.js defaults and TypeScript strict mode
- `"use client"` directive at the top of client components, before imports
- Explicit return types on exported functions and hooks
- Interface-first props definition before component body

**TypeScript:**
- Strict mode enabled (`"strict": true` in `tsconfig.json`)
- `noImplicitAny` enforced
- Type imports use `import type { ... }` syntax
- Nullable fields typed as `T | null` (not `T | undefined`) to match backend JSON contract

**Backend:**
- Constructor injection exclusively — no `@Autowired` on fields
- `@Transactional` on service layer only (not on Port adapters, except `SaveCartPort` and `SaveProductHierarchyPort`)
- `Logger` via SLF4J: `private static final Logger LOG = LoggerFactory.getLogger(ClassName.class);`
- Lombok for boilerplate on infrastructure layer; domain is pure Java with no annotations
- Domain models: mutable classes when they contain business behaviour; `record` for value objects and DTOs

## Import Organization

**Frontend order:**
1. React and Next.js core (`"use client"`, `import { useState } from "react"`, `import Image from "next/image"`)
2. Third-party libraries (`framer-motion`, `lucide-react`, `@tanstack/react-query`)
3. Internal FSD slices — upper layers first (`@/features/...`, `@/entities/...`, `@/shared/...`)
4. Relative imports from same slice (`./GeneralInfoCard`, `./SkuTableCard`)

**Path Aliases:**
- `@/*` maps to `./src/*` — all internal imports use this alias
- Never use relative `../../` imports across slice boundaries

**Backend order:**
- Standard Java library imports
- Spring imports
- Jakarta imports
- Project-internal imports (`tj.radolfa.*`)
- Static imports (Mockito, AssertJ in tests)

## Error Handling

**Frontend:**
- `getErrorMessage(err, fallback)` in `shared/lib/utils.ts` — extracts human-readable messages from `AxiosError` or generic `Error`
- Pattern: catch in mutation `onError`, call `getErrorMessage`, then `toast.error(message)` via `sonner`
- API functions throw on error (no swallowed exceptions in api modules)
- `isError` state from `useQuery` renders inline error UI (`AlertCircle` + message)
- Auth hook uses `cancelled` flag pattern to avoid state updates on unmounted components

**Backend:**
- `GlobalExceptionHandler` (`infrastructure/web/GlobalExceptionHandler.java`) handles all exceptions globally via `@RestControllerAdvice`
- Domain exceptions (`FieldLockException`, `ImageProcessingException`, `DiscountTypeInUseException`) are thrown from services and caught centrally
- `IllegalArgumentException` → 400 Bad Request (not-found entities, invalid args)
- `IllegalStateException` → 422 Unprocessable Entity (domain invariant violations)
- `AccessDeniedException` → 403 Forbidden
- `DataIntegrityViolationException` → 409 Conflict
- Non-critical failures (e.g., Elasticsearch indexing) are wrapped in try/catch with `LOG.error(...)` — no exception propagation

## Logging

**Backend Framework:** SLF4J + Logback (via Spring Boot)

**Patterns:**
- `LOG.info("[TAG] message context={}", value)` — use bracketed tags for grep-ability (`[CREATE-PRODUCT]`, `[SECURITY]`, `[FIELD-LOCK]`)
- `LOG.warn(...)` for expected business failures (security, validation, constraint violations)
- `LOG.error(...)` for unexpected failures (infrastructure errors, unhandled exceptions)
- `LOG.debug(...)` for validation details (field errors)
- No logging in domain layer

## Comments

**When to Comment:**
- Javadoc on all public interfaces, classes, and non-trivial methods
- Inline comments explaining non-obvious domain rules (`// cashback was 0% (no prior tier) so 0 points awarded`)
- Section dividers in test files using `// ── section name ──` style
- Frontend: JSDoc on exported types and interfaces explaining field semantics

**TSDoc / Javadoc:**
- Use `/** ... */` on all public API exports (interfaces, functions, hooks)
- Document nullability explicitly in comments (`null for guests / users without a tier`)
- Backend use-case interfaces include `@return` and business rule notes

## Function Design

**Size:** Services follow single-responsibility — one service per use case (one `execute()` method)

**Parameters:**
- Backend service methods accept a typed `Command` record (nested inside the UseCase interface)
- Frontend API functions use explicit typed parameters with default values for pagination
- No raw `any` or `Object` parameters

**Return Values:**
- Backend services return domain types or primitives; never JPA entities
- Frontend API functions always return typed promises; `void` for mutation operations
- React components return JSX; hooks return typed `UseXReturn` interfaces

## Module Design

**FSD Barrel Files:**
- Every FSD slice exposes a public API through `index.ts`
- Only items exported from `index.ts` may be imported by upper slices
- Internal slice files (components not in index) are considered private

**Backend Module Contracts:**
- Use Case interfaces define the `Command` record and return type — this is the contract
- Out-Ports define the interface only; implementations live in `infrastructure/persistence/`
- Domain layer has zero compile dependencies on Spring, JPA, or Jackson
