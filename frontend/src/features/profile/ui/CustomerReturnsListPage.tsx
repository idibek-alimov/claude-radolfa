"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { ChevronLeft, ChevronRight, Package } from "lucide-react";
import Link from "next/link";
import { useMyReturns } from "../api";
import { formatPrice, formatDate } from "@/shared/lib/format";
import { getErrorMessage } from "@/shared/lib";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Skeleton } from "@/shared/ui/skeleton";
import type { CustomerReturnStatus } from "@/entities/pickpoint";

const RETURN_STATUS_STYLES: Record<
  CustomerReturnStatus,
  { label: string; className: string }
> = {
  RECEIVED:          { label: "returnStatus.received",       className: "bg-blue-100 text-blue-800" },
  SENT_TO_WAREHOUSE: { label: "returnStatus.sentToWarehouse", className: "bg-yellow-100 text-yellow-800" },
  REFUND_APPROVED:   { label: "returnStatus.refundApproved", className: "bg-green-100 text-green-800" },
  REFUNDED:          { label: "returnStatus.refunded",       className: "bg-emerald-100 text-emerald-800" },
};

export function CustomerReturnsListPage() {
  const t = useTranslations("profile");
  const [page, setPage] = useState(1);
  const { data, isLoading, isError, error } = useMyReturns(page);

  if (isError) {
    return (
      <div className="rounded-lg border border-destructive/20 bg-destructive/5 p-6 text-center text-sm text-destructive">
        {getErrorMessage(error)}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">{t("myReturns")}</h2>

      {isLoading ? (
        <div className="space-y-3">
          {[...Array(3)].map((_, i) => (
            <Skeleton key={i} className="h-24 w-full rounded-xl" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed p-12 text-center">
          <Package className="mb-3 h-10 w-10 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">{t("noReturns")}</p>
        </div>
      ) : (
        <>
          <div className="space-y-3">
            {data.content.map((ret) => {
              const style = RETURN_STATUS_STYLES[ret.status];
              return (
                <div
                  key={ret.returnId}
                  className="flex items-center justify-between rounded-xl border bg-card px-5 py-4"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium">
                        {t("returnLabel")} #{ret.returnId}
                      </span>
                      <Badge className={style.className}>
                        {t(style.label as Parameters<typeof t>[0])}
                      </Badge>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {t("forOrder")} #{ret.orderId} · {formatDate(ret.receivedAt)}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {t("itemCount", { count: ret.items.length })}
                    </p>
                    {ret.totalRefundAmount != null && (
                      <p className="text-xs font-medium text-green-700">
                        {t("refundAmount")}: {formatPrice(ret.totalRefundAmount)}
                      </p>
                    )}
                  </div>
                  <Link
                    href={`/orders/${ret.orderId}`}
                    className="text-xs text-primary hover:underline"
                  >
                    {t("viewOrder")} →
                  </Link>
                </div>
              );
            })}
          </div>

          {data.totalPages > 1 && (
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              <span>
                {t("showing")} {(page - 1) * 10 + 1}–
                {Math.min(page * 10, data.totalElements)} {t("of")}{" "}
                {data.totalElements}
              </span>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => p - 1)}
                  disabled={data.first}
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
