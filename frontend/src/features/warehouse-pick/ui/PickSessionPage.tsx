"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { ChevronLeft, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";
import { getErrorMessage } from "@/shared/lib";
import { Button } from "@/shared/ui/button";
import { Card, CardContent } from "@/shared/ui/card";
import { Skeleton } from "@/shared/ui/skeleton";
import { usePickSession, useCompletePickSession } from "../api";
import { PickScanInput } from "./PickScanInput";
import { OrderItemPickRow } from "./OrderItemPickRow";

interface Props {
  orderId: number;
}

export function PickSessionPage({ orderId }: Props) {
  const t = useTranslations("warehouse");
  const router = useRouter();
  const { data: session, isLoading, isError } = usePickSession(orderId);
  const [lastScannedItemId, setLastScannedItemId] = useState<number | null>(null);
  const complete = useCompletePickSession(orderId);

  function handleScanned(orderItemId: number) {
    setLastScannedItemId(orderItemId);
    setTimeout(() => setLastScannedItemId(null), 600);
  }

  function handleComplete() {
    complete.mutate(undefined, {
      onSuccess: () => toast.success(t("pick.session.completeSuccess")),
      onError: (e) => toast.error(getErrorMessage(e)),
    });
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6 max-w-5xl">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-12 w-72" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Skeleton className="h-48 rounded-xl" />
          <div className="lg:col-span-2 flex flex-col gap-3">
            <Skeleton className="h-20 rounded-xl" />
            <Skeleton className="h-20 rounded-xl" />
          </div>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center border border-dashed rounded-xl p-12 text-muted-foreground gap-3">
        <p className="text-sm">{t("pick.session.loadError")}</p>
        <Link href="/warehouse/pick" className="text-sm underline underline-offset-4">
          {t("pick.session.backToQueue")}
        </Link>
      </div>
    );
  }

  if (!session) return null;

  const totalUnits = session.items.reduce((sum, i) => sum + i.quantity, 0);
  const totalPicked = session.items.reduce((sum, i) => sum + i.quantityPicked, 0);
  const isPicked = session.status === "PICKED";
  const allPicked = totalUnits > 0 && totalPicked === totalUnits;

  return (
    <div className="flex flex-col gap-6 max-w-5xl">
      {/* Breadcrumb */}
      <Link
        href="/warehouse/pick"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-zinc-900 transition-colors w-fit"
      >
        <ChevronLeft className="h-4 w-4" />
        {t("pick.session.backToQueue")}
      </Link>

      {/* Title + pills */}
      <div className="flex items-center gap-3 flex-wrap">
        <h1 className="text-2xl font-semibold text-zinc-900">
          #{session.externalOrderId}
        </h1>
        <span
          className={`text-xs font-medium px-2 py-0.5 rounded-full ${
            session.deliveryType === "HOME"
              ? "bg-blue-100 text-blue-700"
              : "bg-orange-100 text-orange-700"
          }`}
        >
          {session.deliveryType}
        </span>
        <span
          className={`text-xs font-medium px-2 py-0.5 rounded-full ${
            isPicked ? "bg-green-100 text-green-700" : "bg-amber-100 text-amber-700"
          }`}
        >
          {session.status}
        </span>
      </div>

      {isPicked ? (
        /* Completion banner — full width */
        <div className="rounded-xl border border-green-200 bg-green-50 p-8 flex flex-col items-center gap-4 text-center">
          <CheckCircle2 className="h-12 w-12 text-green-600" />
          <p className="text-lg font-semibold text-green-800">{t("pick.session.complete")}</p>
          <Button onClick={() => router.push("/warehouse/pick")} variant="outline">
            {t("pick.session.backToQueue")}
          </Button>
        </div>
      ) : (
        /* Active session grid */
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left: scan input + aggregate progress */}
          <div className="flex flex-col gap-4">
            <Card>
              <CardContent className="pt-6">
                <PickScanInput orderId={orderId} onScanned={handleScanned} />
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-5 pb-5">
                <p className="text-xs uppercase tracking-wide text-muted-foreground mb-1">
                  {t("pick.session.title")}
                </p>
                <p className="text-2xl font-bold tabular-nums">
                  {totalPicked} / {totalUnits}
                </p>
              </CardContent>
            </Card>
            {allPicked && !isPicked && (
              <div className="flex flex-col gap-2">
                <p className="text-xs text-muted-foreground text-center">
                  {t("pick.session.allScannedHint")}
                </p>
                <Button
                  className="w-full"
                  disabled={complete.isPending}
                  onClick={handleComplete}
                >
                  {complete.isPending
                    ? t("pick.session.completing")
                    : t("pick.session.completeButton")}
                </Button>
              </div>
            )}
          </div>

          {/* Right: item rows */}
          <div className="lg:col-span-2 flex flex-col gap-3">
            {session.items.map((item) => (
              <OrderItemPickRow
                key={item.orderItemId}
                item={item}
                flash={lastScannedItemId === item.orderItemId}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
