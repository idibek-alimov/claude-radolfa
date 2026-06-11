"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { Search } from "lucide-react";
import { CatalogView } from "@/widgets/CatalogView";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { useTranslations } from "next-intl";

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
  const t = useTranslations("search");
  const searchParams = useSearchParams();
  const query = searchParams.get("q")?.trim() || "";

  if (!query) {
    return (
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="mt-24 flex flex-col items-center text-center">
          <div className="flex items-center justify-center h-20 w-20 rounded-2xl bg-muted/60 mb-6">
            <Search className="h-9 w-9 text-muted-foreground/50" />
          </div>
          <h1 className="text-2xl font-semibold text-foreground">
            {t("startTitle")}
          </h1>
          <p className="mt-2 text-sm text-muted-foreground max-w-xs">
            {t("startDescription")}
          </p>
        </div>
      </div>
    );
  }

  return <CatalogView mode="search" query={query} />;
}
