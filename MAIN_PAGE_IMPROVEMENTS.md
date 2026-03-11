# Main Page UI Improvements Plan

Inspired by Amazon, Noon, Wildberries patterns. All changes use existing `ListingVariant` data — no backend changes needed.

## Tasks

### 1. Product Card Enrichment (highest impact) — DONE
- [x] **Image swap on hover**: On hover, fades to `images[1]` using opacity transition (replaces old zoom). Uses `useState` for hover state + two stacked `<Image>` elements with `opacity-0`/`opacity-100` crossfade.
- [x] **Color dot**: Replaced `<Badge>` with a pill containing a colored circle (`colorHexCode`) + text label. White/blur backdrop for readability over images.
- [x] **Category label**: Added tiny uppercase muted text above product name showing `listing.category`.
- [x] **Featured/TopSelling badge**: `topSelling` items get a "Popular" badge (top-left) with a `Flame` icon from lucide-react. Added `popular` key to all 3 locale files (en/ru/tj).
- [x] **Smarter stock display**: Hidden when stock is plentiful. Shows orange "Only X left" when ≤5 (`LOW_STOCK_THRESHOLD`). Out-of-stock gets a dark overlay on the image + red badge. Added `lowStock` key to all 3 locale files.
- [x] **Price styling**: Kept bold primary styling. Removed description line (was taking space, rarely set). Price row is now always single-line `flex items-center justify-between`.
- [x] **Tighter card padding**: Reduced from `p-2.5 sm:p-4` to `p-2 sm:p-3`. Reduced gaps from `gap-1 sm:gap-2` to `gap-0.5 sm:gap-1`.

### 2. Category Quick-Filters — DONE
- [x] **CategoryFilter component** (`widgets/ProductList/ui/CategoryFilter.tsx`): Horizontal scrollable row of rounded pills. "All" pill + one pill per top-level category from `fetchCategoryTree()` (30min stale time, same as MegaMenu). Left/right chevron scroll buttons appear on hover with gradient fades.
- [x] **CatalogSection rewired**: Selected category state drives the infinite query — switches between `fetchListings` (all) and `fetchCategoryProducts` (filtered). Query key includes `selectedCategory` so React Query auto-refetches on change.
- [x] **Breadcrumb removed** from homepage (was redundant Home > Products). Header simplified to single row: title left, count right.
- [x] **scrollbar-hide utility** added to `globals.css` for clean horizontal scroll without visible scrollbar.

### 3. Grid Density — DONE
- [x] **Tighter gaps**: Changed from `gap-3 sm:gap-5` to `gap-2 sm:gap-3`.
- [x] **Earlier 3rd column**: Grid now hits 3 columns at `sm` (640px) instead of `lg` (1024px) — shows more products above the fold on tablets.
- [x] **5th column on 2xl**: Added `2xl:grid-cols-5` so wide monitors fill the screen with products.
- [x] **Wider container**: CatalogSection bumped from `max-w-7xl` (1280px) to `max-w-[1600px]` to give the 5th column room.
- [x] **Skeleton count**: Increased from 8 to 10 to fill the wider grid during loading.

### 4. Clean Up Homepage Header
- [ ] Remove breadcrumb from homepage (redundant: Home > Products on the landing page itself)
- [ ] Simplify "All Products" heading — make it subtler or remove entirely

### 5. Visual Rhythm Breakers
- [ ] Insert a subtle accent row or section break every 8-12 cards
- [ ] Could be a category highlight, promotional strip, or a "Trending" horizontal scroll
