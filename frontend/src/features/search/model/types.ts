import type { ListingVariant } from "@/entities/product";

/** Parameters sent to the search API. */
export interface SearchParams {
  query: string;
  page?: number;
  limit?: number;
}

/** Paginated response returned by the search API. */
export interface SearchResult {
  items: ListingVariant[];
  totalElements: number;
  page: number;
  hasMore: boolean;
}
