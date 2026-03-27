# Wildberries Frontend Research: Product Creation UI/UX

This report analyzes the Wildberries (WB) seller portal's product creation flow to inform the Radolfa frontend implementation.

## 1. The "Category-First" Workflow
The most critical UX pattern in Wildberries is that **Category selection is the first step**. 

- **Why?** The category determines the "Blueprint" (mandatory and optional attributes) for the rest of the form.
- **Radolfa Logic:** Our backend already supports this via `GET /api/v1/categories/{id}/blueprint`. The frontend should fetch this as soon as a category is picked.

## 2. The Stepper (Wizard) Model
Wildberries uses a structured, multi-step process rather than one giant form. This reduces cognitive load.

### Step 1: Basic Information
- **Fields:** Product Name, Category (First), Brand, Vendor Code (External Ref).
- **UX:** A searchable category tree or dropdown. Once selected, the UI "prepares" the subsequent attribute fields.

### Step 2: Media Management
- **UX:** A drag-and-drop zone for high-quality images.
- **Logic:** Images are uploaded independently to our new `POST /api/v1/admin/images/upload` endpoint. The frontend stores the returned URLs to be sent in the final creation payload.
- **Color Association:** Users upload images and then associate them with specific color variants (Nomenclatures).

### Step 3: Variant Matrix (The Table)
Wildberries manages variants using a **Tabular Grid** rather than nested forms.
- **Rows:** Each row is a unique combination of `Color + Size`.
- **Columns:** Barcode, Price, Stock, Weight, Dimensions (L/W/H).
- **UX Tip:** "Fill Down" functionality (dragging a value from one cell to many) is a power-user feature in WB that makes bulk pricing/stocking very fast.

### Step 4: Characteristics (Attributes)
- **UX:** Dynamic form fields based on the Category Blueprint.
- **Logic:** Map over the blueprint entries (e.g., "Material", "Country of Origin") and render appropriate inputs (text, select).

## 3. Design Aesthetics to Borrow
- **Clean Grids:** Use of tables with clear headers for variant management.
- **Progress Indicators:** A vertical or horizontal stepper to show current progress.
- **Sticky Actions:** A "Create Product" or "Next Step" button that remains visible (sticky) as the user scrolls.
- **Validation Feedback:** Immediate error messaging if a barcode is invalid or a mandatory attribute is missing.

## 4. Proposed Radolfa Frontend Architecture
- **State Management:** A centralized store (Zustand or similar) to hold the multi-variant state before final submission.
- **Component Hierarchy:**
    - `ProductCreationWizard` (Stepper)
    - `BasicInfoForm`
    - `MediaUploader` (with Color grouping)
    - `VariantTable` (The matrix)
    - `AttributeForm` (Dynamic from Blueprint)

## 5. Summary Table of logic
| Feature | Implementation Logic |
|---|---|
| **Image Upload** | Call `/admin/images/upload` immediately on drop; store URLs. |
| **Attributes** | Fetch `/categories/{id}/blueprint` after Step 1. |
| **Variants** | Generate a row for every `Color x Size` combination. |
| **Final Save** | Send the full nested JSON to `POST /api/v1/admin/products`. |
