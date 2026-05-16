"use client";

import { useState } from "react";
import { Calendar } from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { OrderStatusBadge } from "@/entities/order/ui/OrderStatusBadge";
import type { PickpointOrder } from "@/entities/user";
import { PickupConfirmModal } from "./PickupConfirmModal";

interface Props {
  order: PickpointOrder;
}

function ExpiryChip({ days }: { days: number }) {
  const cls =
    days <= 1
      ? "bg-rose-50 text-rose-700 border border-rose-200"
      : days <= 3
      ? "bg-amber-50 text-amber-700 border border-amber-200"
      : "bg-muted text-muted-foreground";

  const label = days <= 1 ? "Expires today!" : `${days} days left`;

  return (
    <span className={cn("inline-block rounded-full px-2.5 py-0.5 text-xs font-medium", cls)}>
      {label}
    </span>
  );
}

export function PickpointOrderCard({ order }: Props) {
  const [confirmOpen, setConfirmOpen] = useState(false);
  const isAwaiting = order.status === "READY_FOR_PICKUP";

  return (
    <div className="rounded-xl border bg-card p-5 shadow-sm space-y-4">
      {/* Top row */}
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold">Order #{order.orderId}</span>
        <OrderStatusBadge status={order.status} />
      </div>

      {/* Customer */}
      <p className="text-sm font-medium">{order.customerFirstName}</p>

      {/* Ready date */}
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <Calendar className="h-3.5 w-3.5 shrink-0" />
        <span>Ready since {new Date(order.readyAt).toLocaleDateString()}</span>
      </div>

      {/* Expiry chip — only for awaiting orders */}
      {isAwaiting && <ExpiryChip days={order.daysUntilExpiry} />}

      {/* Action */}
      {isAwaiting && (
        <Button className="w-full h-12" onClick={() => setConfirmOpen(true)}>
          Confirm Pickup
        </Button>
      )}

      <PickupConfirmModal
        open={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        orderId={order.orderId}
      />
    </div>
  );
}
