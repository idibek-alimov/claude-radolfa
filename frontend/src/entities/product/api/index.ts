import { apiClient } from "@/shared/api";
import type {
  ListingVariantDetail,
  PaginatedListings,
} from "@/entities/product/model/types";

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
