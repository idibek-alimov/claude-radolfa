"use client";

import Image from "next/image";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import {
  Check,
  CircleCheckBig,
  Clock,
  Package,
  Truck,
  X,
} from "lucide-react";
import { Button } from "@/shared/ui/button";
import { cancelOrder } from "@/features/profile/api";
import { getErrorMessage } from "@/shared/lib";
import type { Order } from "@/features/profile/types";

/* ── Status timeline constants ─────────────────────────────────── */

const ORDER_STEPS = ["PENDING", "PAID", "SHIPPED", "DELIVERED"] as const;

const STEP_ICONS = {
  PENDING: Clock,
  PAID: Check,
  SHIPPED: Truck,
  DELIVERED: CircleCheckBig,
} as const;

const STEP_KEYS = {
  PENDING: "orderPlaced",
  PAID: "statusPaid",
  SHIPPED: "orderShipped",
  DELIVERED: "orderDelivered",
} as const;

function getStepIndex(status: string): number {
  const idx = ORDER_STEPS.indexOf(status as (typeof ORDER_STEPS)[number]);
  return idx === -1 ? 0 : idx;
}

/* ── Timeline ──────────────────────────────────────────────────── */

function OrderTimeline({ status }: { status: string }) {
  const t = useTranslations("profile");
  const currentStep = getStepIndex(status);
  const isCancelled = status === "CANCELLED";

  if (isCancelled) {
    return (
      <div className="flex items-center gap-2 mt-3">
        <X className="h-4 w-4 text-destructive" />
        <span className="text-xs text-destructive font-medium">
          {t("statusCancelled")}
        </span>
      </div>
    );
  }

  return (
    <div className="flex items-center mt-3 w-full overflow-hidden">
      {ORDER_STEPS.map((step, i) => {
        const Icon = STEP_ICONS[step];
        const isComplete = i <= currentStep;
        const isCurrent = i === currentStep;
        return (
          <div key={step} className="flex items-center flex-1 min-w-0 last:flex-initial">
            <div className="flex flex-col items-center min-w-0">
              <div
                className={`flex items-center justify-center h-6 w-6 sm:h-7 sm:w-7 rounded-full shrink-0 transition-colors ${
                  isCurrent
                    ? "bg-primary text-primary-foreground"
                    : isComplete
                    ? "bg-primary/20 text-primary"
                    : "bg-muted text-muted-foreground"
                }`}
              >
                <Icon className="h-3 w-3 sm:h-3.5 sm:w-3.5" />
              </div>
              <span
                className={`text-[10px] sm:text-xs mt-1 text-center leading-tight truncate max-w-[60px] sm:max-w-none ${
                  isComplete ? "text-foreground" : "text-muted-foreground"
                }`}
              >
                {t(STEP_KEYS[step])}
              </span>
            </div>
            {i < ORDER_STEPS.length - 1 && (
              <div
                className={`h-0.5 flex-1 min-w-2 mx-0.5 rounded-full mt-[-14px] ${
                  i < currentStep ? "bg-primary/40" : "bg-muted"
                }`}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}

/* ── OrderHistoryCard ──────────────────────────────────────────── */

export function OrderHistoryCard({ order }: { order: Order }) {
  const t = useTranslations("profile");
  const queryClient = useQueryClient();

  const cancelMutation = useMutation({
    mutationFn: () => cancelOrder(order.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["my-orders"] });
      toast.success(t("orderCancelled"));
    },
    onError: (err: unknown) => {
      toast.error(t("failedToCancel") + " " + getErrorMessage(err));
    },
  });

  return (
    <div className="border rounded-xl p-4 hover:border-primary/20 transition-colors">
      {/* Header row */}
      <div className="flex justify-between items-start mb-2">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-medium text-sm">
            {t("orderNumber", { id: order.id })}
          </span>
          <span
            className={`text-xs px-2 py-0.5 rounded-full font-medium ${
              order.status === "PENDING"
                ? "bg-yellow-100 text-yellow-800"
                : order.status === "DELIVERED"
                ? "bg-green-100 text-green-800"
                : order.status === "CANCELLED"
                ? "bg-red-100 text-red-800"
                : order.status === "SHIPPED"
                ? "bg-blue-100 text-blue-800"
                : "bg-gray-100 text-gray-800"
            }`}
          >
            {order.status}
          </span>
          <span className="text-xs text-muted-foreground">
            {t("itemCount", { count: order.items.length })}
          </span>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          {order.status === "PENDING" && (
            <Button
              variant="ghost"
              size="sm"
              className="h-7 text-xs text-destructive hover:text-destructive hover:bg-destructive/10 px-2"
              disabled={cancelMutation.isPending}
              onClick={() => cancelMutation.mutate()}
            >
              {cancelMutation.isPending ? t("cancelling") : t("cancelOrder")}
            </Button>
          )}
          <span className="text-xs text-muted-foreground">
            {new Date(order.createdAt).toLocaleDateString()}
          </span>
        </div>
      </div>

      {/* Thumbnail strip */}
      <div className="flex gap-2 overflow-x-auto py-1 scrollbar-none">
        {order.items.map((item, idx) => (
          <div
            key={idx}
            className="relative h-12 w-12 rounded-lg border overflow-hidden shrink-0 bg-muted"
            title={item.productName}
          >
            {item.imageUrl ? (
              <Image
                src={item.imageUrl}
                alt={item.productName}
                fill
                sizes="48px"
                className="object-cover"
                unoptimized
              />
            ) : (
              <div className="flex h-full w-full items-center justify-center">
                <Package className="h-4 w-4 text-muted-foreground" />
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Status timeline */}
      <OrderTimeline status={order.status} />

      {/* Total */}
      <p className="font-semibold text-sm text-right mt-3 pt-3 border-t">
        {t("total")} {order.totalAmount.toFixed(2)} TJS
      </p>
    </div>
  );
}
