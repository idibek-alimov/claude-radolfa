"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { useTranslations } from "next-intl";
import {
  Check,
  CircleCheckBig,
  Clock,
  PackageCheck,
  Truck,
  X,
  type LucideIcon,
} from "lucide-react";
import { useMyOrderDetail } from "../api";
import { useDeliveryCode } from "@/entities/order/api";
import { OrderStatusBadge } from "@/entities/order";
import { PickupCodeDisplay } from "@/entities/order/ui/PickupCodeDisplay";
import { formatPrice, formatDate } from "@/shared/lib/format";
import { getErrorMessage } from "@/shared/lib";
import { Skeleton } from "@/shared/ui/skeleton";
import { Badge } from "@/shared/ui/badge";
import type { OrderStatus } from "@/entities/order/model/types";
import type { MyOrderDetail } from "../types";

/* ── Timeline constants (mirrors OrderHistoryCard pattern) ─────── */

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

const RETURN_STATUSES = new Set<OrderStatus>([
  "RETURN_INITIATED",
  "RETURNED_TO_WAREHOUSE",
  "REFUNDED",
  "RECALL_REQUESTED",
]);

function getStepIndex(status: string, steps: readonly string[]): number {
  const idx = steps.indexOf(status);
  return idx === -1 ? 0 : idx;
}

/* ── OrderTimelineSection ──────────────────────────────────────── */

function OrderTimelineSection({ order }: { order: MyOrderDetail }) {
  const t = useTranslations("profile");

  if (RETURN_STATUSES.has(order.status as OrderStatus)) return null;

  const isCancelled = order.status === "CANCELLED";
  if (isCancelled) {
    return (
      <div className="flex items-center gap-2">
        <X className="h-4 w-4 text-destructive" />
        <span className="text-xs text-destructive font-medium">
          {t("statusCancelled")}
        </span>
      </div>
    );
  }

  const steps = order.deliveryType === "PICKPOINT" ? PICKPOINT_STEPS : HOME_STEPS;
  const currentStep = getStepIndex(order.status, steps);

  return (
    <div className="rounded-xl border bg-card p-5">
      <div className="flex items-center w-full overflow-hidden">
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
    </div>
  );
}

/* ── PickupCodeSection ─────────────────────────────────────────── */

function PickupCodeSection({ order }: { order: MyOrderDetail }) {
  const { data } = useDeliveryCode(order.id, order.status === "READY_FOR_PICKUP");
  if (!data?.code) return null;
  return (
    <section className="rounded-xl border bg-card p-5">
      <PickupCodeDisplay
        code={data.code}
        pickpointName={order.pickpointName}
        pickpointAddress={order.pickpointAddress}
      />
    </section>
  );
}

/* ── CustomerOrderDetailPage ───────────────────────────────────── */

export function CustomerOrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const t = useTranslations("orderDetail");
  const { data: order, isLoading, isError, error } = useMyOrderDetail(id);

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full rounded-xl" />
        <Skeleton className="h-40 w-full rounded-xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="rounded-lg border border-destructive/20 bg-destructive/5 p-6 text-center text-sm text-destructive">
        {getErrorMessage(error)}
      </div>
    );
  }

  if (!order) {
    return (
      <div className="py-20 text-center text-sm text-muted-foreground">
        {t("notFound")}
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6 py-6">
      {/* Breadcrumb */}
      <Link
        href="/profile?tab=orders"
        className="text-sm text-muted-foreground hover:text-foreground"
      >
        ← {t("backToOrders")}
      </Link>

      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">
          {t("orderNumber")} #{order.id}
        </h1>
        <OrderStatusBadge status={order.status} />
      </div>

      {/* Return / Recall Banners */}
      {(order.status === "RETURN_INITIATED" || order.status === "RETURNED_TO_WAREHOUSE") && (
        <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          {t("returnInProgressBanner")}
        </div>
      )}
      {order.status === "RECALL_REQUESTED" && (
        <div className="rounded-lg border border-orange-200 bg-orange-50 px-4 py-3 text-sm text-orange-800">
          {t("recallRequestedBanner")}
        </div>
      )}

      {/* Timeline */}
      <OrderTimelineSection order={order} />

      {/* Items */}
      <section className="rounded-xl border bg-card p-5">
        <h2 className="mb-3 text-sm font-semibold">{t("items")}</h2>
        <div className="divide-y">
          {order.items.map((item, i) => (
            <div key={item.skuId ?? i} className="flex items-center gap-3 py-3">
              {item.imageUrl && (
                <Image
                  src={item.imageUrl}
                  alt={item.productName}
                  width={48}
                  height={48}
                  unoptimized
                  className="rounded-md object-cover"
                />
              )}
              <div className="flex-1">
                <p className="text-sm font-medium">{item.productName}</p>
                {item.sizeLabel && (
                  <p className="text-xs text-muted-foreground">{item.sizeLabel}</p>
                )}
              </div>
              <div className="text-right text-sm">
                <p>{formatPrice(item.price)}</p>
                <p className="text-xs text-muted-foreground">× {item.quantity}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Pricing */}
      <section className="rounded-xl border bg-card p-5">
        <h2 className="mb-3 text-sm font-semibold">{t("pricing")}</h2>
        <div className="space-y-2 text-sm">
          {order.loyaltyPointsRedeemed > 0 && (
            <div className="flex justify-between text-green-700">
              <span>{t("loyaltyDiscount")}</span>
              <span>− {order.loyaltyPointsRedeemed} pts</span>
            </div>
          )}
          <div className="flex justify-between font-semibold">
            <span>{t("total")}</span>
            <span>{formatPrice(order.totalAmount)}</span>
          </div>
        </div>
      </section>

      {/* Pickup code (READY_FOR_PICKUP state) */}
      {order.deliveryType === "PICKPOINT" && order.status === "READY_FOR_PICKUP" && (
        <PickupCodeSection order={order} />
      )}

      {/* Pickpoint info (non-QR states) */}
      {order.deliveryType === "PICKPOINT" &&
        order.status !== "READY_FOR_PICKUP" &&
        (order.pickpointName || order.pickpointAddress) && (
          <section className="rounded-xl border bg-card p-5">
            <h2 className="mb-2 text-sm font-semibold">{t("pickupPoint")}</h2>
            {order.pickpointName && <p className="text-sm">{order.pickpointName}</p>}
            {order.pickpointAddress && (
              <p className="text-xs text-muted-foreground">{order.pickpointAddress}</p>
            )}
          </section>
        )}

      {/* Courier info */}
      {order.deliveryType === "HOME" && order.trackingNumber && (
        <section className="rounded-xl border bg-card p-5">
          <h2 className="mb-2 text-sm font-semibold">{t("courierInfo")}</h2>
          <p className="text-sm">{t("trackingNumber")}: {order.trackingNumber}</p>
          {order.estimatedDeliveryDate && (
            <p className="text-xs text-muted-foreground">
              {t("estimatedDelivery")}: {formatDate(order.estimatedDeliveryDate)}
            </p>
          )}
        </section>
      )}

      {/* Return info — only rendered when backend extends OrderDto with customerReturn */}
      {order.customerReturn && (
        <section className="rounded-xl border bg-card p-5">
          <h2 className="mb-3 text-sm font-semibold">{t("returnInfo")}</h2>
          <div className="space-y-2 text-sm">
            <div className="flex items-center gap-2">
              <span className="text-muted-foreground">{t("returnStatus")}:</span>
              <Badge variant="outline">{order.customerReturn.status}</Badge>
            </div>
            <p>
              <span className="text-muted-foreground">{t("returnReason")}:</span>{" "}
              {order.customerReturn.reason}
            </p>
            {order.customerReturn.refundAmount != null && (
              <p className="font-medium text-green-700">
                {t("refundAmount")}: {formatPrice(order.customerReturn.refundAmount)}
              </p>
            )}
          </div>
        </section>
      )}

      {/* Activity timestamps — only rendered when backend extends OrderDto */}
      {(order.shippedAt || order.deliveredAt || order.cancelledAt || order.refundedAt) && (
        <section className="rounded-xl border bg-card p-5">
          <h2 className="mb-3 text-sm font-semibold">{t("activity")}</h2>
          <div className="space-y-1 text-xs text-muted-foreground">
            <p>{t("ordered")}: {formatDate(order.createdAt)}</p>
            {order.shippedAt && <p>{t("shipped")}: {formatDate(order.shippedAt)}</p>}
            {order.deliveredAt && <p>{t("delivered")}: {formatDate(order.deliveredAt)}</p>}
            {order.cancelledAt && <p>{t("cancelled")}: {formatDate(order.cancelledAt)}</p>}
            {order.refundedAt && <p>{t("refunded")}: {formatDate(order.refundedAt)}</p>}
          </div>
        </section>
      )}
    </div>
  );
}
