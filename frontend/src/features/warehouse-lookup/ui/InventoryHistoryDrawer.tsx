"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { ChevronLeft, ChevronRight } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/shared/ui/sheet";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/table";
import { Button } from "@/shared/ui/button";
import { Skeleton } from "@/shared/ui/skeleton";
import { cn } from "@/shared/lib";
import { formatDate } from "@/shared/lib/format";
import type { InventoryTransactionType } from "../types";
import { useInventoryHistory } from "../api";

const PAGE_SIZE = 20;

const TYPE_STYLES: Record<InventoryTransactionType, string> = {
  SALE:              "bg-rose-50 text-rose-700 ring-1 ring-rose-200",
  CANCELLATION:      "bg-blue-50 text-blue-700 ring-1 ring-blue-200",
  RECALL_RETURN:     "bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200",
  RETURN_RESTORE:    "bg-green-50 text-green-700 ring-1 ring-green-200",
  WRITE_OFF:         "bg-zinc-100 text-zinc-600 ring-1 ring-zinc-200",
  RECEIPT:           "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200",
  MANUAL_ADJUSTMENT: "bg-amber-50 text-amber-700 ring-1 ring-amber-200",
};

interface Props {
  open: boolean;
  onClose: () => void;
  skuId: number;
  productName: string;
  skuCode: string;
}

export function InventoryHistoryDrawer({ open, onClose, skuId, productName, skuCode }: Props) {
  const t = useTranslations("warehouse");
  const [page, setPage] = useState(1);
  const { data, isLoading } = useInventoryHistory(open ? skuId : null, page);

  function renderDelta(delta: number) {
    if (delta > 0) return <span className="text-green-700 font-mono tabular-nums">+{delta}</span>;
    if (delta < 0) return <span className="text-rose-700 font-mono tabular-nums">{delta}</span>;
    return <span className="text-muted-foreground font-mono tabular-nums">0</span>;
  }

  function renderReference(referenceType: string | null, referenceId: number | null) {
    if (!referenceType || !referenceId) return <span className="text-muted-foreground">—</span>;
    if (referenceType === "STOCK_RECEIPT") {
      return (
        <Link
          href={`/warehouse/receipts/${referenceId}`}
          className="text-amber-700 hover:underline font-mono text-xs"
        >
          Receipt #{referenceId}
        </Link>
      );
    }
    if (referenceType === "ORDER") {
      return (
        <a
          href={`/manage/orders/${referenceId}`}
          target="_blank"
          rel="noopener noreferrer"
          className="text-blue-700 hover:underline font-mono text-xs"
        >
          Order #{referenceId}
        </a>
      );
    }
    return (
      <span className="font-mono text-xs text-muted-foreground">
        {referenceType} #{referenceId}
      </span>
    );
  }

  return (
    <Sheet open={open} onOpenChange={(o) => !o && onClose()}>
      <SheetContent side="right" className="sm:max-w-3xl flex flex-col">
        <SheetHeader className="shrink-0">
          <SheetTitle>{t("lookup.history.title")}</SheetTitle>
          <div className="flex items-center gap-2 mt-1 text-sm text-muted-foreground">
            <span className="font-medium text-zinc-900">{productName}</span>
            <span className="font-mono text-xs bg-zinc-100 px-1.5 py-0.5 rounded">
              {skuCode}
            </span>
          </div>
        </SheetHeader>

        <div className="flex-1 overflow-y-auto mt-4">
          {isLoading ? (
            <div className="space-y-2">
              {[...Array(6)].map((_, i) => (
                <Skeleton key={i} className="h-10 w-full rounded-lg" />
              ))}
            </div>
          ) : !data || data.content.length === 0 ? (
            <div className="flex items-center justify-center border border-dashed rounded-xl p-12 text-muted-foreground">
              <p className="text-sm">{t("lookup.history.empty")}</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("lookup.history.columns.when")}</TableHead>
                  <TableHead>{t("lookup.history.columns.type")}</TableHead>
                  <TableHead className="text-right">{t("lookup.history.columns.delta")}</TableHead>
                  <TableHead>{t("lookup.history.columns.reference")}</TableHead>
                  <TableHead>{t("lookup.history.columns.actor")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((tx) => (
                  <TableRow key={tx.id}>
                    <TableCell
                      className="text-xs text-muted-foreground whitespace-nowrap"
                      title={tx.occurredAt}
                    >
                      {formatDate(tx.occurredAt)}
                    </TableCell>
                    <TableCell>
                      <span
                        className={cn(
                          "inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide",
                          TYPE_STYLES[tx.type] ?? "bg-zinc-100 text-zinc-600",
                        )}
                      >
                        {t(`lookup.history.type.${tx.type}` as Parameters<typeof t>[0])}
                      </span>
                    </TableCell>
                    <TableCell className="text-right">
                      {renderDelta(tx.delta)}
                    </TableCell>
                    <TableCell>
                      {renderReference(tx.referenceType, tx.referenceId)}
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {tx.actorUserId ? `User #${tx.actorUserId}` : "—"}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>

        {/* Pagination */}
        {data && data.totalElements > PAGE_SIZE && (
          <div className="flex items-center justify-between text-sm text-muted-foreground shrink-0 pt-4 border-t border-zinc-100 mt-4">
            <span>
              {(page - 1) * PAGE_SIZE + 1}–
              {Math.min(page * PAGE_SIZE, data.totalElements)} / {data.totalElements}
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
        )}
      </SheetContent>
    </Sheet>
  );
}
