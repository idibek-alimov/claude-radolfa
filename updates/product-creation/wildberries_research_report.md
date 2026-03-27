# Wildberries Product Creation Research Report

Based on an analysis of the Wildberries (WB Partners) seller portal and API, here is a breakdown of how they structure product creation from both a frontend UX and backend data perspective.

This research can help us design our own product creation flow for the Radolfa system.

## 1. Backend Data Structure (The WB API Model)

Wildberries uses a highly structured, hierarchical data model separated into logical components:

*   **Nomenclature (The Base Product):** This represents the root generic item (similar to our `ProductBase`). It holds the primary identifier, the brand, and the overarching **Category / Subject**.
*   **Characteristics (Attributes by Category):** In WB, attributes are strongly tied to the category. When a vendor selects a "Subject" (Category), the system dynamically queries the API (`/content/v2/object/charcs/{subjectId}`) to fetch all mandatory and optional characteristics for that specific subject. 
*   **Colors (The Variant Level):** WB treats different colors of the same nomenclature as distinct variants. They have a strict global dictionary of colors (`/content/v2/directory/colors`). Each color variant gets its own set of images and its own localized description.
*   **Sizes & Barcodes (The SKU Level):** Inside each color variant, vendors define the available sizes. Every size must have a generated barcode (`/content/v2/barcodes`). Price and stock are mapped deeply down to the SKU (size) level, although price overrides exist globally.

## 2. Frontend UX Flow (WB Partners Portal)

The vendor experience on the WB Partners portal is designed to prevent bad data while handling complex hierarchies. The typical flow is:

1.  **Category First:** Vendors *must* select the product category (Subject) first. This is critical because the rest of the form (especially attributes) is dynamically generated based on this choice.
2.  **Base Information:** Entering the Brand, Title, and Description.
3.  **Variant & Attribute Expansion (The Big Form):** 
    *   The frontend displays a dynamic form for the characteristics required by the chosen category (e.g., "Screen Size" for TVs, "Material" for T-shirts).
    *   Vendors add "Colors". For each color added, a new section or tab appears.
4.  **SKU Matrix Generation:** Within a specific color section, the vendor defines the applicable sizes. The frontend immediately asks for the Barcode and Stock for each size.
5.  **Media Upload (Decoupled but integrated):** 
    *   Images are attached to the **Color Variant**, not the base product.
    *   While creating the product, the image upload is usually a dedicated drag-and-drop zone per color. 
    *   As you suggested for our system, WB effectively handles images via separate asynchronous API calls in the background while the user is filling out the form, stitching the final references together upon saving.

## 3. Key Takeaways & Recommendations for Our System

If we want to mimic the best parts of the Wildberries approach:

1.  **Category-Driven Attributes (Frontend logic):** In the frontend, the user should select the Category first. This allows the frontend to know exactly what `attributes` to ask for in the UI. 
2.  **Variant List Structure (Already aligned):** Our newly rewritten backend prompt (having a list of `ListingVariantCreationDto` inside the `CreateProductRequestDto`) aligns perfectly with the WB model. A single request can encompass the Base Product and an array of Color Variants.
3.  **Images as a Separate Step / Endpoint:** Your idea to have a separate endpoint for images is exactly how large marketplaces handle it. The frontend uploads images to a dedicated endpoint asynchronously, receives URIs back, and passes those URIs in the `images` array of the JSON payload when the vendor finally clicks "Save/Create Product".
4.  **Separate TopSelling/Featured Flags:** As you noted, WB doesn't ask for "Featured" or "Top Selling" during creation. Those are dynamic platform metrics or post-creation marketing toggles. Leaving them out of creation is the correct call.

## Next Steps
Please review this report. Once you decide how we should adjust our structure (e.g., removing `topSelling`/`featured` from the creation prompt, keeping the multiple variant structure, and decoupling the image endpoint), I will update the `claude_prompt.md` to perfectly match your final architectural decision!
