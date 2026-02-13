"use client";

import Link from "next/link";
import { useInfiniteQuery } from "@tanstack/react-query";
import { ProductGrid } from "@/widgets/ProductList";
import { fetchCollectionPage } from "@/entities/product";
import {
  Breadcrumb,
  BreadcrumbList,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/shared/ui/breadcrumb";

const PAGE_LIMIT = 12;

export default function CollectionViewAllPage({
  params,
}: {
  params: { key: string };
}) {
  const { key } = params;

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isLoading,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ["collection", key],
    queryFn: async ({ pageParam = 1 }) => {
      const result = await fetchCollectionPage(key, pageParam, PAGE_LIMIT);
      return result;
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage.page.hasMore) return undefined;
      return allPages.length + 1;
    },
  });

  const title = data?.pages[0]?.title ?? "Collection";
  const listings = data?.pages.flatMap((p) => p.page.items) ?? [];
  const totalCount = data?.pages[0]?.page.totalElements ?? 0;

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
            <BreadcrumbPage>{title}</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl sm:text-3xl font-bold text-foreground">
          {title}
        </h1>
        {!isLoading && totalCount > 0 && (
          <p className="text-sm text-muted-foreground mt-1">
            {totalCount} products
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
