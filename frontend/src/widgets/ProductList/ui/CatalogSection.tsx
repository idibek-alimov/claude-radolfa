"use client";

import { useState } from "react";
import { useInfiniteQuery } from "@tanstack/react-query";
import { ProductGrid, fetchListings } from "@/widgets/ProductList";
import { fetchCategoryProducts } from "@/entities/product";
import CategoryFilter from "./CategoryFilter";
import { useTranslations } from "next-intl";

const PAGE_LIMIT = 12;

export default function CatalogSection() {
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const t = useTranslations("common");

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isLoading,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ["listings", "catalog", selectedCategory],
    queryFn: async ({ pageParam = 1 }) => {
      if (selectedCategory) {
        return fetchCategoryProducts(selectedCategory, pageParam, PAGE_LIMIT);
      }
      return fetchListings(pageParam, PAGE_LIMIT);
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage.hasMore) return undefined;
      return allPages.length + 1;
    },
  });

  const listings = data?.pages.flatMap((page) => page.items) ?? [];
  const totalCount = data?.pages[0]?.totalElements ?? 0;

  return (
    <section className="max-w-[1600px] mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-10">
      {/* Category pills + count */}
      <div className="flex items-center justify-between gap-4 mb-5">
        <div className="flex-1 min-w-0">
          <CategoryFilter
            selected={selectedCategory}
            onSelect={setSelectedCategory}
          />
        </div>
        {!isLoading && totalCount > 0 && (
          <span className="text-xs sm:text-sm text-muted-foreground whitespace-nowrap shrink-0">
            {t("productsAvailable", { count: totalCount })}
          </span>
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
