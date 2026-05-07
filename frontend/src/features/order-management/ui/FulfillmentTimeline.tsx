"use client";

import { cn } from "@/shared/lib/utils";
import type { OrderStatus } from "@/entities/order";

interface TimelineNode {
  label: string;
  timestamp: string | null;
  reached: boolean;
  isCurrent: boolean;
  isCancelled?: boolean;
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
    : node.reached
    ? "bg-primary"
    : "border-2 border-muted bg-background";

  return (
    <div className="flex flex-col items-center gap-1 min-w-[72px]">
      <div className="relative flex items-center justify-center">
        <div className={cn("h-6 w-6 rounded-full", circleClass)} />
        {node.isCurrent && !node.isCancelled && (
          <div className="absolute h-6 w-6 rounded-full bg-primary opacity-30 animate-ping" />
        )}
      </div>
      <span
        className={cn(
          "text-[11px] font-semibold text-center leading-tight",
          node.isCancelled
            ? "text-rose-600"
            : node.reached
            ? "text-foreground"
            : "text-muted-foreground"
        )}
      >
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
  deliveryType: "HOME" | "PICKPOINT" | null;
}

export function FulfillmentTimeline({
  status,
  createdAt,
  shippedAt,
  deliveredAt,
  cancelledAt,
  deliveryType,
}: Props) {
  const isPaid      = ["PAID", "SHIPPED", "READY_FOR_PICKUP", "DELIVERED"].includes(status);
  const isShipped   = ["SHIPPED", "READY_FOR_PICKUP", "DELIVERED"].includes(status);
  const isDelivered = status === "DELIVERED";
  const isCancelled = !!cancelledAt;

  const shippedLabel = deliveryType === "PICKPOINT" ? "Ready" : "Shipped";

  const nodes: TimelineNode[] = [
    { label: "Created",      timestamp: createdAt,   reached: true,        isCurrent: status === "PENDING" && !isCancelled },
    { label: "Paid",         timestamp: null,         reached: isPaid,      isCurrent: status === "PAID" && !isCancelled },
    { label: shippedLabel,   timestamp: shippedAt,    reached: isShipped,   isCurrent: (status === "SHIPPED" || status === "READY_FOR_PICKUP") && !isCancelled },
    { label: "Delivered",    timestamp: deliveredAt,  reached: isDelivered, isCurrent: isDelivered },
  ];

  if (isCancelled) {
    nodes.push({ label: "Cancelled", timestamp: cancelledAt, reached: true, isCurrent: false, isCancelled: true });
  }

  return (
    <div className="rounded-xl border bg-card p-5">
      <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-4">
        Fulfillment Timeline
      </p>
      <div className="flex items-start">
        {nodes.map((node, i) => (
          <div key={i} className="contents">
            <Node node={node} />
            {i < nodes.length - 1 && (
              <div
                className={cn(
                  "flex-1 h-0.5 mt-3 mx-1",
                  nodes[i + 1].isCancelled
                    ? "bg-rose-200"
                    : nodes[i + 1].reached
                    ? "bg-primary/40"
                    : "bg-border"
                )}
              />
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
