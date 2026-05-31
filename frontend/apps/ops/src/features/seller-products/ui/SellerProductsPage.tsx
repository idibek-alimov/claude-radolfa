"use client";

import React, { useState, useCallback, useRef, useEffect } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import {
  useMyProducts,
  useSubmitMyProductForReview,
} from "@/entities/seller/api/seller";
import { createMyProduct } from "@/entities/seller/api/seller";
import { ProductStatusBadge } from "@/entities/product/ui/ProductStatusBadge";
import { ProductStatus } from "@/entities/product/model/types";
import { ProductCreationWizard } from "@/features/product-creation";
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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@radolfa/shared/ui/dialog";
import {
  Package,
  Plus,
  Search,
  ChevronLeft,
  ChevronRight,
  Pencil,
  Send,
} from "lucide-react";
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

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(new Date(iso));
}

export function SellerProductsPage() {
  const t = useTranslations("seller");
  const tp = useTranslations("manage.products");
  const router = useRouter();

  const [activeTab, setActiveTab] = useState<StatusTab>("ALL");
  const [page, setPage] = useState(1);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const debounceRef = useRef<NodeJS.Timeout | undefined>(undefined);
  const [showCreateWizard, setShowCreateWizard] = useState(false);
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
    setActiveTab(tab as StatusTab);
    setPage(1);
  }

  const statusFilter = activeTab === "ALL" ? undefined : (activeTab as ProductStatus);

  const { data, isLoading } = useMyProducts({
    status: statusFilter,
    search: debouncedSearch,
    page,
    size: pageSize,
  });

  const submitForReview = useSubmitMyProductForReview();
  const rows = data?.content ?? [];

  return (
    <div className="flex flex-col flex-1 min-h-0 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">{t("products.title")}</h1>
        <Button className="gap-1.5" onClick={() => setShowCreateWizard(true)}>
          <Plus className="h-4 w-4" />
          {t("products.createProduct")}
        </Button>
      </div>

      {/* Status tabs */}
      <Tabs value={activeTab} onValueChange={handleTabChange}>
        <TabsList>
          {STATUS_TABS.map((tab) => (
            <TabsTrigger key={tab} value={tab}>
              {tp(`tabs.${tab}` as Parameters<typeof tp>[0])}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>

      {/* Search */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          value={searchQuery}
          onChange={(e) => handleSearchChange(e.target.value)}
          placeholder={t("products.searchPlaceholder")}
          className="pl-9"
        />
      </div>

      {/* Table */}
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
                <TableHead className="pl-4 w-[56px]">{t("products.columns.image")}</TableHead>
                <TableHead>{t("products.columns.product")}</TableHead>
                <TableHead>{t("products.columns.status")}</TableHead>
                <TableHead>{t("products.columns.updated")}</TableHead>
                <TableHead className="text-right pr-4">{t("products.columns.actions")}</TableHead>
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
                    <span className="text-sm text-muted-foreground">{formatDate(row.updatedAt)}</span>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center justify-end gap-1.5 pr-4">
                      {(row.status === ProductStatus.DRAFT ||
                        row.status === ProductStatus.REJECTED) && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="text-blue-700 border-blue-200 hover:bg-blue-50"
                          onClick={() => submitForReview.mutate(row.productBaseId)}
                          disabled={submitForReview.isPending}
                        >
                          <Send className="h-3.5 w-3.5 mr-1" />
                          {t("products.submitForReview")}
                        </Button>
                      )}
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() =>
                          router.push(`/seller/products/${row.productBaseId}/edit`)
                        }
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
            {t("products.empty")}
          </div>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            {t("products.total", { count: data.totalElements })}
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
            <span className="text-sm text-muted-foreground">{t("products.page", { page })}</span>
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

      {/* Create Product Wizard (Dialog) */}
      <Dialog open={showCreateWizard} onOpenChange={setShowCreateWizard}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{t("products.createProduct")}</DialogTitle>
          </DialogHeader>
          <ProductCreationWizard
            createFn={createMyProduct}
            successPath="/seller/products"
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}
