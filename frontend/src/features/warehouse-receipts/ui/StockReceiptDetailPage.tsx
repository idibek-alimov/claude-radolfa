"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { ChevronLeft } from "lucide-react";
import { Card, CardContent } from "@/shared/ui/card";
import { Skeleton } from "@/shared/ui/skeleton";
import { formatDate } from "@/shared/lib";
import { cn } from "@/shared/lib";
import { useStockReceipt } from "../api";
import { ReceiptLineItemsTable } from "./ReceiptLineItemsTable";

interface Props {
  id: number;
}

const STATUS_STYLES: Record<string, string> = {
  COMPLETED: "bg-green-50 text-green-700 ring-green-200",
};

export function StockReceiptDetailPage({ id }: Props) {
  const t = useTranslations("warehouse");
  const { data, isLoading, isError } = useStockReceipt(id);

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6 max-w-4xl">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-40 w-full rounded-xl" />
        <Skeleton className="h-60 w-full rounded-xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center border border-dashed rounded-xl p-12 text-muted-foreground gap-3">
        <p className="text-sm">{t("receipts.detail.loadError")}</p>
        <Link href="/warehouse/receipts" className="text-sm underline underline-offset-4">
          {t("receipts.detail.backToList")}
        </Link>
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
            "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1",
            STATUS_STYLES[data.status] ?? "bg-zinc-50 text-zinc-700 ring-zinc-200"
          )}
        >
          {t(`receipts.detail.status${data.status.charAt(0) + data.status.slice(1).toLowerCase()}`)}
        </span>
      </div>

      {/* Metadata */}
      <Card>
        <CardContent className="pt-6">
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

          <div className="mt-4 pt-4 border-t border-zinc-100">
            <p className="text-xs uppercase tracking-wide text-muted-foreground mb-1">
              {t("receipts.detail.notes")}
            </p>
            {data.notes ? (
              <p className="text-sm text-zinc-700">{data.notes}</p>
            ) : (
              <p className="text-sm italic text-muted-foreground">{t("receipts.detail.noNotes")}</p>
            )}
          </div>
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
