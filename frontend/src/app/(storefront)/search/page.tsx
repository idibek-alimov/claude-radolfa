"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useInfiniteQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { searchListings } from "@/entities/product";
import { ProductGrid } from "@/widgets/ProductList";
import { Skeleton } from "@/shared/ui/skeleton";

const PAGE_LIMIT = 12;

function SearchFallback() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <Skeleton className="h-9 w-64 mb-2" />
      <Skeleton className="h-4 w-40 mb-8" />
      <div className="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3 sm:gap-5">
        {Array.from({ length: 8 }).map((_, i) => (
          <Skeleton key={i} className="h-72 w-full rounded-lg" />
        ))}
      </div>
    </div>
  );
}

export default function SearchPage() {
  return (
    <Suspense fallback={<SearchFallback />}>
      <SearchContent />
    </Suspense>
  );
}

function SearchContent() {
  const searchParams = useSearchParams();
  const query = searchParams.get("q")?.trim() || "";

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isLoading,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ["search", query],
    queryFn: async ({ pageParam = 1 }) =>
      searchListings(query, pageParam, PAGE_LIMIT),
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage.hasMore) return undefined;
      return allPages.length + 1;
    },
    enabled: query.length > 0,
  });

  const listings = data?.pages.flatMap((p) => p.items) ?? [];
  const totalCount = data?.pages[0]?.totalElements ?? 0;

  if (!query) {
    return (
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="mt-24 flex flex-col items-center text-center">
          <div className="flex items-center justify-center h-20 w-20 rounded-2xl bg-muted/60 mb-6">
            <Search className="h-9 w-9 text-muted-foreground/50" />
          </div>
          <h1 className="text-2xl font-semibold text-foreground">
            Start your search
          </h1>
          <p className="mt-2 text-sm text-muted-foreground max-w-xs">
            Use the search bar above to find products across the Radolfa
            catalog.
          </p>
        </div>
      </div>
    );
  }

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="mb-8">
        <h1 className="text-2xl sm:text-3xl font-semibold text-foreground tracking-tight">
          Results for{" "}
          <span className="text-primary">&ldquo;{query}&rdquo;</span>
        </h1>
        {!isLoading && totalCount > 0 && (
          <p className="mt-2 text-sm text-muted-foreground">
            {totalCount} products found
          </p>
        )}
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
