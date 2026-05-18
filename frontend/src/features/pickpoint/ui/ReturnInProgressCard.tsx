"use client";

import { useState } from "react";
import { Clock } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/shared/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/shared/ui/dialog";
import { useConfirmReturnedToWarehouse } from "@/features/pickpoint/api";
import { getErrorMessage } from "@/shared/lib";
import type { PickpointOrder } from "@/entities/user";

interface Props {
  order: PickpointOrder;
}

export function ReturnInProgressCard({ order }: Props) {
  const [confirmOpen, setConfirmOpen] = useState(false);
  const confirm = useConfirmReturnedToWarehouse();

  function handleConfirm() {
    confirm.mutate(order.orderId, {
      onSuccess: () => {
        toast.success(`Order #${order.orderId} handed off to carrier.`);
        setConfirmOpen(false);
      },
      onError: (err) =>
        toast.error(getErrorMessage(err, "Failed to confirm carrier pickup")),
    });
  }

  return (
    <>
      <div className="rounded-xl border bg-card p-5 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <span className="text-sm font-semibold">Order #{order.orderId}</span>
          <span className="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200">
            <Clock className="h-3 w-3" />
            Return initiated
          </span>
        </div>

        <p className="text-sm font-medium">{order.customerFirstName}</p>

        <p className="text-xs text-muted-foreground">
          Awaiting 3PL carrier pickup
        </p>

        <Button
          className="w-full h-12"
          onClick={() => setConfirmOpen(true)}
        >
          Confirm Carrier Pickup
        </Button>
      </div>

      <Dialog open={confirmOpen} onOpenChange={(o) => !o && setConfirmOpen(false)}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Confirm Carrier Pickup — Order #{order.orderId}</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground py-2">
            Confirm that the 3PL carrier has collected Order #{order.orderId} for return to warehouse.
            This action cannot be undone.
          </p>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmOpen(false)} disabled={confirm.isPending}>
              Cancel
            </Button>
            <Button onClick={handleConfirm} disabled={confirm.isPending}>
              {confirm.isPending ? "Confirming…" : "Confirm Pickup"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
