import { searchListings } from "@/entities/product";
import type { SearchParams, SearchResult } from "@/features/search";

/**
 * Hit the backend listing search endpoint.
 * Elasticsearch adapter on the backend with SQL fallback.
 */
export async function searchProducts(
  params: SearchParams
): Promise<SearchResult> {
  const result = await searchListings(
    params.query,
    params.page ?? 1,
    params.limit ?? 12
  );
  return result;
}
