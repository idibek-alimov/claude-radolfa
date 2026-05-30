"use client";

import { useState, useCallback, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { Search, ChevronLeft, ChevronRight } from "lucide-react";
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
import { Tabs, TabsList, TabsTrigger } from "@radolfa/shared/ui/tabs";
import { OrderStatusBadge } from "@/entities/order/ui/OrderStatusBadge";
import { useAdminOrders } from "@/entities/order";
import type { AdminOrderListItem } from "@/entities/order";
import { useDynamicPageSize } from "@radolfa/shared/lib";

const TAB_GROUPS = {
  "needs-action": {
    label: "tabNeedsAction",
    statuses: ["PENDING", "PAID", "DELIVERY_ATTEMPTED", "RETURN_INITIATED", "RECALL_REQUESTED"],
  },
  "in-fulfillment": {
    label: "tabInFulfillment",
    statuses: ["SHIPPED", "OUT_FOR_DELIVERY", "READY_FOR_PICKUP", "RETURNED_TO_WAREHOUSE"],
  },
  "completed": {
    label: "tabCompleted",
    statuses: ["DELIVERED", "CANCELLED", "REFUNDED"],
  },
} as const;

type TabKey = keyof typeof TAB_GROUPS;

const NEEDS_ACTION_CSV   = TAB_GROUPS["needs-action"].statuses.join(",");
const IN_FULFILLMENT_CSV = TAB_GROUPS["in-fulfillment"].statuses.join(",");
const COMPLETED_CSV      = TAB_GROUPS["completed"].statuses.join(",");

function TabBadge({ count }: { count: number | undefined }) {
  if (!count) return null;
  return (
    <span className="ml-1.5 rounded-full bg-muted px-1.5 py-0.5 text-[10px] font-semibold leading-none">
      {count}
    </span>
  );
}

export function OrderManagementTable() {
  const t = useTranslations("manage.orders");

  const router       = useRouter();
  const searchParams = useSearchParams();

  const activeTab = (searchParams.get("tab") ?? "needs-action") as TabKey;

  const [page, setPage]                 = useState(1);
  const [searchQuery, setSearchQuery]   = useState("");
  const [debouncedSearch, setDebounced] = useState("");

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

  const activeStatuses = TAB_GROUPS[activeTab].statuses.join(",");

  const { data, isLoading } = useAdminOrders({
    page,
    search: debouncedSearch,
    statuses: activeStatuses,
    sortBy: "createdAt",
    sortDir: "DESC",
    size: pageSize,
  });

  // Lightweight count queries for the other two tabs
  const { data: needsActionData }   = useAdminOrders({ page: 1, size: 1, statuses: NEEDS_ACTION_CSV });
  const { data: inFulfillmentData } = useAdminOrders({ page: 1, size: 1, statuses: IN_FULFILLMENT_CSV });
  const { data: completedData }     = useAdminOrders({ page: 1, size: 1, statuses: COMPLETED_CSV });

  const tabCounts: Record<TabKey, number | undefined> = {
    "needs-action":   needsActionData?.totalElements,
    "in-fulfillment": inFulfillmentData?.totalElements,
    "completed":      completedData?.totalElements,
  };

  const orders: AdminOrderListItem[] = data?.content ?? [];

  function formatDate(iso: string) {
    return new Date(iso).toLocaleDateString("ru-RU", {
      day: "2-digit", month: "2-digit", year: "2-digit",
    });
  }

  return (
    <div className="flex flex-col flex-1 min-h-0">
      {/* Tabs */}
      <Tabs
        value={activeTab}
        onValueChange={(v) => {
          setPage(1);
          router.replace(`?tab=${v}`);
        }}
        className="mb-4"
      >
        <TabsList>
          {(Object.entries(TAB_GROUPS) as [TabKey, typeof TAB_GROUPS[TabKey]][]).map(([key, group]) => (
            <TabsTrigger key={key} value={key}>
              {t(group.label as Parameters<typeof t>[0])}
              <TabBadge count={tabCounts[key]} />
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>

      {/* Search */}
      <div className="mb-4">
        <div className="relative max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => handleSearch(e.target.value)}
            placeholder={t("searchPlaceholder")}
            className="pl-9"
          />
        </div>
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
