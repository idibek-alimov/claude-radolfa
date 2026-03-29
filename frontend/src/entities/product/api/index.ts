import { apiClient } from "@/shared/api";
import type { PaginatedResponse } from "@/shared/api/types";
import type {
  Sku,
  ListingVariant,
  ListingVariantDetail,
  HomeSection,
  CollectionPage,
  CategoryTree,
} from "@/entities/product/model/types";

export interface UpdateListingRequest {
  webDescription?: string;
}

export interface UpdateDimensionsRequest {
  weightKg?: number;
  widthCm?: number;
  heightCm?: number;
  depthCm?: number;
}

export interface ImageUploadResponse {
  images: string[];
}

// ── API functions ─────────────────────────────────────────────────

/** Paginated listing grid. Backend is 1-based. */
export async function fetchListings(
  page: number = 1,
  size: number = 12
): Promise<PaginatedResponse<ListingVariant>> {
  const { data } = await apiClient.get<PaginatedResponse<ListingVariant>>(
    "/api/v1/listings",
    { params: { page, size } }
  );
  return data;
}

/** Full detail for a single listing variant (slug-based). */
export async function fetchListingBySlug(
  slug: string
): Promise<ListingVariantDetail> {
  const { data } = await apiClient.get<ListingVariantDetail>(
    `/api/v1/listings/${slug}`
  );
  return data;
}

/** Full-text fuzzy search (ES with SQL fallback). */
export async function searchListings(
  q: string,
  page: number = 1,
  size: number = 12
): Promise<PaginatedResponse<ListingVariant>> {
  const { data } = await apiClient.get<PaginatedResponse<ListingVariant>>(
    "/api/v1/listings/search",
    { params: { q, page, size } }
  );
  return data;
}

/** Autocomplete suggestions for the search box. */
export async function fetchAutocomplete(
  q: string,
  limit: number = 5
): Promise<string[]> {
  const { data } = await apiClient.get<string[]>(
    "/api/v1/listings/autocomplete",
    { params: { q, limit } }
  );
  return data;
}

/** Homepage collection sections. */
export async function fetchHomeCollections(): Promise<HomeSection[]> {
  const { data } = await apiClient.get<HomeSection[]>(
    "/api/v1/home/collections"
  );
  return data ?? [];
}

/** Single collection page. */
export async function fetchCollectionPage(
  key: string
): Promise<CollectionPage> {
  const { data } = await apiClient.get<CollectionPage>(
    `/api/v1/home/collections/${key}`
  );
  return data;
}

/** Update listing enrichment fields (manager only). */
export async function updateListing(
  slug: string,
  data: UpdateListingRequest
): Promise<void> {
  await apiClient.put(`/api/v1/listings/${slug}`, data);
}

/** PATCH /api/v1/listings/{slug}/dimensions — MANAGER+ */
export async function updateListingDimensions(
  slug: string,
  data: UpdateDimensionsRequest
): Promise<void> {
  await apiClient.patch(`/api/v1/listings/${slug}/dimensions`, data);
}

/** Upload images to a listing (manager only). Field name must be "files". */
export async function uploadListingImage(
  slug: string,
  file: File
): Promise<ImageUploadResponse> {
  if (!file.type.startsWith("image/")) {
    throw new Error("File must be an image");
  }
  const form = new FormData();
  form.append("files", file);
  const { data } = await apiClient.post<ImageUploadResponse>(
    `/api/v1/listings/${slug}/images`,
    form,
    { headers: { "Content-Type": "multipart/form-data" } }
  );
  return data;
}

/** Products filtered by category slug (includes descendants). */
export async function fetchCategoryProducts(
  slug: string,
  page: number = 1,
  size: number = 12,
): Promise<PaginatedResponse<ListingVariant>> {
  const { data } = await apiClient.get<PaginatedResponse<ListingVariant>>(
    `/api/v1/categories/${slug}/products`,
    { params: { page, size } },
  );
  return data;
}

/** Category tree for navigation. */
export async function fetchCategoryTree(): Promise<CategoryTree[]> {
  const { data } = await apiClient.get<CategoryTree[]>("/api/v1/categories");
  return data;
}

/** Remove an image from a listing (manager only). */
export async function removeListingImage(
  slug: string,
  imageUrl: string
): Promise<void> {
  await apiClient.delete(`/api/v1/listings/${slug}/images`, {
    data: { imageUrl },
  });
}
