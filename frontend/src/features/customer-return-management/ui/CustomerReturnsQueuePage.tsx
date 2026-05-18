"use client";

import { useState, useEffect, useRef } from "react";
import { Search, ChevronLeft, ChevronRight } from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/table";
import { getErrorMessage } from "@/shared/lib";
import type { CustomerReturn, CustomerReturnStatus } from "@/entities/pickpoint";
import { useAdminCustomerReturns } from "../api";

const PAGE_SIZE = 20;

const STATUS_STYLES: Record<CustomerReturnStatus, string> = {
  RECEIVED:          "bg-amber-50 text-amber-700 ring-1 ring-amber-200",
  SENT_TO_WAREHOUSE: "bg-blue-50  text-blue-700  ring-1 ring-blue-200",
  REFUND_APPROVED:   "bg-violet-50 text-violet-700 ring-1 ring-violet-200",
  REFUNDED:          "bg-green-50 text-green-700  ring-1 ring-green-200",
};

const STATUS_LABELS: Record<CustomerReturnStatus, string> = {
  RECEIVED:          "Received",
  SENT_TO_WAREHOUSE: "At Warehouse",
  REFUND_APPROVED:   "Refund Sent",
  REFUNDED:          "Refunded",
};

function StatusBadge({ status }: { status: CustomerReturnStatus }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide",
        STATUS_STYLES[status],
      )}
    >
      {STATUS_LABELS[status]}
    </span>
  );
}

function firstReason(r: CustomerReturn): string {
  return r.items[0]?.reason.replace(/_/g, " ").toLowerCase() ?? "—";
}

export function CustomerReturnsQueuePage() {
  const [page, setPage]               = useState(1);
  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch, setDebounced] = useState("");
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebounced(searchInput);
      setPage(1);
    }, 300);
    return () => { if (debounceRef.current) clearTimeout(debounceRef.current); };
  }, [searchInput]);

  const { data, isLoading, isError, error } = useAdminCustomerReturns(page, PAGE_SIZE, debouncedSearch);

  if (isError) {
    toast.error(getErrorMessage(error, "Failed to load customer returns"));
  }

  return (
    <div className="flex flex-col flex-1 gap-6">
      <div>
        <h1 className="text-2xl font-semibold">Customer Returns</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Walk-in returns received at pickup points, pending refund processing.
        </p>
      </div>

      {/* Toolbar */}
      <div className="flex items-center gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            className="pl-9"
            placeholder="Search by order ID or customer name"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
        </div>
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="space-y-2">
          {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-12 w-full rounded-lg" />)}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed p-12 text-muted-foreground">
          <p className="text-sm">No customer returns found.</p>
        </div>
      ) : (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Return ID</TableHead>
                <TableHead>Order ID</TableHead>
                <TableHead>Customer</TableHead>
                <TableHead>Pickup Point</TableHead>
                <TableHead>Items</TableHead>
                <TableHead className="text-right">Refund</TableHead>
                <TableHead>Received</TableHead>
                <TableHead>Status</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((r) => (
                <TableRow key={r.id}>
                  <TableCell className="font-mono text-xs">#{r.id}</TableCell>
                  <TableCell className="font-mono text-xs">#{r.orderId}</TableCell>
                  <TableCell className="text-sm">{r.customerName ?? "—"}</TableCell>
                  <TableCell className="text-sm">{r.pickpointName ?? "—"}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {r.items.length} · {firstReason(r)}
                  </TableCell>
                  <TableCell className="text-right tabular-nums text-sm">
                    {typeof r.totalRefundAmount === "object"
                      ? (r.totalRefundAmount as { amount: number }).amount.toFixed(2)
                      : Number(r.totalRefundAmount).toFixed(2)}{" "}
                    TJS
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {new Date(r.receivedAt).toLocaleDateString()}
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={r.status} />
                  </TableCell>
                  <TableCell>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled
                      className="text-xs"
                    >
                      Approve &amp; Refund
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          {/* Pagination */}
          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <span>
              Showing {(page - 1) * PAGE_SIZE + 1}–
              {Math.min(page * PAGE_SIZE, data.totalElements)} of {data.totalElements}
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p - 1)}
                disabled={page <= 1}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={data.last}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
