---
name: bridge
description: Synchronize Java backend types (DTOs, enums, read models) to TypeScript interfaces on the frontend, maintaining FSD placement rules.
---

**ultrathink**

# Full-Stack Type Synchronizer

Sync Java backend types to TypeScript interfaces so the frontend always matches the backend contract.

## Source Locations (Backend)

| Source | Path | What to sync |
|--------|------|-------------|
| Response/Request DTOs | `infrastructure/web/dto/*.java` | All `record` types |
| Read Models | `application/readmodel/*.java` | View records consumed by controllers |
| Enums | `domain/model/*.java` | Only enum types (e.g., `AttributeType`, `OrderStatus`, `UserRole`) |

**Do NOT sync:** Domain classes (`Cart.java`, `Sku.java`, etc.) — the frontend never consumes domain internals directly.

## Target Locations (Frontend — FSD-compliant)

| Type | Target |
|------|--------|
| Response DTOs / Read Models | `frontend/src/entities/{domain}/model/types.ts` |
| Request DTOs | `frontend/src/features/{feature}/model/types.ts` |
| Shared enums | `frontend/src/shared/types/enums.ts` |

### Domain-to-directory mapping

| Java area | Frontend entity/feature |
|-----------|------------------------|
| `UserDto`, `AuthResponseDto` | `entities/user` |
| `CartDto`, `CartItemDto` | `entities/cart` |
| `OrderDto`, `OrderItemDto` | `entities/order` |
| `LoyaltyTierDto` | `entities/loyalty` |
| Listing/Product response types | `entities/product` |
| `CreateProductRequestDto` | `features/product-creation` |
| `UpdateListingRequest` | `features/product-edit` |
| `SubmitReviewRequestDto` | `features/review-submission` |
| `CheckoutRequestDto` | `features/checkout` |
| Blueprint types | `features/blueprint-management` or `features/product-creation` |

## Type Mapping Rules

| Java | TypeScript |
|------|-----------|
| `String` | `string` |
| `Long`, `Integer`, `int`, `Double`, `double`, `BigDecimal` | `number` |
| `Boolean`, `boolean` | `boolean` |
| `List<T>` | `T[]` |
| `Set<T>` | `T[]` |
| `Map<String, T>` | `Record<string, T>` |
| `LocalDateTime`, `Instant`, `ZonedDateTime` | `string` |
| `@Nullable` field or `Optional<T>` | `T \| null` |
| Java enum (e.g., `AttributeType`) | `type AttributeType = 'TEXT' \| 'NUMBER' \| 'ENUM' \| 'MULTI'` |

### Nested records
When a Java record contains another record (e.g., `CreateProductRequestDto` contains `ListingVariantCreationDto`), generate both and use a named reference:

```typescript
export interface ListingVariantCreationDto {
  colorKey: string;
  // ...
}

export interface CreateProductRequest {
  name: string;
  variants: ListingVariantCreationDto[];
}
```

## File Header

Every generated file must begin with:

```typescript
// Synced from backend — do not edit manually
```

## Execution Steps

1. **Scan** the backend source locations listed above.
2. **Parse** each Java `record` or `enum`, extracting field names, types, and nullability.
3. **Map** each Java type to its TypeScript equivalent using the table above.
4. **Place** the generated interface in the correct FSD location.
5. **Merge** — if the target `types.ts` already has manually written types, append the synced types below them (after the header comment). Never overwrite manual types.
6. **Report** a summary of synced types and their target files.

## Output

Print a table:
| Java Source | → | TypeScript Target | Types Synced |
|---|---|---|---|