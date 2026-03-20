import { apiClient } from "@/shared/api";
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
    params.size ?? 12
  );
  return result;
}

export interface ReindexResult {
  indexed: number;
  errorCount: number;
  message: string;
}

/** Trigger a full Elasticsearch reindex (ADMIN only). */
export async function reindexSearch(): Promise<ReindexResult> {
  const { data } = await apiClient.post<ReindexResult>("/api/v1/search/reindex");
  return data;
}
