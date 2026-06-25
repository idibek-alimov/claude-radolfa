"use client";

import { useState, useEffect } from "react";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
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
import { fetchDiscountChangeLog } from "../api";
import type { DiscountChangeLogEntry } from "../model/types";

const PAGE_SIZE = 20;

const TYPE_STYLES: Record<DiscountChangeLogEntry["changeType"], string> = {
  CREATE: "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200",
  UPDATE: "bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200",
};

const SUMMARY_FIELDS = ["amountType", "amountValue", "validFrom", "validUpto", "disabled"] as const;

function parseSnapshot(json: string | null): Record<string, unknown> | null {
  if (!json) return null;
  try {
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}

function summarize(entry: DiscountChangeLogEntry, t: ReturnType<typeof useTranslations>): string {
  if (entry.changeType === "CREATE") return t("discountChangeLog.created");

  const before = parseSnapshot(entry.oldValue);
  const after = parseSnapshot(entry.newValue);
  if (!before || !after) return "—";

  const parts: string[] = [];
  for (const field of SUMMARY_FIELDS) {
    if (JSON.stringify(before[field]) !== JSON.stringify(after[field])) {
      parts.push(`${field}: ${String(before[field])} → ${String(after[field])}`);
    }
  }
  if (JSON.stringify(before.targets) !== JSON.stringify(after.targets)) {
    parts.push("targets changed");
  }
  return parts.length > 0 ? parts.join(", ") : "—";
}

interface Props {
  open: boolean;
  onClose: () => void;
  discountId: number | null;
}

export function DiscountChangeLogDrawer({ open, onClose, discountId }: Props) {
  const t = useTranslations("manage");
  const [page, setPage] = useState(1);
  useEffect(() => { setPage(1); }, [discountId]);

  const { data, isLoading } = useQuery({
    queryKey: ["discount-change-log", discountId, page],
    queryFn: () => fetchDiscountChangeLog(discountId!, { page, size: PAGE_SIZE }),
    enabled: open && discountId !== null,
    placeholderData: keepPreviousData,
  });

  return (
    <Sheet open={open} onOpenChange={(o) => !o && onClose()}>
      <SheetContent side="right" className="sm:max-w-3xl flex flex-col">
        <SheetHeader className="shrink-0">
          <SheetTitle>{t("discountChangeLog.title")}</SheetTitle>
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
              <p className="text-sm">{t("discountChangeLog.empty")}</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("discountChangeLog.columns.when")}</TableHead>
                  <TableHead>{t("discountChangeLog.columns.type")}</TableHead>
                  <TableHead>{t("discountChangeLog.columns.summary")}</TableHead>
                  <TableHead>{t("discountChangeLog.columns.changedBy")}</TableHead>
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
                    <TableCell>
                      <span
                        className={cn(
                          "inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide",
                          TYPE_STYLES[c.changeType],
                        )}
                      >
                        {c.changeType === "CREATE"
                          ? t("discountChangeLog.types.create")
                          : t("discountChangeLog.types.update")}
                      </span>
                    </TableCell>
                    <TableCell className="text-sm">{summarize(c, t)}</TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {c.actorUserId ? `User #${c.actorUserId}` : "—"}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>

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
