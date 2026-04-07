"use client";

import React, { useState, useCallback, useRef, useEffect } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { fetchListings, searchListings } from "@/entities/product/api";
import type { ListingVariant } from "@/entities/product/model/types";
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
import { Pencil, Lock, Search, Package, Plus, ChevronLeft, ChevronRight } from "lucide-react";
import { useTranslations } from "next-intl";
import { useDynamicPageSize } from "@/shared/lib";

export function ProductManagementTable() {
  const t = useTranslations("manage");
  const router = useRouter();

  const [page, setPage] = useState(1);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const debounceRef = useRef<NodeJS.Timeout | undefined>(undefined);
  const cardRef = useRef<HTMLDivElement>(null);
  const pageSize = useDynamicPageSize(cardRef, 57);

  useEffect(() => { setPage(1); }, [pageSize]);

  const handleSearchChange = useCallback((value: string) => {
    setSearchQuery(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value.trim());
      setPage(1);
    }, 300);
  }, []);

  const { data, isLoading } = useQuery({
    queryKey: ["listings", page, debouncedSearch, pageSize],
    queryFn: () =>
      debouncedSearch
        ? searchListings(debouncedSearch, page, pageSize)
        : fetchListings(page, pageSize),
    placeholderData: keepPreviousData,
  });

  const listings = data?.content ?? [];

  const displayPrice = (item: ListingVariant) => `${item.originalPrice.toFixed(2)} TJS`;
  const computeStock = (item: ListingVariant) =>
    item.skus.reduce((acc, s) => acc + s.stockQuantity, 0);

  return (
    <div className="flex flex-col flex-1 min-h-0">
      <div className="mb-4 flex items-center gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder={t("searchByName")}
            className="pl-9"
          />
        </div>
        <Button className="gap-1.5" onClick={() => router.push("/manage/products/create")}>
          <Plus className="h-4 w-4" />
          {t("newProduct")}
        </Button>
      </div>

      <div ref={cardRef} className="flex-1 min-h-0 overflow-auto bg-card rounded-xl border shadow-sm">
        {isLoading && listings.length === 0 ? (
          <div className="p-6 space-y-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4 w-[56px]">{t("tableImage")}</TableHead>
                <TableHead>{t("tableProduct")}</TableHead>
                <TableHead>{t("tableSlugKey")}</TableHead>
                <TableHead>{t("tablePrice")}</TableHead>
                <TableHead>{t("tableStock")}</TableHead>
                <TableHead>{t("tableStatus")}</TableHead>
                <TableHead className="text-right pr-4">{t("tableActions")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {listings.map((item) => (
                <TableRow key={item.variantId}>
                  <TableCell className="pl-4">
                    {item.images?.[0] ? (
                      <div className="relative h-10 w-10 rounded-md border overflow-hidden">
                        <Image
                          src={item.images[0]}
                          alt={item.colorDisplayName}
                          width={40}
                          height={40}
                          className="object-cover aspect-square"
                          unoptimized
                        />
                      </div>
                    ) : (
                      <div className="flex h-10 w-10 items-center justify-center rounded-md bg-muted">
                        <Package className="h-4 w-4 text-muted-foreground" />
                      </div>
                    )}
                  </TableCell>
                  <TableCell>
                    <div>
                      <p className="font-medium text-sm">{item.colorDisplayName}</p>
                      <p className="text-xs text-muted-foreground truncate max-w-xs">
                        {item.productCode}
                      </p>
                    </div>
                  </TableCell>
                  <TableCell>
                    <code className="text-xs bg-muted px-1.5 py-0.5 rounded">
                      {item.slug}
                    </code>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1">
                      <Lock className="h-3 w-3 text-muted-foreground" />
                      <span className="text-sm">{displayPrice(item)}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1">
                      <Lock className="h-3 w-3 text-muted-foreground" />
                      <span className="text-sm">{computeStock(item)}</span>
                    </div>
                  </TableCell>
                  <TableCell />
                  <TableCell className="text-right pr-4">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => router.push(`/manage/products/${item.slug}/edit`)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        {listings.length === 0 && !isLoading && (
          <div className="p-12 text-center text-muted-foreground">
            {debouncedSearch
              ? t("noProductsMatching", { search: debouncedSearch })
              : t("noProductsFound")}
          </div>
        )}
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-muted-foreground">
            {t("productsTotal", { count: data.totalElements })}
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
            <span className="text-sm text-muted-foreground">{t("page", { page })}</span>
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
    </div>
  );
}
