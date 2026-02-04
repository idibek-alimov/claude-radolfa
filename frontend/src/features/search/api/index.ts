import { apiClient } from "@/shared/api";
import type { SearchParams, SearchResult } from "@/features/search";

/**
 * Hit the backend search endpoint.
 * Elasticsearch adapter on the backend side is not yet wired;
 * this call will 404 until that phase lands.
 */
export async function searchProducts(
  params: SearchParams
): Promise<SearchResult> {
  const { data } = await apiClient.get<SearchResult>(
    "/api/v1/products/search",
    { params }
  );
  return data;
}
