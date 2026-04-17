"use client";

import { useState, useCallback, useRef, useEffect } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@/shared/ui/table";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { fetchDiscountedProducts, fetchDiscounts } from "../api";
import type { DiscountedProductFilters } from "../model/types";
import { useDynamicPageSize } from "@/shared/lib";
import { cn } from "@/shared/lib";
import {
  ChevronLeft,
  ChevronRight,
  Search,
  Package,
} from "lucide-react";

// ── Main component ────────────────────────────────────────────────────────────

export function DiscountedProductsTable() {
  const router = useRouter();
  const cardRef = useRef<HTMLDivElement>(null);
  const pageSize = useDynamicPageSize(cardRef, 49);

  const [filters, setFilters] = useState<DiscountedProductFilters>({
    page: 1,
    size: pageSize,
  });
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [minDelta, setMinDelta] = useState("");
  const [maxDelta, setMaxDelta] = useState("");
  const debounceRef = useRef<NodeJS.Timeout | undefined>(undefined);
  const deltaRef = useRef<NodeJS.Timeout | undefined>(undefined);

  useEffect(() => {
    setFilters((f) => ({ ...f, page: 1, size: pageSize }));
  }, [pageSize]);

  const handleSearchChange = useCallback((value: string) => {
    setSearch(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value.trim());
      setFilters((f) => ({ ...f, page: 1 }));
    }, 300);
  }, []);

  const applyDeltaFilter = useCallback((min: string, max: string) => {
    clearTimeout(deltaRef.current);
    deltaRef.current = setTimeout(() => {
      setFilters((f) => ({
        ...f,
        page: 1,
        minDeltaPercent: min !== "" ? Number(min) : undefined,
        maxDeltaPercent: max !== "" ? Number(max) : undefined,
      }));
    }, 400);
  }, []);

  // Load active campaigns for the campaign filter dropdown
  const { data: campaignData } = useQuery({
    queryKey: ["discounts-active-list"],
    queryFn: () => fetchDiscounts({ page: 1, size: 200, status: "active" }),
  });
  const activeCampaigns = campaignData?.content ?? [];

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["discounted-products", filters, debouncedSearch],
    queryFn: () =>
      fetchDiscountedProducts({
        ...filters,
        search: debouncedSearch || undefined,
      }),
    placeholderData: keepPreviousData,
  });

  const rows = data?.content ?? [];

  return (
    <div className="flex flex-col flex-1 min-h-0 gap-4">
      {/* Toolbar */}
      <div className="flex flex-wrap items-end gap-3">
        {/* Search */}
        <div className="relative min-w-[180px] flex-1 max-w-xs">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
          <Input
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder="Search by SKU or product…"
            className="pl-8 h-9 text-sm"
          />
        </div>

        {/* Campaign filter */}
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">Campaign</label>
          <Select
            value={filters.campaignId?.toString() ?? "all"}
            onValueChange={(v) =>
              setFilters((f) => ({
                ...f,
                page: 1,
                campaignId: v === "all" ? undefined : Number(v),
              }))
            }
          >
            <SelectTrigger className="h-9 w-[180px] text-sm">
              <SelectValue placeholder="All campaigns" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All campaigns</SelectItem>
              {activeCampaigns.map((c) => (
                <SelectItem key={c.id} value={c.id.toString()}>
                  <span className="flex items-center gap-1.5">
                    <span
                      className="inline-block w-2 h-2 rounded-full shrink-0"
                      style={{ backgroundColor: `#${c.colorHex}` }}
                    />
                    {c.title}
                  </span>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Δ% range */}
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">Discount %</label>
          <div className="flex items-center gap-1.5">
            <Input
              value={minDelta}
              onChange={(e) => {
                setMinDelta(e.target.value);
                applyDeltaFilter(e.target.value, maxDelta);
              }}
              placeholder="Min"
              className="h-9 w-16 text-sm tabular-nums"
            />
            <span className="text-xs text-muted-foreground">–</span>
            <Input
              value={maxDelta}
              onChange={(e) => {
                setMaxDelta(e.target.value);
                applyDeltaFilter(minDelta, e.target.value);
              }}
              placeholder="Max"
              className="h-9 w-16 text-sm tabular-nums"
            />
          </div>
        </div>
      </div>

      {/* Table */}
      <div
        ref={cardRef}
        className={cn(
          "flex-1 min-h-0 overflow-auto bg-card rounded-xl border shadow-sm transition-opacity",
          isFetching && !isLoading && "opacity-60"
        )}
      >
        {isLoading ? (
          <div className="p-6 space-y-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : rows.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full min-h-[240px] gap-3 border border-dashed rounded-xl m-6 p-8 text-center">
            <Package className="h-10 w-10 text-muted-foreground/40" />
            <p className="text-sm text-muted-foreground">
              {debouncedSearch
                ? `No discounted products matching "${debouncedSearch}".`
                : "No discounted products found."}
            </p>
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4 w-14" />
                <TableHead className="min-w-[180px] text-xs font-medium text-muted-foreground">
                  Product
                </TableHead>
                <TableHead className="min-w-[120px] text-xs font-medium text-muted-foreground">
                  SKU code
                </TableHead>
                <TableHead className="min-w-[60px] text-xs font-medium text-muted-foreground">
                  Size
                </TableHead>
                <TableHead className="min-w-[160px] text-xs font-medium text-muted-foreground">
                  Price
                </TableHead>
                <TableHead className="min-w-[70px] text-xs font-medium text-muted-foreground">
                  Δ%
                </TableHead>
                <TableHead className="min-w-[160px] text-xs font-medium text-muted-foreground">
                  Winning campaign
                </TableHead>
                <TableHead className="min-w-[140px] text-xs font-medium text-muted-foreground">
                  Other campaigns
                </TableHead>
                <TableHead className="min-w-[70px] pr-4 text-xs font-medium text-muted-foreground text-right">
                  Stock
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow
                  key={row.skuId}
                  className="cursor-pointer"
                  onClick={() =>
                    router.push(`/manage/products/${row.productBaseId}/edit`)
                  }
                >
                  {/* Image */}
                  <TableCell className="pl-4 py-2">
                    {row.imageUrl ? (
                      <Image
                        src={row.imageUrl}
                        alt={row.productName}
                        width={40}
                        height={40}
                        unoptimized
                        className="rounded-md object-cover w-10 h-10 shrink-0 bg-muted"
                      />
                    ) : (
                      <div className="w-10 h-10 rounded-md bg-muted shrink-0" />
                    )}
                  </TableCell>

                  {/* Product + SKU sub-label */}
                  <TableCell>
                    <div>
                      <p className="text-sm font-medium truncate max-w-[180px]">
                        {row.productName}
                      </p>
                      <p className="text-xs text-muted-foreground font-mono">
                        {row.productCode}
                      </p>
                    </div>
                  </TableCell>

                  {/* SKU code */}
                  <TableCell className="text-xs font-mono text-muted-foreground">
                    {row.skuCode}
                  </TableCell>

                  {/* Size */}
                  <TableCell className="text-sm text-muted-foreground">
                    {row.sizeLabel || "—"}
                  </TableCell>

                  {/* Original → final price */}
                  <TableCell>
                    <div className="flex items-center gap-1.5">
                      <span className="text-sm line-through text-muted-foreground tabular-nums">
                        {row.originalPrice.toLocaleString()} ₸
                      </span>
                      <span className="text-sm font-semibold text-rose-600 dark:text-rose-400 tabular-nums">
                        {row.finalPrice.toLocaleString()} ₸
                      </span>
                    </div>
                  </TableCell>

                  {/* Δ% pill */}
                  <TableCell>
                    <span className="inline-block rounded-full bg-rose-100 dark:bg-rose-900/30 text-rose-600 dark:text-rose-400 text-xs font-semibold px-2 py-0.5 tabular-nums">
                      −{row.deltaPercent.toFixed(1)}%
                    </span>
                  </TableCell>

                  {/* Winning campaign pill — click navigates to campaign edit */}
                  <TableCell>
                    <button
                      className="inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold text-white truncate max-w-[150px]"
                      style={{
                        backgroundColor: `#${row.winningCampaign.colorHex}`,
                      }}
                      onClick={(e) => {
                        e.stopPropagation();
                        router.push(
                          `/manage/discounts/${row.winningCampaign.id}/edit`
                        );
                      }}
                    >
                      {row.winningCampaign.title}
                    </button>
                  </TableCell>

                  {/* Other campaigns chips */}
                  <TableCell>
                    <div className="flex flex-wrap gap-1">
                      {row.otherCampaigns.slice(0, 2).map((c) => (
                        <span
                          key={c.id}
                          className="inline-block rounded-full px-2 py-0.5 text-xs font-medium text-white truncate max-w-[90px]"
                          style={{ backgroundColor: `#${c.colorHex}` }}
                        >
                          {c.title}
                        </span>
                      ))}
                      {row.otherCampaigns.length > 2 && (
                        <span className="inline-block rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                          +{row.otherCampaigns.length - 2}
                        </span>
                      )}
                    </div>
                  </TableCell>

                  {/* Stock */}
                  <TableCell className="pr-4 text-right">
                    <span
                      className={cn(
                        "text-sm font-medium tabular-nums",
                        row.stockQuantity === 0
                          ? "text-rose-500"
                          : row.stockQuantity <= 5
                          ? "text-orange-500"
                          : "text-foreground"
                      )}
                    >
                      {row.stockQuantity}
                    </span>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            {data.totalElements} product{data.totalElements !== 1 ? "s" : ""} discounted
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={filters.page <= 1}
              onClick={() => setFilters((f) => ({ ...f, page: f.page - 1 }))}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="text-sm text-muted-foreground">
              {filters.page} / {data.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={data.last}
              onClick={() => setFilters((f) => ({ ...f, page: f.page + 1 }))}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
