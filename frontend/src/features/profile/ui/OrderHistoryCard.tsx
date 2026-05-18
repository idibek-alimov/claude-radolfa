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
  PackageCheck,
  Pencil,
  RotateCcw,
  Truck,
  X,
  type LucideIcon,
} from "lucide-react";
import { Button } from "@/shared/ui/button";
import { cancelOrder } from "@/features/profile/api";
import { addToCart } from "@/entities/cart";
import { useDeliveryCode } from "@/entities/order/api";
import { PickupCodeDisplay } from "@/entities/order/ui/PickupCodeDisplay";
import { SubmitReviewForm } from "@/features/review-submission";
import { getErrorMessage } from "@/shared/lib";
import type { Order, OrderItem } from "@/features/profile/types";

/* ── Status timeline constants ─────────────────────────────────── */

const HOME_STEPS      = ["PENDING", "PAID", "SHIPPED",          "DELIVERED"] as const;
const PICKPOINT_STEPS = ["PENDING", "PAID", "READY_FOR_PICKUP", "DELIVERED"] as const;

type AnyStep = (typeof HOME_STEPS)[number] | (typeof PICKPOINT_STEPS)[number];

const STEP_ICONS: Record<AnyStep, LucideIcon> = {
  PENDING:          Clock,
  PAID:             Check,
  SHIPPED:          Truck,
  READY_FOR_PICKUP: PackageCheck,
  DELIVERED:        CircleCheckBig,
};

const STEP_KEYS: Record<AnyStep, string> = {
  PENDING:          "orderPlaced",
  PAID:             "statusPaid",
  SHIPPED:          "orderShipped",
  READY_FOR_PICKUP: "orderReadyForPickup",
  DELIVERED:        "orderDelivered",
};

type StepsTuple = readonly string[];

function getStepIndex(status: string, steps: StepsTuple): number {
  const idx = steps.indexOf(status);
  return idx === -1 ? 0 : idx;
}

/* ── Timeline ──────────────────────────────────────────────────── */

function OrderTimeline({ status, deliveryType }: { status: string; deliveryType: string | null }) {
  const t = useTranslations("profile");
  const steps = deliveryType === "PICKPOINT" ? PICKPOINT_STEPS : HOME_STEPS;
  const currentStep = getStepIndex(status, steps);
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
      {steps.map((step, i) => {
        const Icon = STEP_ICONS[step as AnyStep];
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
                {t(STEP_KEYS[step as AnyStep] as Parameters<typeof t>[0])}
              </span>
            </div>
            {i < steps.length - 1 && (
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
        variant="default"
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
            <th className="px-3 py-2 w-12" />
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
              <td className="px-3 py-2">
                <div className="relative h-10 w-10 rounded-md border overflow-hidden bg-muted shrink-0">
                  {item.imageUrl ? (
                    <Image
                      src={item.imageUrl}
                      alt={item.productName}
                      fill
                      sizes="40px"
                      className="object-cover"
                      unoptimized
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center">
                      <Package className="h-4 w-4 text-muted-foreground" />
                    </div>
                  )}
                </div>
              </td>
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

const CODE_STATUSES = ["SHIPPED", "OUT_FOR_DELIVERY", "READY_FOR_PICKUP"] as const;

export function OrderHistoryCard({ order }: { order: Order }) {
  const t = useTranslations("profile");
  const queryClient = useQueryClient();
  const [detailsOpen, setDetailsOpen] = useState(false);
  const [showCode, setShowCode]       = useState(false);

  const needsCode = (CODE_STATUSES as readonly string[]).includes(order.status);
  const { data: codeData } = useDeliveryCode(order.id, needsCode);

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
    <div className="py-5">
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
                : order.status === "PAID"
                ? "bg-emerald-100 text-emerald-800"
                : order.status === "DELIVERED"
                ? "bg-green-100 text-green-800"
                : order.status === "CANCELLED"
                ? "bg-red-100 text-red-800"
                : order.status === "SHIPPED"
                ? "bg-cyan-100 text-cyan-800"
                : order.status === "OUT_FOR_DELIVERY"
                ? "bg-blue-100 text-blue-800"
                : order.status === "DELIVERY_ATTEMPTED"
                ? "bg-amber-100 text-amber-800"
                : order.status === "READY_FOR_PICKUP"
                ? "bg-indigo-100 text-indigo-800"
                : "bg-gray-100 text-gray-800"
            }`}
          >
            {order.status === "OUT_FOR_DELIVERY"
              ? t("statusOutForDelivery")
              : order.status === "DELIVERY_ATTEMPTED"
              ? t("statusDeliveryAttempted")
              : order.status === "READY_FOR_PICKUP"
              ? t("statusReadyForPickupWithCode")
              : order.status}
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

      {/* READY_FOR_PICKUP callout */}
      {order.status === "READY_FOR_PICKUP" && (
        <div className="mt-3 rounded-xl border border-indigo-200 bg-indigo-50 p-3 flex items-start gap-2">
          <PackageCheck className="h-4 w-4 text-indigo-700 shrink-0 mt-0.5" />
          <p className="text-sm text-indigo-800 font-medium">
            {t("readyForPickupCallout")}
          </p>
        </div>
      )}

      {/* Delivery / Pickup Code */}
      {needsCode && codeData?.code && (
        <div className="mt-3 rounded-xl border bg-muted/30 p-4 space-y-2">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
            {order.status === "READY_FOR_PICKUP" ? "Pickup Code" : "Delivery Code"}
          </p>
          {order.status === "READY_FOR_PICKUP" && order.deliveryType === "PICKPOINT" ? (
            <PickupCodeDisplay code={codeData.code} />
          ) : (
            <div className="flex items-center gap-3">
              <span className="font-mono text-2xl tracking-[0.5em] text-foreground select-all">
                {showCode ? codeData.code : "••••••"}
              </span>
              <Button variant="ghost" size="sm" onClick={() => setShowCode((v) => !v)}>
                {showCode ? "Hide" : "Show Code"}
              </Button>
            </div>
          )}
          {codeData.expiresAt && (
            <p className="text-xs text-muted-foreground">
              Valid until {new Date(codeData.expiresAt).toLocaleString()}
            </p>
          )}
        </div>
      )}

      {/* Status timeline */}
      <OrderTimeline status={order.status} deliveryType={order.deliveryType ?? null} />

      {/* Shipment details (SHIPPED / DELIVERED, courier data present) */}
      {order.courierName && (order.status === "SHIPPED" || order.status === "DELIVERED") && (
        <div className="mt-3 rounded-xl border bg-muted/30 p-3 space-y-1.5 text-xs">
          <div className="flex items-center gap-1.5 text-muted-foreground font-semibold uppercase tracking-wide mb-1">
            <Truck className="h-3.5 w-3.5" />
            {t("shipmentTitle")}
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">{t("courierLabel")}</span>
            <span className="font-medium">{order.courierName}</span>
          </div>
          {order.trackingNumber && (
            <div className="flex justify-between">
              <span className="text-muted-foreground">{t("trackingLabel")}</span>
              <span className="font-medium font-mono">{order.trackingNumber}</span>
            </div>
          )}
          {order.estimatedDeliveryDate && (
            <div className="flex justify-between">
              <span className="text-muted-foreground">{t("estimatedDeliveryLabel")}</span>
              <span className="font-medium">
                {new Date(order.estimatedDeliveryDate).toLocaleDateString()}
              </span>
            </div>
          )}
        </div>
      )}

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
