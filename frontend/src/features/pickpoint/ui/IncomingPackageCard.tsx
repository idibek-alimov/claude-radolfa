"use client";

import { Package } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/shared/ui/button";
import { useConfirmArrival } from "@/features/pickpoint/api";
import { getErrorMessage } from "@/shared/lib";
import type { PickpointOrder } from "@/entities/user";

interface Props {
  order: PickpointOrder;
}

export function IncomingPackageCard({ order }: Props) {
  const confirm = useConfirmArrival();

  function handleConfirmArrival() {
    confirm.mutate(order.orderId, {
      onSuccess: () =>
        toast.success("Arrival confirmed — pickup code sent to customer."),
      onError: (err) =>
        toast.error(getErrorMessage(err, "Failed to confirm arrival")),
    });
  }

  return (
    <div className="rounded-xl border bg-card p-5 shadow-sm space-y-4">
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold">Order #{order.orderId}</span>
        <Package className="h-4 w-4 text-muted-foreground" />
      </div>

      <p className="text-sm font-medium">{order.customerFirstName}</p>

      <p className="text-xs text-muted-foreground">
        En route — awaiting arrival confirmation
      </p>

      <Button
        className="w-full h-12"
        onClick={handleConfirmArrival}
        disabled={confirm.isPending}
      >
        {confirm.isPending ? "Confirming…" : "Confirm Arrival"}
      </Button>
    </div>
  );
}
