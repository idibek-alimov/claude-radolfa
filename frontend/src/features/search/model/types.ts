import type { ListingVariant } from "@/entities/product";

/** Parameters sent to the search API. */
export interface SearchParams {
  query: string;
  page?: number;
  size?: number;
}

/** Paginated response returned by the search API. */
export type SearchResult = import("@/shared/api").PaginatedResponse<ListingVariant>;
