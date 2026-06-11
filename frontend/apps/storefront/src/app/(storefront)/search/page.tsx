"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { CatalogView } from "@/widgets/CatalogView";
import { Skeleton } from "@radolfa/shared/ui/skeleton";

function SearchFallback() {
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

  if (!query) {
    return <CatalogView mode="browse" />;
  }

  return <CatalogView mode="search" query={query} />;
}
