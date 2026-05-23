"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { ChevronLeft } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import { Skeleton } from "@/shared/ui/skeleton";
import { formatDate } from "@/shared/lib";
import { cn } from "@/shared/lib";
import { useStockReceipt } from "../api";
import { ReceiptLineItemsTable } from "./ReceiptLineItemsTable";

interface Props {
  id: number;
}

export function StockReceiptDetailPage({ id }: Props) {
  const t = useTranslations("warehouse");
  const { data, isLoading } = useStockReceipt(id);

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6 max-w-4xl">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-40 w-full rounded-xl" />
        <Skeleton className="h-60 w-full rounded-xl" />
      </div>
    );
  }

  if (!data) return null;

  return (
    <div className="flex flex-col gap-6 max-w-4xl">
      {/* Breadcrumb */}
      <div>
        <Link
          href="/warehouse/receipts"
          className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-zinc-900 transition-colors"
        >
          <ChevronLeft className="h-4 w-4" />
          {t("receipts.detail.backToList")}
        </Link>
      </div>

      {/* Title + status */}
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-semibold text-zinc-900">
          {t("receipts.detail.title")} #{data.id}
        </h1>
        <span
          className={cn(
            "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold",
            "bg-green-50 text-green-700 ring-1 ring-green-200"
          )}
        >
          {t("receipts.detail.statusCompleted")}
        </span>
      </div>

      {/* Metadata */}
      <Card>
        <CardHeader className="pb-4">
          <CardTitle className="text-sm font-semibold border-b pb-2">
            {t("receipts.detail.title")}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid grid-cols-2 gap-x-8 gap-y-4 text-sm">
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground mb-0.5">
                {t("receipts.detail.supplierRef")}
              </dt>
              <dd className="font-medium">
                {data.supplierReference ?? <span className="italic text-muted-foreground">—</span>}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground mb-0.5">
                {t("receipts.detail.date")}
              </dt>
              <dd className="font-medium">{formatDate(data.createdAt)}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground mb-0.5">
                {t("receipts.detail.createdBy")}
              </dt>
              <dd className="font-medium">#{data.createdByUserId}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground mb-0.5">
                {t("receipts.detail.totalUnits")}
              </dt>
              <dd className="text-xl font-bold tabular-nums">{data.totalUnitsReceived}</dd>
            </div>
          </dl>

          {data.notes && (
            <div className="mt-4 pt-4 border-t border-zinc-100">
              <p className="text-xs uppercase tracking-wide text-muted-foreground mb-1">
                {t("receipts.detail.notes")}
              </p>
              <p className="text-sm text-zinc-700">{data.notes}</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Line items */}
      <Card>
        <CardContent className="pt-5">
          <ReceiptLineItemsTable
            readonly
            items={data.items.map((item) => ({
              skuCode: item.skuCode,
              productName: item.productName,
              quantityReceived: item.quantityReceived,
              notes: item.notes,
            }))}
          />
        </CardContent>
      </Card>
    </div>
  );
}
