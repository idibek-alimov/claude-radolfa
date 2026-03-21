# Phase 1: Product Create & Edit Overhaul — Research

**Researched:** 2026-03-21
**Domain:** TipTap rich text editor, React multi-step form flow, Spring Boot DTO extension
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Use TipTap (headless, ProseMirror-based). Install `@tiptap/react`, `@tiptap/starter-kit`.
- **D-02:** Editor used in two places: the new create page and the existing `EnrichmentCard` on the edit page.
- **D-03:** Keep the 5000-character limit already enforced by `EnrichmentCard`. Show char count below editor on both pages.
- **D-04:** Two-column layout — left column: name, category, color, rich text description. Right column: image upload zone + SKU table.
- **D-05:** Page uses `max-w-[1600px]` container (same as the admin dashboard target width from EDIT-01).
- **D-06:** FSD placement: new feature slice `features/product-creation-page/` with a `CreateProductPage` component. The `app/(admin)/manage/products/new/page.tsx` route imports it. The old `CreateProductDialog` remains but the "New Product" button on the manage page navigates to the new route instead of opening the dialog.
- **D-07:** Simple file input (click to browse, no drag-drop required). Multiple files accepted. Images are optional (CREATE-04).
- **D-08:** Upload happens after product creation succeeds (CREATE-03 sequence: create product → upload images if any → redirect to edit page).
- **D-09:** No elaborate progress indicator needed — a loading spinner on the submit button is sufficient. Button label changes: "Creating…" during product creation, "Uploading images…" during image upload.
- **D-10:** Change `max-w-3xl` to `max-w-[1600px]` in `ProductEditPage.tsx`. No other layout changes to the edit page.

### Claude's Discretion
- Exact TipTap toolbar buttons (bold, italic, bullet list, ordered list is a reasonable default set)
- SKU table reuse strategy on create page (can reuse `SkuTableCard` logic or inline it)
- Error handling for partial failures (image upload fails after product created — toast error, stay on create page or redirect anyway)

### Deferred Ideas (OUT OF SCOPE)
- Drag-and-drop image upload
- Multi-color variant creation in one form
- Draft/publish workflow
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| BE-01 | `POST /api/v1/admin/products` accepts optional `webDescription` field and persists it on the listing variant | Backend chain fully audited; all 4 layers identified for change |
| CREATE-01 | Clicking "New Product" navigates to `/manage/products/new` (full page, not popup) | `manage/page.tsx` ProductManagement sub-component change documented |
| CREATE-02 | Create page has name, category, color, description (rich text), image upload zone, and SKU table | All reusable building blocks identified from CreateProductDialog and ImageCard |
| CREATE-03 | On submit — creates product with description in one backend call, then uploads images if provided, then redirects to edit page | Sequential async pattern documented; critical slug-response mismatch found (see Critical Finding) |
| CREATE-04 | Image upload on create is optional — product can be created without images | Conditional upload branch documented |
| EDIT-01 | Edit page container matches admin dashboard width (`max-w-[1600px]`) | Single `max-w-3xl` change confirmed in `ProductEditPage.tsx` line 48 |
| EDIT-02 | Description field on edit page is a rich text editor (not plain textarea) | `EnrichmentCard.tsx` textarea at lines 49–55 is the exact replacement target |
</phase_requirements>

---

## Summary

This phase has two tracks: a backend DTO extension and two frontend feature changes. The backend work is a straightforward hexagonal-layer propagation — `webDescription` is added as a nullable `String` field at the DTO, Command, service, and domain-model construction levels. The domain model (`ListingVariant`) already has `webDescription` as a field with an `updateWebDescription()` mutation method; the service just needs to pass it through during creation.

The frontend work is a new `features/product-creation-page/` FSD slice that composes existing building blocks (`CreateProductDialog`'s SKU table logic, the image upload pattern from `ImageCard`, TipTap for the description field). The edit page changes are minimal: one CSS class change and a textarea-to-TipTap replacement in `EnrichmentCard`.

A critical discrepancy was found during code audit: the backend `POST /api/v1/admin/products` controller currently returns only `{ productBaseId: Long }`, but the frontend type `CreateProductResponse` declares `{ productBaseId, variantId, slug }` and the redirect logic uses `data.slug`. The backend response must be fixed as part of BE-01 to also return the `slug` (at minimum), or the create page cannot redirect to the edit page. This is the highest-risk integration point.

**Primary recommendation:** Fix the backend response to return `slug` first, then build the frontend create page against the corrected contract.

---

## Critical Finding: Backend Response Mismatch

**Finding:** `ProductManagementController.createProduct()` at line 60–77 currently returns:
```java
ResponseEntity.status(HttpStatus.CREATED).body(Map.of("productBaseId", productBaseId))
```
Only `productBaseId` is returned.

**Problem:** `CreateProductResponse` type in `frontend/src/entities/product/api/admin.ts` declares:
```typescript
export interface CreateProductResponse {
  productBaseId: number;
  variantId: number;
  slug: string;
}
```
And `data.slug` is used to redirect to `/manage/products/${slug}/edit`. The `slug` is never returned, so this redirect is broken today. BE-01 **must** fix the response to return `slug` (and optionally `variantId`).

**Fix required in:** `ProductManagementController`, `CreateProductService.execute()` return type (change from `Long` to a result record), `CreateProductUseCase` interface.

---

## Standard Stack

### Core (already installed)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19 | UI | Project standard |
| Next.js | 15 (App Router) | Routing | Project standard |
| TanStack Query | v5 | Server state / mutations | Project standard |
| Tailwind CSS | 3.4 | Utility styling | Project standard |
| Shadcn UI | current | Component primitives | Project standard — components in `shared/ui/` |
| Axios | 1.x | HTTP client | Project standard — `shared/api/axios.ts` |
| sonner | current | Toast notifications | Project standard |

### To Install
| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `@tiptap/react` | 3.20.4 | React TipTap bindings | Locked decision D-01 |
| `@tiptap/starter-kit` | 3.20.4 | Bold, italic, lists, heading, code, history, paragraph | Locked decision D-01; covers all needed toolbar actions in one package |

**Installation:**
```bash
npm install @tiptap/react @tiptap/starter-kit --prefix frontend
```

**Version verification:** Confirmed from npm registry 2026-03-21 — both packages at `3.20.4`.

### Backend (already present)
| Library | Version | Purpose |
|---------|---------|---------|
| Spring Boot | 3.4.4 | Application framework |
| Bean Validation | (jakarta) | `@Valid` DTO validation |
| MapStruct | 1.5.5.Final | Already used; not needed for this change (no new mapping layer) |

---

## Architecture Patterns

### Recommended Project Structure (additions for this phase)

```
frontend/src/
├── features/
│   └── product-creation-page/          # NEW FSD slice (D-06)
│       ├── index.ts                     # barrel: export { CreateProductPage }
│       └── ui/
│           └── CreateProductPage.tsx    # two-column full-page form
├── app/(admin)/manage/products/
│   ├── [slug]/edit/page.tsx             # existing
│   └── new/                             # NEW route
│       └── page.tsx                     # imports CreateProductPage from feature slice
```

### Pattern 1: TipTap Controlled Editor (shared `RichTextEditor` component)

**What:** A reusable `RichTextEditor` component wrapping TipTap with a standard toolbar. Used in both `EnrichmentCard` (EDIT-02) and `CreateProductPage` (CREATE-02).

**When to use:** Any place that replaces a `<textarea>` for `webDescription`.

**Placement:** `shared/ui/RichTextEditor.tsx` — used by both features without cross-slice imports.

**Example (TipTap React integration):**
```tsx
// Source: https://tiptap.dev/docs/editor/getting-started/overview (TipTap v3 docs)
"use client";

import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";

interface Props {
  content: string;
  onChange: (html: string) => void;
  maxLength?: number;
}

export function RichTextEditor({ content, onChange, maxLength = 5000 }: Props) {
  const editor = useEditor({
    extensions: [StarterKit],
    content,
    onUpdate: ({ editor }) => {
      const html = editor.getHTML();
      if (!maxLength || html.length <= maxLength) {
        onChange(html);
      }
    },
  });

  return (
    <div className="rounded-md border border-input bg-transparent text-sm shadow-sm">
      {/* Toolbar: Bold, Italic, BulletList, OrderedList */}
      <div className="flex gap-1 border-b px-2 py-1.5">
        <button
          type="button"
          onClick={() => editor?.chain().focus().toggleBold().run()}
          className={editor?.isActive("bold") ? "bg-muted rounded px-1.5 py-0.5" : "px-1.5 py-0.5"}
        >B</button>
        {/* etc. */}
      </div>
      <EditorContent editor={editor} className="px-3 py-2 min-h-[100px]" />
    </div>
  );
}
```

**Key integration note:** TipTap stores content as HTML strings. `EnrichmentCard` already stores `webDescription` as a string and sends it to `PUT /api/v1/listings/{slug}`. No backend changes are needed for EDIT-02. The create page sends `webDescription` as HTML string too — the backend stores it as `TEXT` (no structural concern).

**Character count:** Use `editor.getText().length` (plain text length) rather than `editor.getHTML().length` (HTML includes tags). This matches the spirit of the existing 5000-char limit that was on the plain textarea. The planner must decide which to use; plain text length is more user-friendly.

### Pattern 2: Sequential Async Submit (CREATE-03)

**What:** The submit handler on `CreateProductPage` runs three sequential steps.

**Example:**
```tsx
const [submitPhase, setSubmitPhase] = useState<"idle" | "creating" | "uploading">("idle");

async function handleSubmit() {
  if (!validate()) return;
  setSubmitPhase("creating");
  let slug: string;
  try {
    const result = await createProduct({ name, categoryId, colorId, webDescription, skus });
    slug = result.slug;
  } catch (err) {
    toast.error(getErrorMessage(err));
    setSubmitPhase("idle");
    return;
  }

  if (files.length > 0) {
    setSubmitPhase("uploading");
    try {
      for (const file of files) {
        await uploadListingImage(slug, file);
      }
    } catch (err) {
      toast.error(getErrorMessage(err)); // partial failure — show error
      // redirect anyway since product exists
    }
  }

  router.push(`/manage/products/${slug}/edit`);
}
```

**Button label logic:**
```tsx
const buttonLabel =
  submitPhase === "creating" ? t("creating") :
  submitPhase === "uploading" ? t("uploadingImages") :
  t("createProduct");
const isSubmitting = submitPhase !== "idle";
```

### Pattern 3: File Input with Multiple Files (CREATE-02, CREATE-04)

**What:** `<input type="file" multiple accept="image/*">` triggered via a ref. Selected files held in local `useState<File[]>`. Preview shown as thumbnails using `URL.createObjectURL()`.

**Consistent with:** `ImageCard.tsx` uses single-file upload one at a time; the create page should upload each file individually using the same `uploadListingImage(slug, file)` function in a loop.

### Pattern 4: Inline Validation (existing pattern from CreateProductDialog)

**What:** `errors` state as `Record<string, string>`, populated in a `validate()` function before submit. No external form library needed.

**Reuse directly from:** `CreateProductDialog.tsx` lines 84–96. The create page extends this pattern to include a `webDescription` validation (optional, as description is not required by the backend).

### Anti-Patterns to Avoid

- **Don't put logic in `app/` pages:** The `new/page.tsx` route must only wrap `CreateProductPage` in `ProtectedRoute`. All state and mutation logic lives in the feature slice.
- **Don't import between features at the same FSD level:** `features/product-creation-page` must not import from `features/product-creation`. Any shared logic should be in `entities/product/api/admin.ts` (already the case for `createProduct`).
- **Don't use `editor.getHTML().length` for character limit enforcement:** This inflates the visible count with HTML tags. Use `editor.getText().length` or `editor.storage.characterCount?.characters()` if `@tiptap/extension-character-count` is added.
- **Don't trigger TipTap `onUpdate` on initial render:** TipTap fires `onUpdate` on first mount if `content` prop changes. Guard with a dirty-tracking pattern to avoid unnecessary mutation calls in `EnrichmentCard`.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Rich text editing | Custom contenteditable | `@tiptap/react` + `@tiptap/starter-kit` | Selection, undo, paste normalization, XSS-safe serialization — all solved |
| Toast notifications | Custom toast system | `sonner` (already installed) | Project standard |
| Form field components | Custom inputs | Shadcn `Input`, `Button` from `shared/ui/` | Already consistent across edit page |
| HTTP client | Fetch wrapper | Axios instance at `shared/api/axios.ts` | Auth cookie handling, interceptors, error shape — all wired |
| Image upload progress | Custom XMLHttpRequest | Sequential `uploadListingImage()` calls from `entities/product/api` | Existing function handles multipart, headers, and error shape |

**Key insight:** TipTap's StarterKit extension bundle covers bold, italic, headings, bullet list, ordered list, code, blockquote, hard break, horizontal rule, and undo/redo. There is no need to install individual extensions for a basic toolbar.

---

## Backend Change Chain for BE-01

Four files require modification in strict hexagonal order:

### 1. `CreateProductUseCase.java` — Application layer interface
Add `webDescription` to `Command` record (nullable `String`):
```java
record Command(
    String name,
    Long   categoryId,
    Long   colorId,
    String webDescription,  // ADD — nullable
    List<SkuDefinition> skus
) { ... }
```
Also change return type from `Long` to a `Result` record:
```java
record Result(Long productBaseId, Long variantId, String slug) {}
Result execute(Command command);
```

### 2. `CreateProductService.java` — Application layer service
- Accept `command.webDescription()` and pass it to `ListingVariant` constructor (already accepts `webDescription` as 5th constructor parameter).
- Return `new CreateProductUseCase.Result(savedBase.getId(), savedVariant.getId(), savedVariant.getSlug())` instead of just `savedBase.getId()`.

### 3. `CreateProductRequestDto.java` — Infrastructure layer DTO
Add optional field:
```java
public record CreateProductRequestDto(
    @NotBlank String name,
    @NotNull Long categoryId,
    @NotNull Long colorId,
    String webDescription,           // ADD — no validation constraint; nullable
    @NotEmpty @Valid List<SkuDefinitionDto> skus
) { ... }
```

### 4. `ProductManagementController.java` — Infrastructure layer controller
- Map `request.webDescription()` into the `Command`.
- Change response body from `Map<String, Long>` to a proper DTO returning `productBaseId`, `variantId`, `slug`.

### 5. `CreateProductResponseDto.java` — NEW Infrastructure layer DTO
```java
public record CreateProductResponseDto(
    Long productBaseId,
    Long variantId,
    String slug
) {}
```

**No domain model change needed:** `ListingVariant` constructor already accepts `webDescription` at position 5.

---

## Common Pitfalls

### Pitfall 1: TipTap SSR Hydration Error
**What goes wrong:** Next.js server-renders a different HTML than TipTap produces client-side, causing a React hydration mismatch and console error.
**Why it happens:** TipTap uses `window` and browser-only ProseMirror APIs. Next.js 15 App Router server-renders by default.
**How to avoid:** Add `"use client"` to any component that instantiates `useEditor()`. The `RichTextEditor` wrapper component, `EnrichmentCard`, and `CreateProductPage` must all be client components.
**Warning signs:** "Text content does not match server-rendered HTML" error in browser console on first load.

### Pitfall 2: TipTap Content Sync Loop
**What goes wrong:** Editor `onUpdate` fires `onChange(html)` → parent state updates → parent passes new `content` prop → editor re-renders → fires `onUpdate` again. Infinite loop.
**Why it happens:** TipTap's `content` prop is only used for initial value; don't update it reactively after mount.
**How to avoid:** Initialize `useEditor` with the initial value only. Do NOT pass a reactive `content` prop that changes after mount. Use the `editor.commands.setContent()` imperative API only when explicitly resetting (e.g., after successful save).
**Warning signs:** Maximum update depth exceeded, browser tab becoming unresponsive.

### Pitfall 3: Backend Response Mismatch (already a bug)
**What goes wrong:** `POST /api/v1/admin/products` returns `{ productBaseId }` but the frontend expects `{ productBaseId, variantId, slug }`. The `data.slug` used for redirect is `undefined`, causing navigation to `/manage/products/undefined/edit`.
**Why it happens:** Frontend type was written anticipating the full response; backend implementation only returns `productBaseId`. This bug exists in the current codebase but is masked because `CreateProductDialog` is only reachable from the manage page which redirects via `onCreated(data.slug)` — `data.slug` would be `undefined` today.
**How to avoid:** Fix the backend response in BE-01 before building the frontend create page.
**Warning signs:** Edit page loads with `slug = "undefined"` in URL.

### Pitfall 4: Multiple File Upload — Files State Not Cleared After Submit
**What goes wrong:** After successful product creation and upload, the user is redirected to the edit page. But if navigation is slow or fails, the create page may still be mounted with stale `files` state. A retry would attempt to re-upload already-uploaded files.
**How to avoid:** Clear `files` state after upload completes, before `router.push()`. Since `router.push` unmounts the component anyway in App Router, this is low risk but good hygiene.

### Pitfall 5: `@tiptap/starter-kit` v3 breaking changes from v2
**What goes wrong:** Community tutorials and StackOverflow examples are often written for TipTap v2. The import paths and some APIs changed in v3.
**Why it happens:** TipTap v3 (3.x) uses the same `@tiptap/react` package name but the `useEditor` hook signature has minor changes.
**How to avoid:** Use only the official TipTap v3 documentation. Do not copy v2 examples. The `useEditor` call signature is compatible, but toolbar `isActive` checks and command chains are identical in v2 and v3.
**Warning signs:** TypeScript errors on extension options.

---

## Code Examples

### TipTap minimal working setup (verified, TipTap v3)
```tsx
// Source: https://tiptap.dev/docs/editor/getting-started/overview
"use client";
import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";

const editor = useEditor({
  extensions: [StarterKit],
  content: "<p>Hello World</p>",
  onUpdate: ({ editor }) => {
    console.log(editor.getHTML()); // "<p>Hello World</p>"
  },
});
```

### Toolbar button pattern
```tsx
// Toggle bold — isActive drives visual feedback
<button
  type="button"
  onClick={() => editor?.chain().focus().toggleBold().run()}
  disabled={!editor?.can().chain().focus().toggleBold().run()}
  data-active={editor?.isActive("bold")}
  className="px-2 py-1 rounded data-[active=true]:bg-muted"
>
  B
</button>
```

### File input with multiple selection and preview
```tsx
// No library needed — native file input
const [files, setFiles] = useState<File[]>([]);
const inputRef = useRef<HTMLInputElement>(null);

<input
  ref={inputRef}
  type="file"
  multiple
  accept="image/*"
  className="hidden"
  onChange={(e) => setFiles(Array.from(e.target.files ?? []))}
/>
<Button type="button" onClick={() => inputRef.current?.click()}>
  Add Images
</Button>
```

### Upload loop (reusing existing API function)
```tsx
// Source: frontend/src/entities/product/api/index.ts — uploadListingImage
for (const file of files) {
  await uploadListingImage(slug, file); // POST /listings/{slug}/images, field "files"
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Product creation via popup dialog | Full-page form at `/manage/products/new` | Phase 1 | Better UX for complex forms |
| Plain `<textarea>` for webDescription | TipTap rich text editor | Phase 1 | HTML stored in DB; storefront can render rich content |
| Edit page at `max-w-3xl` | `max-w-[1600px]` matching dashboard | Phase 1 | Consistent admin layout |

---

## Open Questions

1. **Character count for TipTap: plain text vs HTML length**
   - What we know: Current `textarea` enforces `maxLength={5000}` which counts raw characters.
   - What's unclear: TipTap HTML for "hello" is `<p>hello</p>` (13 chars). If we count HTML length, the user gets far fewer effective characters. If we count `editor.getText().length`, it matches the original intent.
   - Recommendation: Count `editor.getText().length` to match the user-visible character count. The planner should specify this explicitly so the implementor doesn't accidentally count HTML bytes.

2. **Partial failure on image upload: redirect or stay?**
   - What we know: D-09 says spinner only; D-03 says stay on create page if image upload fails.
   - What's unclear: CONTEXT.md says "Claude's Discretion" for partial failure handling. The CONTEXT.md text says "toast error, stay on create page or redirect anyway".
   - Recommendation: Redirect anyway. The product exists at this point. Show a toast error for the failed images. The user can re-upload from the edit page using the existing `ImageCard`. Staying on the create page would be confusing since the product is already created.

3. **`webDescription` in Elasticsearch index**
   - What we know: `CreateProductService.indexVariant()` currently passes `null` for webDescription (line 131 in service).
   - What's unclear: Should the ES index be updated to include `webDescription` when it's passed at creation time? This is not in scope for Phase 1 (no search-by-description requirement), but the indexing call will still pass `null` even when a description is provided.
   - Recommendation: Out of scope for Phase 1. The listing can be reindexed later. The admin reindex button already covers this.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (Spring Boot 3.4.4) |
| Config file | none — Spring Boot auto-configures |
| Quick run command | `./mvnw test -pl backend -Dtest=CreateProductServiceTest -q` |
| Full suite command | `./mvnw test -pl backend` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| BE-01 | `CreateProductService` propagates `webDescription` to `ListingVariant` | unit | `./mvnw test -pl backend -Dtest=CreateProductServiceTest -q` | Wave 0 |
| BE-01 | Controller returns `slug` + `variantId` in response body | integration | `./mvnw test -pl backend -Dtest=ProductManagementControllerTest -q` | Wave 0 |
| CREATE-01 through CREATE-04 | Frontend routing + form flow | manual smoke | n/a — no Jest/Vitest in project | manual only |
| EDIT-01 | Width CSS class change | manual visual | n/a | manual only |
| EDIT-02 | TipTap renders in EnrichmentCard, saves HTML | manual smoke | n/a | manual only |

**Note:** No JavaScript test framework (Jest/Vitest) is configured in this project (`frontend/package.json` has no test scripts). All frontend verification is manual smoke testing.

### Sampling Rate
- **Per task commit:** `./mvnw test -pl backend -Dtest=CreateProductServiceTest -q`
- **Per wave merge:** `./mvnw test -pl backend`
- **Phase gate:** Full backend suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `backend/src/test/java/tj/radolfa/application/services/CreateProductServiceTest.java` — covers BE-01 (webDescription propagation to ListingVariant)
- [ ] `backend/src/test/java/tj/radolfa/infrastructure/web/ProductManagementControllerTest.java` — covers BE-01 (response contains `slug`, `variantId`, `productBaseId`)

---

## Sources

### Primary (HIGH confidence)
- Direct code audit of `/frontend/src/features/product-creation/ui/CreateProductDialog.tsx` — SKU table structure, validation pattern, existing API call
- Direct code audit of `/frontend/src/features/product-edit/ui/EnrichmentCard.tsx` — textarea lines 49–55, character count pattern, save mutation
- Direct code audit of `/frontend/src/features/product-edit/ui/ProductEditPage.tsx` — `max-w-3xl` at line 48 confirmed as the exact target
- Direct code audit of `/frontend/src/app/(admin)/manage/page.tsx` — `setIsCreateOpen(true)` at line 229 is the exact change target
- Direct code audit of `/frontend/src/entities/product/api/admin.ts` — `createProduct()` and `CreateProductRequest` interface
- Direct code audit of `/backend/.../CreateProductRequestDto.java` — current fields
- Direct code audit of `/backend/.../CreateProductUseCase.java` — Command record, return type `Long`
- Direct code audit of `/backend/.../CreateProductService.java` — full implementation, `ListingVariant` constructor call
- Direct code audit of `/backend/.../ProductManagementController.java` — response body is `Map.of("productBaseId", productBaseId)` only
- Direct code audit of `/backend/.../domain/model/ListingVariant.java` — constructor accepts `webDescription` at position 5 already
- npm registry 2026-03-21: `@tiptap/react` and `@tiptap/starter-kit` both at version `3.20.4`

### Secondary (MEDIUM confidence)
- TipTap v3 official documentation — `useEditor` hook, `EditorContent`, `StarterKit` extension bundle, `onUpdate` callback, toolbar command chains

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries verified via direct code audit and npm registry
- Architecture patterns: HIGH — derived from direct codebase inspection, not assumptions
- Backend change chain: HIGH — all 4 layers audited; ListingVariant constructor confirmed as accepting webDescription
- Pitfalls: HIGH for items 1–3 (verified by code inspection); MEDIUM for items 4–5 (general TipTap knowledge)
- Critical finding (response mismatch): HIGH — confirmed by reading both controller and frontend type definitions

**Research date:** 2026-03-21
**Valid until:** 2026-04-21 (stable stack; TipTap 3.x API unlikely to change)
