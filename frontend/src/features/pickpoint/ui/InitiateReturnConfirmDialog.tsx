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
import { useInitiateReturn } from "@/features/pickpoint/api";
import { getErrorMessage } from "@/shared/lib";

interface Props {
  open: boolean;
  onClose: () => void;
  orderId: number;
}

export function InitiateReturnConfirmDialog({ open, onClose, orderId }: Props) {
  const initiate = useInitiateReturn();

  function handleConfirm() {
    initiate.mutate(orderId, {
      onSuccess: () => {
        toast.success(`Return initiated for Order #${orderId}. Customer has been notified.`);
        onClose();
      },
      onError: (err) =>
        toast.error(getErrorMessage(err, "Failed to initiate return")),
    });
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Initiate Return — Order #{orderId}</DialogTitle>
        </DialogHeader>

        <p className="text-sm text-muted-foreground py-2">
          This will mark Order #{orderId} as <strong>Return Initiated</strong> and notify
          the customer by SMS that their uncollected package is being returned to the warehouse.
        </p>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={initiate.isPending}>
            Cancel
          </Button>
          <Button
            variant="destructive"
            onClick={handleConfirm}
            disabled={initiate.isPending}
          >
            {initiate.isPending ? "Initiating…" : "Initiate Return"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
