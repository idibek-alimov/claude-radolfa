"use client";

import Link from "next/link";
import { useInfiniteQuery } from "@tanstack/react-query";
import { ProductGrid } from "@/widgets/ProductList";
import { fetchCategoryProducts } from "@/entities/product";
import {
  Breadcrumb,
  BreadcrumbList,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/shared/ui/breadcrumb";

const PAGE_LIMIT = 12;

export default function CategoryProductsPage({
  params,
}: {
  params: { slug: string };
}) {
  const { slug } = params;

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isLoading,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ["category-products", slug],
    queryFn: async ({ pageParam = 1 }) =>
      fetchCategoryProducts(slug, pageParam, PAGE_LIMIT),
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage.hasMore) return undefined;
      return allPages.length + 1;
    },
  });

  const listings = data?.pages.flatMap((p) => p.items) ?? [];
  const totalCount = data?.pages[0]?.totalElements ?? 0;
  const title = slug.replace(/-/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
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
