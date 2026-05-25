"use client";

import { useState } from "react";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import {
  DELIVERY_ATTEMPT_REASONS,
  useMarkAttempted,
  type DeliveryAttemptReason,
} from "@/features/courier/api";
import { getErrorMessage } from "@/shared/lib";

const REASON_LABELS: Record<DeliveryAttemptReason, string> = {
  NO_ANSWER:        "No answer / not home",
  WRONG_ADDRESS:    "Wrong address",
  CUSTOMER_REFUSED: "Customer refused",
  PACKAGE_DAMAGED:  "Package damaged",
  OTHER:            "Other",
};

interface Props {
  open: boolean;
  onClose: () => void;
  orderId: number;
}

export function DeliveryAttemptModal({ open, onClose, orderId }: Props) {
  const attempt = useMarkAttempted();
  const [reason, setReason] = useState<DeliveryAttemptReason | "">("");

  function handleClose() { setReason(""); onClose(); }

  function handleSubmit() {
    if (!reason) return;
    attempt.mutate(
      { orderId, reason },
      {
        onSuccess: () => { toast.success("Delivery attempt recorded."); handleClose(); },
        onError: (err) => toast.error(getErrorMessage(err, "Failed to record attempt")),
      }
    );
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && handleClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Record Delivery Attempt</DialogTitle>
        </DialogHeader>

        <div className="py-3 space-y-3">
          <Label className="text-sm">
            Why couldn&apos;t you complete the delivery? <span className="text-destructive">*</span>
          </Label>
          <Select
            value={reason}
            onValueChange={(v) => setReason(v as DeliveryAttemptReason)}
          >
            <SelectTrigger>
              <SelectValue placeholder="Select a reason…" />
            </SelectTrigger>
            <SelectContent>
              {DELIVERY_ATTEMPT_REASONS.map((r) => (
                <SelectItem key={r} value={r}>{REASON_LABELS[r]}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={attempt.isPending}>Cancel</Button>
          <Button onClick={handleSubmit} disabled={attempt.isPending || !reason}>
            {attempt.isPending ? "Recording…" : "Record Attempt"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
