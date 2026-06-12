"use client";

import { useState, useMemo } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { ChevronLeft } from "lucide-react";
import { Button } from "@radolfa/shared/ui/button";
import { Card, CardContent } from "@radolfa/shared/ui/card";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { cn } from "@radolfa/shared/lib";
import { formatDate, formatPrice } from "@radolfa/shared/lib/format";
import type { Resellability } from "@/entities/pickpoint";
import { useWarehouseReturn, useReviewReturnItems } from "../api";
import type { ItemReview } from "../api";
import { ReturnItemReviewRow } from "./ReturnItemReviewRow";

interface Props {
  id: number;
}

const STATUS_STYLES: Record<string, string> = {
  SENT_TO_WAREHOUSE: "bg-amber-50 text-amber-700 ring-amber-200",
};

export function ReturnReviewPage({ id }: Props) {
  const t = useTranslations("warehouse");
  const { data, isLoading, isError } = useWarehouseReturn(id);
  const reviewMutation = useReviewReturnItems(id);

  const [chosen, setChosen] = useState<Record<number, Resellability>>({});

  const pendingItems = useMemo(
    () => (data?.items ?? []).filter((i) => i.resellability === "PENDING_REVIEW"),
    [data]
  );

  const allPendingChosen = pendingItems.every((i) => chosen[i.orderItemId] != null);
  const canSubmit = allPendingChosen && pendingItems.length > 0 && !reviewMutation.isPending;

  function handleSubmit() {
    const reviews: ItemReview[] = pendingItems.map((i) => ({
      orderItemId: i.orderItemId,
      resellability: chosen[i.orderItemId]!,
    }));
    reviewMutation.mutate(reviews, {
      onSuccess: () => setChosen({}),
    });
  }

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
        <p className="text-sm">{t("returns.review.loadError")}</p>
        <Link href="/warehouse/returns" className="text-sm underline underline-offset-4">
          {t("returns.review.backToQueue")}
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
          href="/warehouse/returns"
          className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-zinc-900 transition-colors"
        >
          <ChevronLeft className="h-4 w-4" />
          {t("returns.review.backToQueue")}
        </Link>
      </div>

      {/* Title + status */}
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-semibold text-zinc-900">
          {t("returns.review.title", { id: data.id })}
        </h1>
        <span
          className={cn(
            "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1",
            STATUS_STYLES[data.status] ?? "bg-zinc-50 text-zinc-700 ring-zinc-200"
          )}
        >
          {data.status}
        </span>
      </div>

      {/* Metadata */}
      <Card>
        <CardContent className="pt-6">
          <dl className="grid grid-cols-2 gap-x-8 gap-y-4 text-sm">
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground mb-0.5">
                {t("returns.review.metadata.orderId")}
              </dt>
              <dd className="font-medium">
                <a
                  href={`/manage/orders/${data.orderId}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-primary underline underline-offset-2"
                >
                  #{data.orderId}
                </a>
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground mb-0.5">
                {t("returns.review.metadata.customer")}
              </dt>
              <dd className="font-medium">
                {data.customerName ?? "—"}
                {data.customerPhone && (
                  <div className="text-xs text-muted-foreground font-normal">{data.customerPhone}</div>
                )}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground mb-0.5">
                {t("returns.review.metadata.pickpoint")}
              </dt>
              <dd className="font-medium">{data.pickpointName ?? "—"}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground mb-0.5">
                {t("returns.review.metadata.sentAt")}
              </dt>
              <dd className="font-medium">
                {data.sentToWarehouseAt ? formatDate(data.sentToWarehouseAt) : "—"}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground mb-0.5">
                {t("returns.review.metadata.totalRefund")}
              </dt>
              <dd className="text-xl font-bold tabular-nums">{formatPrice(data.totalRefundAmount)}</dd>
            </div>
          </dl>

          {data.notes && (
            <div className="mt-4 pt-4 border-t border-zinc-100">
              <p className="text-xs uppercase tracking-wide text-muted-foreground mb-1">
                {t("returns.review.metadata.notes")}
              </p>
              <p className="text-sm text-zinc-700">{data.notes}</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Items */}
      <Card>
        <CardContent className="pt-5">
          <p className="text-sm font-semibold border-b pb-2 mb-2">{t("returns.review.itemsTitle")}</p>
          <div>
            {data.items.map((item) => (
              <ReturnItemReviewRow
                key={item.orderItemId}
                item={item}
                chosen={item.resellability !== "PENDING_REVIEW" ? item.resellability : chosen[item.orderItemId]}
                onChange={(val) =>
                  setChosen((prev) => ({ ...prev, [item.orderItemId]: val }))
                }
              />
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Submit */}
      {pendingItems.length > 0 && (
        <div className="flex justify-end">
          <Button onClick={handleSubmit} disabled={!canSubmit}>
            {reviewMutation.isPending ? t("returns.review.submitting") : t("returns.review.submit")}
          </Button>
        </div>
      )}
    </div>
  );
}
