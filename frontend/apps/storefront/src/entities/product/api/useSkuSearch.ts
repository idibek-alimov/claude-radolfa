"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchListings, searchListings } from "./index";
import { useDebounce } from "@/shared/lib";

export function useSkuSearch(query: string, page: number) {
  const debouncedQuery = useDebounce(query.trim(), 300);
  const hasQuery = debouncedQuery.length >= 2;

  return useQuery({
    queryKey: ["sku-picker-search", debouncedQuery, page],
    queryFn: () =>
      hasQuery
        ? searchListings(debouncedQuery, page, 10)
        : fetchListings(page, 10),
    placeholderData: (prev) => prev,
  });
}
