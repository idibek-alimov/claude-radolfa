"use client";

import { useState } from "react";
import Link from "next/link";
import { useInfiniteQuery } from "@tanstack/react-query";
import { SlidersHorizontal, ArrowUpDown, Check } from "lucide-react";
import { ProductGrid, fetchListings } from "@/widgets/ProductList";
import { SearchBar } from "@/features/search";
import type { SearchParams } from "@/features/search";
import type { ListingSort } from "@/entities/product/api";
import {
  Breadcrumb,
  BreadcrumbList,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/shared/ui/breadcrumb";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/utils";

const PAGE_LIMIT = 12;

const SORT_OPTIONS: { value: ListingSort; label: string }[] = [
  { value: "default", label: "Default" },
  { value: "price_asc", label: "Price: Low → High" },
  { value: "price_desc", label: "Price: High → Low" },
  { value: "newest", label: "Newest" },
];

export default function CatalogSection() {
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [sort, setSort] = useState<ListingSort>("default");
  const [inStock, setInStock] = useState(false);

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isLoading,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ["listings", "catalog", searchQuery, sort, inStock],
    queryFn: async ({ pageParam = 1 }) => {
      return fetchListings(pageParam as number, PAGE_LIMIT, { sort, inStock });
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage.hasMore) return undefined;
      return allPages.length + 1;
    },
  });

  const listings = data?.pages.flatMap((page) => page.items) ?? [];
  const totalCount = data?.pages[0]?.totalElements ?? 0;

  const handleSearch = (params: SearchParams) => {
    setSearchQuery(params.query);
  };

  const activeSortLabel = SORT_OPTIONS.find((o) => o.value === sort)?.label ?? "Default";

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
            <BreadcrumbPage>All Products</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      {/* Header row */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-foreground">
            All Products
          </h1>
          {!isLoading && totalCount > 0 && (
            <p className="text-sm text-muted-foreground mt-1">
              {totalCount} products available
            </p>
          )}
        </div>
        <SearchBar onSearch={handleSearch} />
      </div>

      {/* Filter bar */}
      <div className="flex flex-wrap items-center gap-2 mb-8 pb-4 border-b">
        {/* Sort dropdown */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm" className="gap-2">
              <ArrowUpDown className="h-3.5 w-3.5" />
              {activeSortLabel}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start">
            {SORT_OPTIONS.map((option) => (
              <DropdownMenuItem
                key={option.value}
                onClick={() => setSort(option.value)}
                className="gap-2"
              >
                <Check
                  className={cn(
                    "h-3.5 w-3.5",
                    sort === option.value ? "opacity-100" : "opacity-0"
                  )}
                />
                {option.label}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>

        {/* In-stock toggle */}
        <Button
          variant={inStock ? "default" : "outline"}
          size="sm"
          className="gap-2"
          onClick={() => setInStock((v) => !v)}
        >
          <SlidersHorizontal className="h-3.5 w-3.5" />
          In Stock Only
        </Button>

        {/* Active filter chips */}
        {(sort !== "default" || inStock) && (
          <Button
            variant="ghost"
            size="sm"
            className="text-muted-foreground hover:text-foreground ml-auto"
            onClick={() => { setSort("default"); setInStock(false); }}
          >
            Clear filters
          </Button>
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
