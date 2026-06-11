"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Search } from "lucide-react";
import {
  ProductCard,
  ProductCardSkeleton,
  type CatalogCriteria,
} from "@/entities/product";
import {
  useCatalogQuery,
  SortPills,
  FilterPanel,
  MobileFilterBar,
  CatalogSheet,
  CatalogPagination,
} from "@/features/catalog-search";
import { useTranslations } from "next-intl";

const PRODUCT_CODE_RE = /^RD-\d{5,}$/i;
const SKELETON_COUNT = 8;

interface CatalogViewProps {
  mode: "search" | "category" | "browse";
  query?: string;
  categorySlug?: string;
  categoryName?: string;
}

function countActiveFilters(criteria: CatalogCriteria): number {
  let count = 0;
  if (criteria.priceMin != null) count++;
  if (criteria.priceMax != null) count++;
  if (criteria.minDiscount != null) count++;
  if (criteria.inStock) count++;
  if (criteria.colorKeys?.length) count++;
  if (criteria.brandIds?.length) count++;
  return count;
}

export function CatalogView({ mode, query, categorySlug, categoryName }: CatalogViewProps) {
  const router = useRouter();
  const tc = useTranslations("common");
  const {
    criteria,
    listings,
    totalCount,
    facets,
    hasMore,
    fetchNextPage,
    isLoading,
    isFetchingNextPage,
    setSort,
    setFilters,
    setPage,
    setCategory,
    reset,
  } = useCatalogQuery({ categorySlug, mode });

  const [sheet, setSheet] = useState<"filter" | "sort" | null>(null);
  const [draft, setDraft] = useState<CatalogCriteria>(criteria);

  const isProductCode = mode === "search" && !!query && PRODUCT_CODE_RE.test(query);

  useEffect(() => {
    if (isProductCode && !isLoading && listings.length === 1 && listings[0].slug) {
      router.replace(`/products/${listings[0].slug}`);
    }
  }, [isProductCode, isLoading, listings, router]);

  const onCategorySelect = (slug: string | null) => {
    if (mode === "category") {
      // On a category route, switching category navigates to that category's page.
      if (slug) router.push(`/categories/${slug}/products`);
      return;
    }
    // On /search and the default browse grid, category is just another in-place filter.
    setCategory(slug);
  };

  const openFilterSheet = () => {
    setDraft(criteria);
    setSheet("filter");
  };

  if (isProductCode && (isLoading || listings.length === 1)) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12">
        <div className="mt-24 flex flex-col items-center text-center gap-4">
          <ProductCardSkeleton />
        </div>
      </div>
    );
  }

  const title =
    mode === "search" ? query ?? "" : mode === "category" ? categoryName ?? "" : tc("allProducts");
  const filterCount = countActiveFilters(criteria);
  const showSkeletons = isLoading && listings.length === 0;
  const showEmpty = !isLoading && listings.length === 0;

  return (
    <div>
      <div className="max-w-[1440px] mx-auto px-4 md:px-6 pt-5">
        {mode === "category" ? (
          <div className="hidden md:block text-[12px] text-ink/55 mb-1.5">
            <Link href="/" className="hover:text-mag">
              Home
            </Link>{" "}
            <span className="opacity-50">/</span>{" "}
            <span className="text-ink/80 font-semibold">{categoryName}</span>
          </div>
        ) : mode === "search" ? (
          <div className="hidden md:block text-[12px] text-ink/55 mb-1.5">
            <span className="text-ink/80 font-semibold">Search results</span>
          </div>
        ) : null}

        <div className="flex items-end justify-between gap-4 flex-wrap">
          <div>
            <h1 className="text-[22px] md:text-[26px] font-black leading-none">{title}</h1>
            <div className="text-[12px] md:text-[13px] text-ink/55 mt-1 md:mt-1.5">
              <span className="font-bold text-ink">{totalCount}</span> items found
            </div>
          </div>
          <div className="hidden md:flex">
            <SortPills value={criteria.sort ?? "POPULAR"} onChange={setSort} variant="desktop" />
          </div>
        </div>

        <div className="md:hidden py-2.5">
          <SortPills value={criteria.sort ?? "POPULAR"} onChange={setSort} variant="mobile" />
        </div>
      </div>

      <div className="max-w-[1440px] mx-auto px-4 md:px-6 mt-4 grid grid-cols-1 md:grid-cols-12 gap-6 pb-24 md:pb-10">
        <aside className="hidden md:block md:col-span-3 xl:col-span-2 text-[13px]">
          <div className="rounded-2xl border border-ink/8 p-4 space-y-5 bg-soft">
            <FilterPanel
              facets={facets}
              value={criteria}
              onChange={setFilters}
              onReset={reset}
              selectedCategorySlug={criteria.categorySlug ?? null}
              onCategorySelect={onCategorySelect}
            />
          </div>
        </aside>

        <main className="md:col-span-9 xl:col-span-10">
          {showEmpty ? (
            <div className="border border-dashed rounded-xl p-12 flex flex-col items-center text-center gap-3">
              <Search className="h-10 w-10 text-muted-foreground/40" />
              <p className="text-sm text-muted-foreground">No products found.</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-3 md:gap-4 md:grid-cols-3 xl:grid-cols-4">
              {showSkeletons
                ? Array.from({ length: SKELETON_COUNT }).map((_, i) => (
                    <ProductCardSkeleton key={i} />
                  ))
                : listings.map((listing) => (
                    <ProductCard key={listing.variantId} listing={listing} />
                  ))}
            </div>
          )}

          <CatalogPagination
            hasMore={hasMore}
            isFetchingNextPage={isFetchingNextPage}
            onShowMore={fetchNextPage}
            page={criteria.page ?? 1}
            totalPages={1}
            onPageChange={setPage}
            showPager={false}
          />
        </main>
      </div>

      <div className="md:hidden">
        <MobileFilterBar
          filterCount={filterCount}
          onOpenFilters={openFilterSheet}
          onOpenSort={() => setSheet("sort")}
        />
        <CatalogSheet
          open={sheet !== null}
          onOpenChange={(open) => setSheet(open ? sheet : null)}
          mode={sheet ?? "filter"}
          resultCount={totalCount}
          facets={facets}
          draftValue={draft}
          onDraftChange={(patch) => setDraft((prev) => ({ ...prev, ...patch }))}
          onReset={reset}
          selectedCategorySlug={categorySlug ?? null}
          onCategorySelect={onCategorySelect}
          onApplyFilters={() => setFilters(draft)}
          sortValue={criteria.sort ?? "POPULAR"}
          onSortChange={setSort}
        />
      </div>
    </div>
  );
}
