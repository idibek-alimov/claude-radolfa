"use client";

import { Suspense, use } from "react";
import { CatalogView } from "@/widgets/CatalogView";
import { Skeleton } from "@radolfa/shared/ui/skeleton";

function CategoryFallback() {
  return (
    <div className="max-w-[1440px] mx-auto px-4 md:px-6 py-10">
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

export default function CategoryProductsPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = use(params);
  const title = slug.replace(/-/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());

  return (
    <Suspense fallback={<CategoryFallback />}>
      <CatalogView mode="category" categorySlug={slug} categoryName={title} />
    </Suspense>
  );
}
