"use client";

import { useState } from "react";
import Link from "next/link";
import { useInfiniteQuery } from "@tanstack/react-query";
import { ProductGrid, fetchListings } from "@/widgets/ProductList";
import { SearchBar } from "@/features/search";
import type { SearchParams } from "@/features/search";
import {
  Breadcrumb,
  BreadcrumbList,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/shared/ui/breadcrumb";

const PAGE_LIMIT = 12;

export default function CatalogSection() {
  const [searchQuery, setSearchQuery] = useState<string>("");

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isLoading,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ["listings", "catalog", searchQuery],
    queryFn: async ({ pageParam = 1 }) => {
      const result = await fetchListings(pageParam, PAGE_LIMIT);
      return result;
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage.hasMore) return undefined;
      return allPages.length + 1;
    },
  });

  const listings = data?.pages.flatMap((page) => page.items) ?? [];
  const totalCount = data?.pages[0]?.totalElements ?? 0;

  const handleSearch = (params: SearchParams) => {
    setSearchQuery(params.query);
  };

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Breadcrumb */}
      <Breadcrumb className="mb-6">
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href="/">Home</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage>Products</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      {/* Header row */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-foreground">
            All Products
          </h1>
          {!isLoading && totalCount > 0 && (
            <p className="text-sm text-muted-foreground mt-1">
              {totalCount} products available
            </p>
          )}
        </div>
        <SearchBar onSearch={handleSearch} />
      </div>

      <ProductGrid
        listings={listings}
        loading={isLoading || isFetchingNextPage}
        hasMore={hasNextPage}
        onLoadMore={fetchNextPage}
      />
    </section>
  );
}
