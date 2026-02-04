import type { Product } from "@/entities/product";

/** Parameters sent to the search API. */
export interface SearchParams {
  query: string;
  page?: number;
  limit?: number;
}

/** Paginated response returned by the search API. */
export interface SearchResult {
  products: Product[];
  total: number;
  page: number;
}
