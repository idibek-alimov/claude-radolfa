"use client";

import { useState, useRef, useCallback } from "react";
import Image from "next/image";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/shared/ui/sheet";
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@/shared/ui/table";
import { Input } from "@/shared/ui/input";
import { Button } from "@/shared/ui/button";
import { Skeleton } from "@/shared/ui/skeleton";
import { fetchCampaignSkus, fetchDiscountById } from "../api";
import { ChevronLeft, ChevronRight, Search, Package } from "lucide-react";
import { cn } from "@/shared/lib";

interface CampaignSkuDrawerProps {
  campaignId: number | null;
  onOpenChange: (open: boolean) => void;
}

export function CampaignSkuDrawer({ campaignId, onOpenChange }: CampaignSkuDrawerProps) {
  const isOpen = campaignId !== null;

  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [page, setPage] = useState(1);
  const debounceRef = useRef<NodeJS.Timeout | undefined>(undefined);

  const handleSearchChange = useCallback((value: string) => {
    setSearch(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value.trim());
      setPage(1);
    }, 300);
  }, []);

  const { data: campaign } = useQuery({
    queryKey: ["discount", campaignId],
    queryFn: () => fetchDiscountById(campaignId!),
    enabled: campaignId !== null,
  });

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["campaign-skus", campaignId, debouncedSearch, page],
    queryFn: () => fetchCampaignSkus(campaignId!, { search: debouncedSearch || undefined, page, size: 15 }),
    enabled: campaignId !== null,
    placeholderData: keepPreviousData,
  });

  const rows = data?.content ?? [];

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      setSearch("");
      setDebouncedSearch("");
      setPage(1);
    }
    onOpenChange(open);
  };

  return (
    <Sheet open={isOpen} onOpenChange={handleOpenChange}>
      <SheetContent
        side="right"
        className="w-full sm:max-w-2xl flex flex-col p-0 gap-0"
      >
        {/* Header */}
        <SheetHeader className="px-6 pt-6 pb-4 border-b shrink-0">
          <div className="flex items-center gap-3 pr-6">
            {campaign && (
              <span
                className="inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold text-white shrink-0"
                style={{ backgroundColor: `#${campaign.colorHex}` }}
              >
                {campaign.title}
              </span>
            )}
            <SheetTitle className="text-base font-semibold truncate">
              {campaign ? `${campaign.amountValue}${campaign.amountType === "FIXED" ? " TJS" : "%"} off — SKUs` : "Campaign SKUs"}
            </SheetTitle>
          </div>
        </SheetHeader>

        {/* Search */}
        <div className="px-6 py-3 border-b shrink-0">
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
            <Input
              value={search}
              onChange={(e) => handleSearchChange(e.target.value)}
              placeholder="Search by SKU or product…"
              className="pl-8 h-9 text-sm"
            />
          </div>
        </div>

        {/* Table */}
        <div
          className={cn(
            "flex-1 overflow-auto transition-opacity",
            isFetching && !isLoading && "opacity-60"
          )}
        >
          {isLoading ? (
            <div className="p-4 space-y-3">
              {Array.from({ length: 8 }).map((_, i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : rows.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full min-h-[240px] gap-3 border border-dashed rounded-xl m-6 p-8 text-center">
              <Package className="h-10 w-10 text-muted-foreground/40" />
              <p className="text-sm text-muted-foreground">
                {debouncedSearch
                  ? `No SKUs matching "${debouncedSearch}".`
                  : "No SKUs in this campaign."}
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  <TableHead className="pl-6 w-14" />
                  <TableHead className="min-w-[160px] text-xs font-medium text-muted-foreground">
                    Product
                  </TableHead>
                  <TableHead className="min-w-[120px] text-xs font-medium text-muted-foreground">
                    SKU code
                  </TableHead>
                  <TableHead className="min-w-[60px] text-xs font-medium text-muted-foreground">
                    Size
                  </TableHead>
                  <TableHead className="min-w-[140px] text-xs font-medium text-muted-foreground">
                    Price
                  </TableHead>
                  <TableHead className="min-w-[70px] pr-6 text-xs font-medium text-muted-foreground text-right">
                    Stock
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.skuId}>
                    {/* Image */}
                    <TableCell className="pl-6 py-2">
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

                    {/* Product name */}
                    <TableCell className="text-sm font-medium max-w-[160px]">
                      <span className="block truncate">{row.productName}</span>
                    </TableCell>

                    {/* SKU code */}
                    <TableCell className="text-xs font-mono text-muted-foreground">
                      {row.skuCode}
                    </TableCell>

                    {/* Size */}
                    <TableCell className="text-sm text-muted-foreground">
                      {row.sizeLabel || "—"}
                    </TableCell>

                    {/* Price */}
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

                    {/* Stock */}
                    <TableCell className="pr-6 text-right">
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

        {/* Footer pagination */}
        {data && data.totalElements > 0 && (
          <div className="px-6 py-3 border-t shrink-0 flex items-center justify-between">
            <p className="text-xs text-muted-foreground">
              {data.totalElements} SKU{data.totalElements !== 1 ? "s" : ""} total
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page <= 1}
                onClick={() => setPage((p) => p - 1)}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-xs text-muted-foreground tabular-nums">
                {page} / {data.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={data.last}
                onClick={() => setPage((p) => p + 1)}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
