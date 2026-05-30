"use client";

import React, { useState, useCallback, useRef, useEffect } from "react";
import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { useAuth } from "@radolfa/shared/auth";
import { fetchAdminProducts, useApproveProduct, usePendingProductCount } from "@/entities/product/api/moderation";
import { ProductStatusBadge } from "@/entities/product/ui/ProductStatusBadge";
import { ProductStatus } from "@/entities/product/model/types";
import { RejectProductDialog } from "@/features/product-moderation";
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@radolfa/shared/ui/table";
import { Tabs, TabsList, TabsTrigger } from "@radolfa/shared/ui/tabs";
import { Button } from "@radolfa/shared/ui/button";
import { Input } from "@radolfa/shared/ui/input";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { Pencil, Search, Package, Plus, ChevronLeft, ChevronRight, CheckCircle } from "lucide-react";
import { useTranslations } from "next-intl";
import { useDynamicPageSize } from "@radolfa/shared/lib";

const STATUS_TABS = [
  "ALL",
  "DRAFT",
  "PENDING_REVIEW",
  "AWAITING_STOCK",
  "ACTIVE",
  "REJECTED",
] as const;

type StatusTab = (typeof STATUS_TABS)[number];

function TabBadge({ count }: { count: number | undefined }) {
  if (!count) return null;
  return (
    <span className="ml-1.5 rounded-full bg-muted px-1.5 py-0.5 text-[10px] font-semibold leading-none">
      {count > 99 ? "99+" : count}
    </span>
  );
}

function formatUpdatedAt(iso: string): string {
  const d = new Date(iso);
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(d);
}

export function ProductManagementTable() {
  const t = useTranslations("manage");
  const tp = useTranslations("manage.products");
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  // Default tab for ADMIN is PENDING_REVIEW; set via URL on first mount
  const statusParam = searchParams.get("status") as StatusTab | null;
  const activeTab: StatusTab = statusParam ?? "ALL";

  useEffect(() => {
    if (isAdmin && !searchParams.get("status")) {
      router.replace("?status=PENDING_REVIEW");
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdmin]);

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

  function handleTabChange(tab: string) {
    setPage(1);
    if (tab === "ALL") {
      router.replace("?");
    } else {
      router.replace(`?status=${tab}`);
    }
  }

  const statusFilter = activeTab === "ALL" ? undefined : (activeTab as ProductStatus);

  const { data, isLoading } = useQuery({
    queryKey: ["admin-products", { page, search: debouncedSearch, size: pageSize, status: statusFilter }],
    queryFn: () => fetchAdminProducts({ search: debouncedSearch, page, size: pageSize, status: statusFilter }),
    placeholderData: keepPreviousData,
  });

  const { data: pendingCountData } = usePendingProductCount(isAdmin);
  const pendingCount = pendingCountData?.count ?? 0;

  const approve = useApproveProduct();

  const rows = data?.content ?? [];

  return (
    <div className="flex flex-col flex-1 min-h-0">
      {/* Status tabs */}
      <Tabs value={activeTab} onValueChange={handleTabChange} className="mb-4">
        <TabsList>
          {STATUS_TABS.map((tab) => (
            <TabsTrigger key={tab} value={tab}>
              {tp(`tabs.${tab}` as Parameters<typeof tp>[0])}
              {tab === "PENDING_REVIEW" && <TabBadge count={pendingCount} />}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>

      {/* Search + New Product */}
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
        {isLoading && rows.length === 0 ? (
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
                <TableHead>{t("tableStatus")}</TableHead>
                <TableHead>{tp("tableUpdated")}</TableHead>
                <TableHead className="text-right pr-4">{t("tableActions")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.productBaseId}>
                  <TableCell className="pl-4">
                    {row.primaryImageUrl ? (
                      <div className="relative h-10 w-10 rounded-md border overflow-hidden">
                        <Image
                          src={row.primaryImageUrl}
                          alt={row.name}
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
                      <p className="font-medium text-sm">{row.name}</p>
                      <p className="text-xs text-muted-foreground truncate max-w-xs">
                        {row.externalRef ?? row.productCode}
                      </p>
                    </div>
                  </TableCell>
                  <TableCell>
                    <ProductStatusBadge status={row.status} />
                  </TableCell>
                  <TableCell>
                    <span className="text-sm text-muted-foreground">
                      {formatUpdatedAt(row.updatedAt)}
                    </span>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center justify-end gap-1.5 pr-4">
                      {isAdmin && row.status === ProductStatus.PENDING_REVIEW && (
                        <>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-emerald-700 border-emerald-200 hover:bg-emerald-50"
                            onClick={() => approve.mutate(row.productBaseId)}
                            disabled={approve.isPending}
                          >
                            <CheckCircle className="h-3.5 w-3.5 mr-1" />
                            {tp("actions.approve")}
                          </Button>
                          <RejectProductDialog productBaseId={row.productBaseId} />
                        </>
                      )}
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => router.push(`/manage/products/${row.productBaseId}/edit`)}
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        {rows.length === 0 && !isLoading && (
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
