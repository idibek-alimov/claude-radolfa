"use client";

import { useState } from "react";
import { toast } from "sonner";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from "@/shared/ui/alert-dialog";
import { Label } from "@/shared/ui/label";
import { Textarea } from "@/shared/ui/textarea";
import { useCancelOrder } from "@/entities/order";
import { getErrorMessage } from "@/shared/lib";

interface CancelOrderModalProps {
  open: boolean;
  onClose: () => void;
  orderId: number;
}

export function CancelOrderModal({ open, onClose, orderId }: CancelOrderModalProps) {
  const cancelOrder = useCancelOrder();
  const [reason, setReason] = useState("");

  function handleConfirm() {
    cancelOrder.mutate(
      { orderId, reason: reason.trim() || undefined },
      {
        onSuccess: () => {
          toast.success("Order cancelled.");
          setReason("");
          onClose();
        },
        onError: (err) => toast.error(getErrorMessage(err, "Failed to cancel order.")),
      }
    );
  }

  return (
    <AlertDialog open={open} onOpenChange={(o) => !o && onClose()}>
      <AlertDialogContent className="max-w-sm">
        <AlertDialogHeader>
          <AlertDialogTitle>Cancel Order #{orderId}?</AlertDialogTitle>
          <AlertDialogDescription>
            This will mark the order as CANCELLED and cannot be undone. The customer will be
            notified.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="space-y-1.5 py-2">
          <Label htmlFor="cancel-reason" className="text-sm">Reason</Label>
          <Textarea
            id="cancel-reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Optional — visible to staff"
            rows={3}
          />
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel onClick={onClose}>Keep Order</AlertDialogCancel>
          <AlertDialogAction
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            onClick={handleConfirm}
            disabled={cancelOrder.isPending}
          >
            {cancelOrder.isPending ? "Cancelling…" : "Cancel Order"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
