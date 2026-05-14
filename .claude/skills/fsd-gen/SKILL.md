---
name: fsd-gen
description: Scaffold a new FSD slice (Entity, Feature, or Widget) with TanStack Query hooks, Shadcn UI imports, and correct dependency rules.
---

**ultrathink**

# FSD Slice Generator

Scaffold a complete Feature-Sliced Design slice for the Radolfa frontend.

## Inputs

- `$NAME` — kebab-case slice name (e.g., `product-creation`, `order-history`)
- `$LAYER` — one of: `entities`, `features`, `widgets`
- `$DESCRIPTION` — Brief description of what the slice does

## Step 1 — Validate Placement

Before creating anything, verify FSD rules:

- **Dependency direction** is strictly downward: `app → pages → widgets → features → entities → shared`
- Cross-slice imports at the same layer are **forbidden** (e.g., `features/cart` must NOT import from `features/checkout`)
- If shared logic is needed between slices, extract to `entities/` or `shared/`

## Step 2 — Create Directory Structure

```
frontend/src/$LAYER/$NAME/
├── api/
│   └── $NAME.ts          # Axios API calls
├── model/
│   └── types.ts           # TypeScript interfaces
├── ui/
│   └── ${PascalName}.tsx  # Main component
└── index.ts               # Public API barrel export
```

## Step 3 — Generate Files

### `index.ts` (Public API)
```typescript
export { ${PascalName} } from "./ui/${PascalName}";
export type { /* main types */ } from "./model/types";
```

### `model/types.ts`
```typescript
export interface ${PascalName}Props {
  // Component props
}
```

### `api/${name}.ts` (API calls)
```typescript
import { apiClient } from "@/shared/api";
import type { ${ResponseType} } from "../model/types";

export async function fetch${PascalName}(): Promise<${ResponseType}> {
  const { data } = await apiClient.get<${ResponseType}>(`/api/v1/${endpoint}`);
  return data;
}
```

### `api/use-${name}.ts` (TanStack Query hook) — **Features and Entities only**
```typescript
"use client";

import { useQuery } from "@tanstack/react-query";
import { fetch${PascalName} } from "./${name}";

export function use${PascalName}() {
  return useQuery({
    queryKey: ["${query-key}"],
    queryFn: fetch${PascalName},
  });
}
```

**Query key conventions** (from `frontend/CLAUDE.md`):
| Data | Key pattern |
|------|-------------|
| Listing grid | `["listings", page, search]` |
| Single listing | `["listing", slug]` |
| Categories | `["categories"]` |
| Cart | `["cart"]` |
| Orders | `["my-orders"]` |
| Colors | `["colors"]` |
| Tags | `["tags"]` |
| Users (admin) | `["users", page, search]` |

For new data, follow the pattern: `["${resource-name}", ...params]`.

### `ui/${PascalName}.tsx`
```tsx
"use client";

import { /* Shadcn components */ } from "@/shared/ui/button";

interface ${PascalName}Props {
  // props
}

export function ${PascalName}({ }: ${PascalName}Props) {
  return (
    <div>
      {/* Component content */}
    </div>
  );
}
```

**Component rules:**
- `"use client"` only when hooks, event handlers, or browser APIs are used
- Import Shadcn from `@/shared/ui/` — never from `@/components`
- Functional components only — no class components
- No `useEffect` for data fetching — use TanStack Query hooks
- Absolute imports only: `@/entities/...`, `@/shared/...`, `@/features/...`

## Step 4 — Mutations (Features layer only)

If the feature performs write operations, also generate a mutation hook:

### `api/use-${name}-mutation.ts`
```typescript
"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";

export function use${PascalName}Mutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: ${RequestType}) => submit${PascalName}(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["${related-query-key}"] });
    },
  });
}
```

## Output

Print a tree view of all created files and their purposes.