"use client";

import { useState } from "react";
import { useInfiniteQuery } from "@tanstack/react-query";
import { ProductGrid, fetchProducts } from "@/widgets/ProductList";
import { SearchBar } from "@/features/search";
import type { SearchParams } from "@/features/search";

const PAGE_LIMIT = 12;

/**
 * Client shell for the /products catalog page.
 *
 * - Paginated list via useInfiniteQuery.
 * - SearchBar is rendered but the backend search endpoint is not yet live;
 *   submitting a search logs the query to the console as a placeholder.
 */
export default function CatalogSection() {
  const [searchQuery, setSearchQuery] = useState<string>("");

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isLoading,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ["products", "catalog", searchQuery],
    queryFn: async ({ pageParam = 1 }) => {
      const result = await fetchProducts(pageParam, PAGE_LIMIT);
      return result;
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage.hasMore) return undefined;
      return allPages.length + 1;
    },
  });

  // Flatten all pages into a single product array.
  const products = data?.pages.flatMap((page) => page.products) ?? [];

  const handleSearch = (params: SearchParams) => {
    // Backend search endpoint (Elasticsearch adapter) is not yet wired.
    // Log the incoming query so the integration point is visible during dev.
    console.log(
      "[CatalogSection] search submitted — backend endpoint not yet live.",
      { query: params.query }
    );
    setSearchQuery(params.query);
  };

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h2 className="text-2xl font-bold text-gray-900 mb-6">All Products</h2>

      {/* Search bar lives at the top of the catalog. */}
      <div className="mb-8">
        <SearchBar onSearch={handleSearch} />
      </div>

      <ProductGrid
        products={products}
        loading={isLoading || isFetchingNextPage}
        hasMore={hasNextPage}
        onLoadMore={fetchNextPage}
      />
    </section>
  );
}
