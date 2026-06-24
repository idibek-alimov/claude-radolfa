"use client";

import { useState, useEffect } from "react";
import { useTranslations } from "next-intl";
import { ChevronLeft, ChevronRight } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@radolfa/shared/ui/sheet";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@radolfa/shared/ui/table";
import { Button } from "@radolfa/shared/ui/button";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { cn } from "@radolfa/shared/lib";
import { formatDate } from "@radolfa/shared/lib/format";
import { useSkuPriceHistory } from "@/entities/product/api/useSkuPriceHistory";

const PAGE_SIZE = 20;

const SOURCE_STYLES: Record<string, string> = {
  ADMIN_PANEL:  "bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200",
  SELLER_PANEL: "bg-amber-50 text-amber-700 ring-1 ring-amber-200",
};

interface Props {
  open: boolean;
  onClose: () => void;
  skuId: number | null;
  skuCode: string;
  sizeLabel: string;
}

export function SkuPriceHistoryDrawer({ open, onClose, skuId, skuCode, sizeLabel }: Props) {
  const t = useTranslations("manage");
  const [page, setPage] = useState(1);
  useEffect(() => { setPage(1); }, [skuId]);

  const { data, isLoading } = useSkuPriceHistory(open ? skuId : null, page);

  return (
    <Sheet open={open} onOpenChange={(o) => !o && onClose()}>
      <SheetContent side="right" className="sm:max-w-3xl flex flex-col">
        <SheetHeader className="shrink-0">
          <SheetTitle>{t("priceHistory.title")}</SheetTitle>
          <div className="flex items-center gap-2 mt-1 text-sm text-muted-foreground">
            {sizeLabel && <span className="font-medium text-zinc-900">{sizeLabel}</span>}
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
              <p className="text-sm">{t("priceHistory.empty")}</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("priceHistory.columns.when")}</TableHead>
                  <TableHead className="text-right">{t("priceHistory.columns.old")}</TableHead>
                  <TableHead className="text-right">{t("priceHistory.columns.new")}</TableHead>
                  <TableHead>{t("priceHistory.columns.source")}</TableHead>
                  <TableHead>{t("priceHistory.columns.changedBy")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((c) => (
                  <TableRow key={c.id}>
                    <TableCell
                      className="text-xs text-muted-foreground whitespace-nowrap"
                      title={c.occurredAt}
                    >
                      {formatDate(c.occurredAt)}
                    </TableCell>
                    <TableCell className="text-right text-muted-foreground line-through tabular-nums">
                      {c.oldPrice != null ? `${c.oldPrice.toFixed(2)} TJS` : "—"}
                    </TableCell>
                    <TableCell className="text-right font-medium tabular-nums">
                      {c.newPrice.toFixed(2)} TJS
                    </TableCell>
                    <TableCell>
                      {c.source ? (
                        <span
                          className={cn(
                            "inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide",
                            SOURCE_STYLES[c.source] ?? "bg-zinc-100 text-zinc-600",
                          )}
                        >
                          {c.source}
                        </span>
                      ) : (
                        <span className="text-muted-foreground">—</span>
                      )}
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {c.actorUserId ? `User #${c.actorUserId}` : "—"}
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
