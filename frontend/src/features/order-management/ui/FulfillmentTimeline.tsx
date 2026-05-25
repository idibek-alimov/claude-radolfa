"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/shared/lib/utils";
import type { OrderStatus } from "@/entities/order";

interface TimelineNode {
  label: string;
  timestamp: string | null;
  reached: boolean;
  isCurrent: boolean;
  isCancelled?: boolean;
  isRefunded?: boolean;
  isOutForDelivery?: boolean;
  isAttempted?: boolean;
}

function formatTs(iso: string) {
  return new Date(iso).toLocaleString("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function Node({ node }: { node: TimelineNode }) {
  const circleClass = node.isCancelled
    ? "bg-rose-500"
    : node.isRefunded
    ? "bg-violet-500"
    : node.isAttempted
    ? "bg-amber-500"
    : node.isOutForDelivery
    ? "bg-blue-500"
    : node.reached
    ? "bg-primary"
    : "border-2 border-muted bg-background";

  const labelClass = node.isCancelled
    ? "text-rose-600"
    : node.isRefunded
    ? "text-violet-700"
    : node.isAttempted
    ? "text-amber-700"
    : node.isOutForDelivery
    ? "text-blue-700"
    : node.reached
    ? "text-foreground"
    : "text-muted-foreground";

  return (
    <div className="flex flex-col items-center gap-1 min-w-[72px]">
      <div className="relative flex items-center justify-center">
        <div className={cn("h-6 w-6 rounded-full", circleClass)} />
        {node.isCurrent && !node.isCancelled && !node.isRefunded && !node.isAttempted && (
          <div className="absolute h-6 w-6 rounded-full bg-primary opacity-30 animate-ping" />
        )}
        {node.isCurrent && node.isAttempted && (
          <div className="absolute h-6 w-6 rounded-full bg-amber-500 opacity-30 animate-ping" />
        )}
      </div>
      <span className={cn("text-[11px] font-semibold text-center leading-tight", labelClass)}>
        {node.label}
      </span>
      {node.timestamp ? (
        <span className="text-[10px] text-muted-foreground text-center leading-tight">
          {formatTs(node.timestamp)}
        </span>
      ) : (
        <span className="text-[10px] text-transparent select-none">—</span>
      )}
    </div>
  );
}

interface Props {
  status: OrderStatus;
  createdAt: string;
  shippedAt: string | null;
  deliveredAt: string | null;
  cancelledAt: string | null;
  refundedAt: string | null;
  deliveryType: "HOME" | "PICKPOINT" | null;
  outForDeliveryAt?: string | null;
  deliveryAttemptedAt?: string | null;
  deliveryAttemptCount?: number;
  deliveryAttemptReason?: string | null;
}

const ATTEMPT_REASON_LABELS: Record<string, string> = {
  NO_ANSWER:        "No answer / not home",
  WRONG_ADDRESS:    "Wrong address",
  CUSTOMER_REFUSED: "Customer refused",
  PACKAGE_DAMAGED:  "Package damaged",
  OTHER:            "Other",
};

export function FulfillmentTimeline({
  status,
  createdAt,
  shippedAt,
  deliveredAt,
  cancelledAt,
  refundedAt,
  deliveryType,
  outForDeliveryAt,
  deliveryAttemptedAt,
  deliveryAttemptCount = 0,
  deliveryAttemptReason,
}: Props) {
  const t = useTranslations("manage.orders.timeline");

  const isHome      = deliveryType !== "PICKPOINT";
  const isPaid      = ["PAID", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERY_ATTEMPTED",
                       "READY_FOR_PICKUP", "DELIVERED", "REFUNDED"].includes(status);
  const isShipped   = ["SHIPPED", "OUT_FOR_DELIVERY", "DELIVERY_ATTEMPTED",
                       "READY_FOR_PICKUP", "DELIVERED", "REFUNDED"].includes(status);
  const isOutFor    = ["OUT_FOR_DELIVERY", "DELIVERY_ATTEMPTED", "DELIVERED", "REFUNDED"].includes(status);
  const isDelivered = status === "DELIVERED" || status === "REFUNDED";
  const isCancelled = !!cancelledAt;
  const isRefunded  = !!refundedAt;
  const hasAttempt  = deliveryAttemptCount > 0 || status === "DELIVERY_ATTEMPTED";

  const shippedLabel = deliveryType === "PICKPOINT" ? t("ready") : t("shipped");

  const nodes: TimelineNode[] = isHome
    ? [
        { label: t("created"),       timestamp: createdAt,        reached: true,       isCurrent: status === "PENDING" && !isCancelled && !isRefunded },
        { label: t("paid"),          timestamp: null,             reached: isPaid,      isCurrent: status === "PAID" && !isCancelled && !isRefunded },
        { label: shippedLabel,       timestamp: shippedAt,        reached: isShipped,   isCurrent: status === "SHIPPED" && !isCancelled && !isRefunded },
        { label: "Out for delivery", timestamp: outForDeliveryAt ?? null, reached: isOutFor, isCurrent: status === "OUT_FOR_DELIVERY", isOutForDelivery: isOutFor },
        { label: t("delivered"),     timestamp: deliveredAt,      reached: isDelivered, isCurrent: status === "DELIVERED" },
      ]
    : [
        { label: t("created"),     timestamp: createdAt,  reached: true,        isCurrent: status === "PENDING" && !isCancelled && !isRefunded },
        { label: t("paid"),        timestamp: null,       reached: isPaid,      isCurrent: status === "PAID" && !isCancelled && !isRefunded },
        { label: shippedLabel,     timestamp: shippedAt,  reached: isShipped,   isCurrent: status === "READY_FOR_PICKUP" && !isCancelled && !isRefunded },
        { label: t("delivered"),   timestamp: deliveredAt, reached: isDelivered, isCurrent: status === "DELIVERED" },
      ];

  if (isCancelled) {
    nodes.push({ label: t("cancelled"), timestamp: cancelledAt, reached: true, isCurrent: false, isCancelled: true });
  }

  if (isRefunded) {
    nodes.push({ label: t("refunded"), timestamp: refundedAt, reached: true, isCurrent: false, isRefunded: true });
  }

  const connectorClass = (next: TimelineNode) =>
    next.isCancelled
      ? "bg-rose-200"
      : next.isRefunded
      ? "bg-violet-200"
      : next.isOutForDelivery && next.reached
      ? "bg-blue-200"
      : next.reached
      ? "bg-primary/40"
      : "bg-border";

  return (
    <div className="rounded-xl border bg-card p-5 space-y-4">
      <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
        {t("title")}
      </p>

      {/* Main timeline */}
      <div className="flex items-start">
        {nodes.map((node, i) => (
          <div key={i} className="contents">
            <Node node={node} />
            {i < nodes.length - 1 && (
              <div className={cn("flex-1 h-0.5 mt-3 mx-1", connectorClass(nodes[i + 1]))} />
            )}
          </div>
        ))}
      </div>

      {/* Delivery attempt branch */}
      {isHome && hasAttempt && (
        <div className="ml-4 pl-4 border-l-2 border-amber-200">
          <div className="flex items-start gap-3">
            <div className="flex flex-col items-center gap-1 min-w-[72px]">
              <div className="h-6 w-6 rounded-full bg-amber-500" />
              <span className="text-[11px] font-semibold text-amber-700">
                Attempted
                {deliveryAttemptCount > 0 && (
                  <span className="ml-1 rounded-full bg-amber-100 text-amber-700 px-1 text-[10px]">
                    ×{deliveryAttemptCount}
                  </span>
                )}
              </span>
              {deliveryAttemptedAt && (
                <span className="text-[10px] text-muted-foreground">{formatTs(deliveryAttemptedAt)}</span>
              )}
            </div>
            {deliveryAttemptReason && (
              <p className="text-xs text-amber-800 mt-1">
                {ATTEMPT_REASON_LABELS[deliveryAttemptReason] ?? deliveryAttemptReason}
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
