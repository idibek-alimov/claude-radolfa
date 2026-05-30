"use client";

import { use } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
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

export default function CollectionViewAllPage({
  params,
}: {
  params: Promise<{ key: string }>;
}) {
  const { key } = use(params);

  const { data, isLoading } = useQuery({
    queryKey: ["collection", key],
    queryFn: () => fetchCollectionPage(key),
  });

  const title = data?.title ?? "Collection";
  const listings = data?.listings ?? [];

  return (
    <section className="max-w-[1600px] mx-auto px-4 sm:px-6 lg:px-8 py-10">
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
        {!isLoading && listings.length > 0 && (
          <p className="text-sm text-muted-foreground mt-1">
            {listings.length} products
          </p>
        )}
      </div>

      <ProductGrid
        listings={listings}
        loading={isLoading}
        hasMore={false}
        onLoadMore={undefined}
      />
    </section>
  );
}
