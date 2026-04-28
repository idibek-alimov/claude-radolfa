"use client";

import Image from "next/image";
import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import {
  Check,
  ChevronDown,
  ChevronUp,
  CircleCheckBig,
  Clock,
  Package,
  Pencil,
  RotateCcw,
  Truck,
  X,
} from "lucide-react";
import { Button } from "@/shared/ui/button";
import { cancelOrder } from "@/features/profile/api";
import { addToCart } from "@/entities/cart";
import { SubmitReviewForm } from "@/features/review-submission";
import { getErrorMessage } from "@/shared/lib";
import type { Order, OrderItem } from "@/features/profile/types";

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

/* ── WriteReviewButton ─────────────────────────────────────────── */

function WriteReviewButton({
  orderId,
  item,
}: {
  orderId: number;
  item: OrderItem;
}) {
  const t = useTranslations("profile");
  const [reviewOpen, setReviewOpen] = useState(false);

  if (!item.skuId || !item.listingVariantId || !item.slug) return null;

  return (
    <>
      <Button
        variant="outline"
        size="sm"
        className="h-6 text-[10px] px-1.5 gap-1 shrink-0"
        onClick={() => setReviewOpen(true)}
      >
        <Pencil className="h-3 w-3" />
        {t("writeReview")}
      </Button>
      <SubmitReviewForm
        listingVariantId={item.listingVariantId}
        slug={item.slug}
        preselectedOrderId={orderId}
        preselectedSkuId={item.skuId}
        open={reviewOpen}
        onOpenChange={setReviewOpen}
      />
    </>
  );
}

/* ── BuyAgainButton ────────────────────────────────────────────── */

function BuyAgainButton({ skuId }: { skuId: number }) {
  const t = useTranslations("profile");
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => addToCart(skuId, 1),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cart"] });
      toast.success(t("buyAgainSuccess"));
    },
    onError: (err: unknown) => {
      toast.error(getErrorMessage(err));
    },
  });

  return (
    <Button
      variant="outline"
      size="sm"
      className="h-6 text-[10px] px-1.5 gap-1 shrink-0"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      <RotateCcw className="h-3 w-3" />
      {t("buyAgain")}
    </Button>
  );
}

/* ── OrderDetailsAccordion ─────────────────────────────────────── */

function OrderDetailsAccordion({ order }: { order: Order }) {
  const t = useTranslations("profile");
  const isDelivered = order.status === "DELIVERED";

  const subtotal = order.items.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0
  );

  return (
    <div className="mt-3 border rounded-lg overflow-hidden">
      <table className="w-full text-xs">
        <thead>
          <tr className="bg-muted/50 text-muted-foreground">
            <th className="text-left px-3 py-2 font-medium">{t("product")}</th>
            <th className="text-left px-3 py-2 font-medium hidden sm:table-cell">{t("sizeLabel")}</th>
            <th className="text-left px-3 py-2 font-medium hidden sm:table-cell">{t("skuCode")}</th>
            <th className="text-right px-3 py-2 font-medium">{t("quantity")}</th>
            <th className="text-right px-3 py-2 font-medium">{t("total")}</th>
            {isDelivered && <th className="px-3 py-2" />}
          </tr>
        </thead>
        <tbody className="divide-y">
          {order.items.map((item, idx) => (
            <tr key={idx} className="hover:bg-muted/20 transition-colors">
              <td className="px-3 py-2 font-medium max-w-[140px] truncate">{item.productName}</td>
              <td className="px-3 py-2 text-muted-foreground hidden sm:table-cell">
                {item.sizeLabel ?? "—"}
              </td>
              <td className="px-3 py-2 text-muted-foreground font-mono hidden sm:table-cell">
                {item.skuCode ?? "—"}
              </td>
              <td className="px-3 py-2 text-right text-muted-foreground">×{item.quantity}</td>
              <td className="px-3 py-2 text-right font-medium tabular-nums">
                {(item.price * item.quantity).toFixed(2)} TJS
              </td>
              {isDelivered && (
                <td className="px-3 py-2 text-right">
                  <div className="flex gap-1.5 justify-end flex-wrap">
                    {item.skuId != null && <BuyAgainButton skuId={item.skuId} />}
                    {!item.hasReviewed && (
                      <WriteReviewButton orderId={order.id} item={item} />
                    )}
                  </div>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>

      {/* Price summary */}
      <div className="bg-muted/30 px-3 py-2 space-y-1 text-xs border-t">
        <div className="flex justify-between text-muted-foreground">
          <span>{t("subtotal")}</span>
          <span className="tabular-nums">{subtotal.toFixed(2)} TJS</span>
        </div>
        {order.loyaltyPointsRedeemed > 0 && (
          <div className="flex justify-between text-amber-600">
            <span>{t("loyaltyUsed")}</span>
            <span className="tabular-nums">−{order.loyaltyPointsRedeemed} pts</span>
          </div>
        )}
        <div className="flex justify-between font-semibold text-sm pt-1 border-t">
          <span>{t("total")}</span>
          <span className="tabular-nums">{order.totalAmount.toFixed(2)} TJS</span>
        </div>
      </div>
    </div>
  );
}

/* ── OrderHistoryCard ──────────────────────────────────────────── */

export function OrderHistoryCard({ order }: { order: Order }) {
  const t = useTranslations("profile");
  const queryClient = useQueryClient();
  const [detailsOpen, setDetailsOpen] = useState(false);

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

      {/* View Details toggle */}
      <Button
        variant="ghost"
        size="sm"
        className="mt-3 h-7 text-xs px-2 gap-1 text-muted-foreground hover:text-foreground"
        onClick={() => setDetailsOpen((v) => !v)}
      >
        {detailsOpen ? (
          <>
            <ChevronUp className="h-3.5 w-3.5" />
            {t("hideDetails")}
          </>
        ) : (
          <>
            <ChevronDown className="h-3.5 w-3.5" />
            {t("viewDetails")}
          </>
        )}
      </Button>

      {/* Accordion content */}
      {detailsOpen && <OrderDetailsAccordion order={order} />}

      {/* Total */}
      <p className="font-semibold text-sm text-right mt-3 pt-3 border-t">
        {t("total")} {order.totalAmount.toFixed(2)} TJS
      </p>
    </div>
  );
}
