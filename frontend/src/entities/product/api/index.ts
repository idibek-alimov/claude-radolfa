import { apiClient } from "@/shared/api";
import type {
  ListingVariantDetail,
  PaginatedListings,
  ListingVariant,
  HomeSection,
} from "@/entities/product/model/types";

export interface UpdateListingRequest {
  webDescription?: string;
  topSelling?: boolean;
}

export interface ImageUploadResponse {
  images: string[];
}

/** Paginated listing grid (colour cards). */
export async function fetchListings(
  page: number = 1,
  limit: number = 12
): Promise<PaginatedListings> {
  const { data } = await apiClient.get<PaginatedListings>(
    "/api/v1/listings",
    { params: { page, limit } }
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
  limit: number = 12
): Promise<PaginatedListings> {
  const { data } = await apiClient.get<PaginatedListings>(
    "/api/v1/listings/search",
    { params: { q, page, limit } }
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

/** Homepage collection sections (Featured, New Arrivals, Deals). */
export async function fetchHomeCollections(): Promise<HomeSection[]> {
  const { data } = await apiClient.get<HomeSection[]>(
    "/api/v1/home/collections"
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

/** Upload an image to a listing (manager only). */
export async function uploadListingImage(
  slug: string,
  file: File
): Promise<void> {
  if (!file.type.startsWith("image/")) {
    throw new Error("File must be an image");
  }
  const form = new FormData();
  form.append("image", file);
  await apiClient.post(`/api/v1/listings/${slug}/images`, form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}

/** Remove an image from a listing (manager only). */
export async function removeListingImage(
  slug: string,
  imageUrl: string
): Promise<void> {
  // DELETE with body is tricky in some clients/proxies, but axios supports it via `data` config.
  await apiClient.delete(`/api/v1/listings/${slug}/images`, {
    data: { url: imageUrl },
  });
}
