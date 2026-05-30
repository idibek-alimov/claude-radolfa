"use client";

import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { getErrorMessage } from "@/shared/lib";
import type { CustomerReturn } from "@/entities/pickpoint";
import { useApproveRefund } from "../api";

interface Props {
  open: boolean;
  onClose: () => void;
  customerReturn: CustomerReturn;
}

function formatAmount(r: CustomerReturn): string {
  const amount =
    typeof r.totalRefundAmount === "object"
      ? (r.totalRefundAmount as { amount: number }).amount
      : Number(r.totalRefundAmount);
  return amount.toFixed(2);
}

export function ApproveRefundDialog({ open, onClose, customerReturn: r }: Props) {
  const approve = useApproveRefund();

  function handleConfirm() {
    approve.mutate(r.id, {
      onSuccess: () => {
        toast.success("Refund approved — customer notified by SMS.");
        onClose();
      },
      onError: (err) => toast.error("Refund failed: " + getErrorMessage(err)),
    });
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Approve Refund for Order #{r.orderId}</DialogTitle>
        </DialogHeader>

        <p className="text-sm text-muted-foreground">
          Approve refund of{" "}
          <span className="font-semibold text-foreground">{formatAmount(r)} TJS</span>{" "}
          for Order #{r.orderId}? This will initiate the refund via the payment gateway.
        </p>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={approve.isPending}>
            Cancel
          </Button>
          <Button onClick={handleConfirm} disabled={approve.isPending}>
            {approve.isPending ? "Processing…" : "Confirm Refund"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
