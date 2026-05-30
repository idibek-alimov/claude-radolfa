"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { PackageOpen, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@radolfa/shared/ui/button";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@radolfa/shared/ui/table";
import { formatDate } from "@radolfa/shared/lib/format";
import { useWarehouseReturnsQueue } from "../api";

export function ReturnsQueuePage() {
  const t = useTranslations("warehouse");
  const router = useRouter();
  const [page, setPage] = useState(1);
  const { data, isLoading } = useWarehouseReturnsQueue(page);

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">{t("returns.title")}</h1>
          <p className="text-sm text-muted-foreground mt-0.5">{t("returns.subtitle")}</p>
        </div>
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="space-y-2">
          {[...Array(5)].map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-lg" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center border border-dashed rounded-xl p-12 text-muted-foreground gap-3">
          <PackageOpen className="h-10 w-10 text-muted-foreground/40" />
          <p className="text-sm">{t("returns.empty")}</p>
        </div>
      ) : (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("returns.columns.returnId")}</TableHead>
                <TableHead>{t("returns.columns.orderId")}</TableHead>
                <TableHead>{t("returns.columns.customer")}</TableHead>
                <TableHead className="text-right">{t("returns.columns.items")}</TableHead>
                <TableHead>{t("returns.columns.sentAt")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((ret) => (
                <TableRow
                  key={ret.id}
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => router.push(`/warehouse/returns/${ret.id}`)}
                >
                  <TableCell className="font-medium tabular-nums">#{ret.id}</TableCell>
                  <TableCell className="text-sm text-muted-foreground tabular-nums">#{ret.orderId}</TableCell>
                  <TableCell>
                    <div>
                      <span className="text-sm font-medium">{ret.customerName ?? "—"}</span>
                      {ret.customerPhone && (
                        <div className="text-xs text-muted-foreground">{ret.customerPhone}</div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right font-bold tabular-nums">{ret.items.length}</TableCell>
                  <TableCell className="text-sm text-muted-foreground whitespace-nowrap">
                    {ret.sentToWarehouseAt ? formatDate(ret.sentToWarehouseAt) : "—"}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          {/* Pagination */}
          {data.totalElements > data.size && (
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              <span>
                {(data.number - 1) * data.size + 1}–{Math.min(data.number * data.size, data.totalElements)}{" "}
                / {data.totalElements}
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
        </>
      )}
    </div>
  );
}
