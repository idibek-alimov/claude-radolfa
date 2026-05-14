"use client";

import { useState, useCallback, useRef } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { Search, ChevronLeft, ChevronRight } from "lucide-react";
import { keepPreviousData } from "@tanstack/react-query";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { Skeleton } from "@/shared/ui/skeleton";
import { OrderStatusBadge } from "@/entities/order/ui/OrderStatusBadge";
import { useAdminOrders } from "@/entities/order";
import type { AdminOrderListItem, OrderStatus } from "@/entities/order";
import { useDynamicPageSize } from "@/shared/lib";

const STATUS_OPTIONS: Array<{ value: OrderStatus | "ALL"; label: string }> = [
  { value: "ALL", label: "statusAll" },
  { value: "PENDING",   label: "status.PENDING" },
  { value: "PAID",      label: "status.PAID" },
  { value: "SHIPPED",          label: "status.SHIPPED" },
  { value: "READY_FOR_PICKUP", label: "status.READY_FOR_PICKUP" },
  { value: "DELIVERED",        label: "status.DELIVERED" },
  { value: "CANCELLED", label: "status.CANCELLED" },
];

export function OrderManagementTable() {
  const t = useTranslations("manage.orders");

  const router = useRouter();

  const [page, setPage]                 = useState(1);
  const [searchQuery, setSearchQuery]   = useState("");
  const [debouncedSearch, setDebounced] = useState("");
  const [statusFilter, setStatus]       = useState<OrderStatus | "">("");

  const debounceRef = useRef<NodeJS.Timeout | undefined>(undefined);
  const cardRef     = useRef<HTMLDivElement>(null);
  const pageSize    = useDynamicPageSize(cardRef, 49);

  const handleSearch = useCallback((value: string) => {
    setSearchQuery(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebounced(value.trim());
      setPage(1);
    }, 300);
  }, []);

  const { data, isLoading } = useAdminOrders({
    page,
    search: debouncedSearch,
    status: statusFilter,
    sortBy: "createdAt",
    sortDir: "DESC",
    size: pageSize,
  });

  const orders: AdminOrderListItem[] = data?.content ?? [];

  function formatDate(iso: string) {
    return new Date(iso).toLocaleDateString("ru-RU", {
      day: "2-digit", month: "2-digit", year: "2-digit",
    });
  }

  return (
    <div className="flex flex-col flex-1 min-h-0">
      {/* Toolbar */}
      <div className="mb-4 flex items-center gap-3 flex-wrap">
        <div className="relative max-w-sm flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => handleSearch(e.target.value)}
            placeholder={t("searchPlaceholder")}
            className="pl-9"
          />
        </div>
        <Select
          value={statusFilter || "ALL"}
          onValueChange={(v) => { setStatus(v === "ALL" ? "" : v as OrderStatus); setPage(1); }}
        >
          <SelectTrigger className="w-44">
            <SelectValue placeholder={t("statusAll")} />
          </SelectTrigger>
          <SelectContent>
            {STATUS_OPTIONS.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {t(opt.label as Parameters<typeof t>[0])}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Table */}
      <div ref={cardRef} className="flex-1 min-h-0 overflow-auto bg-card rounded-xl border shadow-sm">
        {isLoading && orders.length === 0 ? (
          <div className="p-6 space-y-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4">{t("table.id")}</TableHead>
                <TableHead>{t("table.customer")}</TableHead>
                <TableHead>{t("table.date")}</TableHead>
                <TableHead>{t("table.status")}</TableHead>
                <TableHead>{t("table.delivery")}</TableHead>
                <TableHead>{t("table.items")}</TableHead>
                <TableHead className="text-right pr-4">{t("table.total")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {orders.map((order) => (
                <TableRow
                  key={order.id}
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => router.push(`/manage/orders/${order.id}`)}
                >
                  <TableCell className="pl-4 font-mono text-xs">{order.id}</TableCell>
                  <TableCell>
                    <p className="text-sm font-medium font-mono">{order.userPhone}</p>
                    {order.userName && (
                      <p className="text-xs text-muted-foreground">{order.userName}</p>
                    )}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(order.createdAt)}
                  </TableCell>
                  <TableCell>
                    <OrderStatusBadge status={order.status} />
                  </TableCell>
                  <TableCell>
                    {order.deliveryType ? (
                      <span className="text-xs text-muted-foreground">
                        {t(`deliveryType.${order.deliveryType}` as Parameters<typeof t>[0])}
                      </span>
                    ) : (
                      <span className="text-muted-foreground">—</span>
                    )}
                  </TableCell>
                  <TableCell className="text-sm">{order.itemCount}</TableCell>
                  <TableCell className="text-right pr-4 text-sm font-semibold tabular-nums">
                    {order.totalAmount.toFixed(2)} TJS
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        {orders.length === 0 && !isLoading && (
          <div className="p-12 text-center text-muted-foreground">{t("empty")}</div>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-muted-foreground">
            {t("total", { count: data.totalElements })}
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
