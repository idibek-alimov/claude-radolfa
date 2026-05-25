"use client";

import { useState } from "react";
import { Loader2, MapPin, Phone } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { OrderStatusBadge } from "@/entities/order/ui/OrderStatusBadge";
import { useMarkCollected } from "@/features/courier/api";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";
import type { CourierOrder } from "@/entities/user";
import { DeliveryConfirmModal } from "./DeliveryConfirmModal";
import { DeliveryAttemptModal } from "./DeliveryAttemptModal";

interface Props {
  order: CourierOrder;
}

export function CourierOrderCard({ order }: Props) {
  const collect = useMarkCollected();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [attemptOpen, setAttemptOpen] = useState(false);

  function handleCollect() {
    collect.mutate(order.orderId, {
      onError: (err) => toast.error(getErrorMessage(err, "Failed to mark collected")),
    });
  }

  const isCancelled = order.status === "CANCELLED";

  return (
    <div className="rounded-xl border bg-card p-5 shadow-sm space-y-4">
      {/* Top row */}
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold text-foreground">Order #{order.orderId}</span>
        <OrderStatusBadge status={order.status} />
      </div>

      {/* Customer + phone */}
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium">{order.customerFirstName}</span>
        <a href={`tel:${order.customerPhone}`}>
          <Button variant="outline" size="sm" className="gap-1.5">
            <Phone className="h-3.5 w-3.5" />
            {order.customerPhone}
          </Button>
        </a>
      </div>

      {/* Address */}
      <div className="flex items-start gap-2">
        <MapPin className="h-4 w-4 text-muted-foreground shrink-0 mt-0.5" />
        <div className="space-y-0.5">
          <p className="text-sm">{order.deliveryAddress}</p>
          {order.preferredTimeWindow && (
            <p className="text-xs text-muted-foreground">Window: {order.preferredTimeWindow}</p>
          )}
        </div>
      </div>

      {/* Meta */}
      <p className="text-xs text-muted-foreground">
        {order.totalItemCount} item{order.totalItemCount !== 1 ? "s" : ""}
        {order.totalWeightKg != null && ` · ${order.totalWeightKg.toFixed(2)} kg`}
        {order.deliveryAttemptCount > 0 && (
          <span className="ml-2 text-amber-700 font-medium">
            {order.deliveryAttemptCount} attempt{order.deliveryAttemptCount !== 1 ? "s" : ""}
          </span>
        )}
      </p>

      {/* Primary action */}
      {isCancelled ? (
        <div className="rounded-lg bg-rose-50 border border-rose-200 px-4 py-3 text-sm text-rose-700 text-center font-medium">
          Cancelled — no action required
        </div>
      ) : order.status === "SHIPPED" ? (
        <Button
          className="w-full h-12"
          onClick={handleCollect}
          disabled={collect.isPending}
        >
          {collect.isPending ? (
            <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Updating…</>
          ) : (
            "I've Collected This Order"
          )}
        </Button>
      ) : order.status === "OUT_FOR_DELIVERY" ? (
        <Button className="w-full h-12" onClick={() => setConfirmOpen(true)}>
          Confirm Delivery
        </Button>
      ) : order.status === "DELIVERY_ATTEMPTED" ? (
        <Button
          variant="outline"
          className="w-full h-12 border-amber-300 text-amber-800 hover:bg-amber-50"
          onClick={() => setAttemptOpen(true)}
        >
          Record Another Attempt
        </Button>
      ) : null}

      <DeliveryConfirmModal
        open={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        orderId={order.orderId}
      />
      <DeliveryAttemptModal
        open={attemptOpen}
        onClose={() => setAttemptOpen(false)}
        orderId={order.orderId}
      />
    </div>
  );
}
