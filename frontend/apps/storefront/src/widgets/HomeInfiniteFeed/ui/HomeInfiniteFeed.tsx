"use client";

import { useTranslations } from "next-intl";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { ProductCard } from "@/entities/product";
import { useCatalogQuery, useInfiniteScrollTrigger } from "@/features/catalog-search";

const SKELETON_COUNT = 10;

export function HomeInfiniteFeed() {
  const tc = useTranslations("common");
  const { listings, hasMore, fetchNextPage, isLoading, isFetchingNextPage } = useCatalogQuery({
    mode: "browse",
  });

  const sentinelRef = useInfiniteScrollTrigger({
    hasMore,
    isLoading: isFetchingNextPage,
    onLoadMore: fetchNextPage,
  });

  if (!isLoading && listings.length === 0) {
    return null;
  }

  return (
    <section className="max-w-[1440px] mx-auto px-6 pt-10 pb-12">
      <div className="flex items-baseline justify-between mb-4">
        <h2 className="font-black text-3xl">{tc("allProducts")}</h2>
      </div>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-5 md:gap-4">
        {isLoading
          ? Array.from({ length: SKELETON_COUNT }).map((_, i) => (
              <Skeleton key={i} className="rounded-2xl aspect-square" />
            ))
          : listings.map((listing) => <ProductCard key={listing.variantId} listing={listing} />)}

        {isFetchingNextPage &&
          Array.from({ length: SKELETON_COUNT }).map((_, i) => (
            <Skeleton key={`next-${i}`} className="rounded-2xl aspect-square" />
          ))}
      </div>

      <div ref={sentinelRef} className="h-1" />
    </section>
  );
}
