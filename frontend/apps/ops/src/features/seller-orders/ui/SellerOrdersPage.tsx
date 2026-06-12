"use client";

import { useState, useCallback, useRef, useEffect } from "react";
import { useMyOrders } from "@/entities/seller/api/seller";
import { OrderStatusBadge } from "@/entities/order";
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@radolfa/shared/ui/table";
import { Button } from "@radolfa/shared/ui/button";
import { Input } from "@radolfa/shared/ui/input";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@radolfa/shared/ui/select";
import {
  ShoppingBag,
  Search,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { useTranslations } from "next-intl";
import { useDynamicPageSize } from "@radolfa/shared/lib";

type SortBy = "orderCreatedAt" | "orderStatus";
type SortDir = "ASC" | "DESC";

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(new Date(iso));
}

function formatPrice(price: number): string {
  return new Intl.NumberFormat(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(price);
}

export function SellerOrdersPage() {
  const t = useTranslations("seller");

  const [page, setPage] = useState(1);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [sortBy, setSortBy] = useState<SortBy>("orderCreatedAt");
  const [sortDir, setSortDir] = useState<SortDir>("DESC");
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

  function handleSortChange(newSortBy: SortBy) {
    setSortBy(newSortBy);
    setPage(1);
  }

  function toggleSortDir() {
    setSortDir((d) => (d === "DESC" ? "ASC" : "DESC"));
    setPage(1);
  }

  const { data, isLoading } = useMyOrders({
    search: debouncedSearch,
    sortBy,
    sortDir,
    page,
    size: pageSize,
  });

  const rows = data?.content ?? [];

  return (
    <div className="flex flex-col flex-1 min-h-0 space-y-4">
      <h1 className="text-2xl font-semibold">{t("orders.title")}</h1>

      {/* Controls */}
      <div className="flex items-center gap-3 flex-wrap">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder={t("orders.searchPlaceholder")}
            className="pl-9"
          />
        </div>
        <Select value={sortBy} onValueChange={(v) => handleSortChange(v as SortBy)}>
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="orderCreatedAt">{t("orders.sortDate")}</SelectItem>
            <SelectItem value="orderStatus">{t("orders.sortStatus")}</SelectItem>
          </SelectContent>
        </Select>
        <Button variant="outline" size="sm" onClick={toggleSortDir}>
          {sortDir === "DESC" ? t("orders.sortDesc") : t("orders.sortAsc")}
        </Button>
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
                <TableHead className="pl-4">{t("orders.columns.orderId")}</TableHead>
                <TableHead>{t("orders.columns.product")}</TableHead>
                <TableHead>{t("orders.columns.sku")}</TableHead>
                <TableHead>{t("orders.columns.qty")}</TableHead>
                <TableHead>{t("orders.columns.price")}</TableHead>
                <TableHead>{t("orders.columns.status")}</TableHead>
                <TableHead>{t("orders.columns.date")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.orderItemId}>
                  <TableCell className="pl-4 font-mono text-xs">#{row.orderId}</TableCell>
                  <TableCell>
                    <span className="font-medium text-sm">{row.productName}</span>
                  </TableCell>
                  <TableCell>
                    <span className="font-mono text-xs text-muted-foreground">{row.skuCode}</span>
                  </TableCell>
                  <TableCell>{row.quantity}</TableCell>
                  <TableCell className="tabular-nums">{formatPrice(row.price)}</TableCell>
                  <TableCell>
                    <OrderStatusBadge status={row.orderStatus} />
                  </TableCell>
                  <TableCell>
                    <span className="text-sm text-muted-foreground">
                      {formatDate(row.orderCreatedAt)}
                    </span>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        {rows.length === 0 && !isLoading && (
          <div className="flex flex-col items-center justify-center p-12 text-center">
            <ShoppingBag className="h-10 w-10 text-muted-foreground/40 mb-4" />
            <p className="text-sm text-muted-foreground">{t("orders.empty")}</p>
          </div>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            {t("orders.total", { count: data.totalElements })}
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
            <span className="text-sm text-muted-foreground">{t("orders.page", { page })}</span>
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
