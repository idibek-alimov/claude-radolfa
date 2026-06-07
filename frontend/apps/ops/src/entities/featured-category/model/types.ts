/**
 * Admin-curated "Featured Category" entry — mirrors the backend
 * `FeaturedCategoryDto` (admin response). Powers the homepage
 * "Shop by Category" spotlight; the public read side enriches each
 * entry with live category data (see storefront `FeaturedCategory`).
 */
export interface FeaturedCategory {
  id: number;
  categoryId: number;
  imageUrl: string | null;
  title: string | null;
  subtitle: string | null;
  displayOrder: number;
  active: boolean;
}

/** Mirrors `FeaturedCategoryRequestDto` — the create/update payload. */
export interface FeaturedCategoryRequest {
  categoryId: number;
  imageUrl: string | null;
  title: string | null; // max 160 chars
  subtitle: string | null; // max 255 chars
  displayOrder: number; // >= 0
  active: boolean;
}
