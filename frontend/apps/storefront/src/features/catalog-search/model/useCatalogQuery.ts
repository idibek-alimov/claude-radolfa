"use client";

import { useCallback, useMemo } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useInfiniteQuery } from "@tanstack/react-query";
import { fetchCatalog } from "@/entities/product";
import type { CatalogCriteria, ListingSort } from "@/entities/product";

const SORT_VALUES: readonly ListingSort[] = [
  "POPULAR",
  "CHEAPEST",
  "DEAREST",
  "RATING",
  "NEWEST",
  "BIGGEST_DISCOUNT",
];

function isListingSort(value: string | null): value is ListingSort {
  return !!value && (SORT_VALUES as readonly string[]).includes(value);
}

function parseNumber(value: string | null): number | undefined {
  if (value == null || value === "") return undefined;
  const n = Number(value);
  return Number.isFinite(n) ? n : undefined;
}

function parseCriteria(
  searchParams: URLSearchParams,
  categorySlug?: string
): CatalogCriteria {
  const q = searchParams.get("q")?.trim() || undefined;
  const sortParam = searchParams.get("sort");
  const pageParam = parseNumber(searchParams.get("page"));
  const colorKeys = searchParams.getAll("color");
  const brandIds = searchParams
    .getAll("brand")
    .map(Number)
    .filter((n) => Number.isFinite(n));

  return {
    q,
    categorySlug,
    sort: isListingSort(sortParam) ? sortParam : "POPULAR",
    page: pageParam && pageParam > 0 ? Math.floor(pageParam) : 1,
    priceMin: parseNumber(searchParams.get("priceMin")),
    priceMax: parseNumber(searchParams.get("priceMax")),
    minDiscount: parseNumber(searchParams.get("minDiscount")),
    inStock: searchParams.get("inStock") === "1" || undefined,
    colorKeys: colorKeys.length ? colorKeys : undefined,
    brandIds: brandIds.length ? brandIds : undefined,
  };
}

/** URL params this hook owns — written back via router.replace. */
type UrlUpdates = Partial<{
  sort: ListingSort | undefined;
  page: number | undefined;
  priceMin: number | undefined;
  priceMax: number | undefined;
  minDiscount: number | undefined;
  inStock: boolean | undefined;
  color: string[] | undefined;
  brand: number[] | undefined;
}>;

export interface UseCatalogQueryOptions {
  /** Category slug from the route (`/categories/[slug]/products`). Undefined on `/search`. */
  categorySlug?: string;
}

/**
 * Reads/writes catalog filter, sort and page state to the URL (the source of
 * truth, per CLAUDE.md's server-side-everything rule) and drives the
 * paginated `/api/v1/listings/catalog` query.
 */
export function useCatalogQuery({ categorySlug }: UseCatalogQueryOptions = {}) {
  const router = useRouter();
  const searchParams = useSearchParams();

  const criteria = useMemo(
    () => parseCriteria(searchParams, categorySlug),
    [searchParams, categorySlug]
  );

  const updateUrl = useCallback(
    (updates: UrlUpdates, resetPage: boolean) => {
      const params = new URLSearchParams(searchParams.toString());

      if ("sort" in updates) {
        const sort = updates.sort;
        if (!sort || sort === "POPULAR") params.delete("sort");
        else params.set("sort", sort);
      }
      if ("priceMin" in updates) setOrDelete(params, "priceMin", updates.priceMin);
      if ("priceMax" in updates) setOrDelete(params, "priceMax", updates.priceMax);
      if ("minDiscount" in updates) setOrDelete(params, "minDiscount", updates.minDiscount);
      if ("inStock" in updates) {
        if (updates.inStock) params.set("inStock", "1");
        else params.delete("inStock");
      }
      if ("color" in updates) setOrDeleteAll(params, "color", updates.color);
      if ("brand" in updates) setOrDeleteAll(params, "brand", updates.brand);

      if (resetPage) params.delete("page");
      else if ("page" in updates) setOrDelete(params, "page", updates.page);

      const qs = params.toString();
      router.replace(qs ? `?${qs}` : "?", { scroll: false });
    },
    [router, searchParams]
  );

  const setSort = useCallback(
    (sort: ListingSort) => updateUrl({ sort }, true),
    [updateUrl]
  );

  const setFilters = useCallback(
    (filters: Partial<CatalogCriteria>) => {
      const updates: UrlUpdates = {};
      if ("priceMin" in filters) updates.priceMin = filters.priceMin;
      if ("priceMax" in filters) updates.priceMax = filters.priceMax;
      if ("minDiscount" in filters) updates.minDiscount = filters.minDiscount;
      if ("inStock" in filters) updates.inStock = filters.inStock;
      if ("colorKeys" in filters) updates.color = filters.colorKeys;
      if ("brandIds" in filters) updates.brand = filters.brandIds;
      if ("sort" in filters) updates.sort = filters.sort;
      updateUrl(updates, true);
    },
    [updateUrl]
  );

  const setPage = useCallback(
    (page: number) => updateUrl({ page }, false),
    [updateUrl]
  );

  const reset = useCallback(() => {
    const params = new URLSearchParams();
    const q = searchParams.get("q");
    if (q) params.set("q", q);
    const qs = params.toString();
    router.replace(qs ? `?${qs}` : "?", { scroll: false });
  }, [router, searchParams]);

  const queryKeyCriteria = useMemo(() => {
    const { page: _page, ...rest } = criteria;
    return rest;
  }, [criteria]);

  const enabled = categorySlug ? true : !!criteria.q;

  const { data, fetchNextPage, hasNextPage, isLoading, isFetchingNextPage } =
    useInfiniteQuery({
      queryKey: ["catalog", queryKeyCriteria],
      queryFn: ({ pageParam }) => fetchCatalog({ ...criteria, page: pageParam }),
      initialPageParam: criteria.page ?? 1,
      getNextPageParam: (lastPage, allPages) =>
        lastPage.page.last ? undefined : (criteria.page ?? 1) + allPages.length,
      enabled,
    });

  const listings = useMemo(
    () => data?.pages.flatMap((p) => p.page.content) ?? [],
    [data]
  );
  const totalCount = data?.pages[0]?.page.totalElements ?? 0;
  const facets = data?.pages[0]?.facets;

  return {
    criteria,
    listings,
    totalCount,
    facets,
    hasMore: hasNextPage,
    fetchNextPage,
    isLoading,
    isFetchingNextPage,
    setSort,
    setFilters,
    setPage,
    reset,
  };
}

function setOrDelete(
  params: URLSearchParams,
  key: string,
  value: number | undefined
) {
  if (value == null) params.delete(key);
  else params.set(key, String(value));
}

function setOrDeleteAll(
  params: URLSearchParams,
  key: string,
  values: (string | number)[] | undefined
) {
  params.delete(key);
  if (values?.length) {
    for (const v of values) params.append(key, String(v));
  }
}
