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
import { Textarea } from "@/shared/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { useCourierList, useBulkReassign } from "@/features/fleet/api";
import { getErrorMessage } from "@/shared/lib";

interface Props {
  open: boolean;
  onClose: () => void;
}

export function BulkReassignDialog({ open, onClose }: Props) {
  const { data: couriers = [] } = useCourierList();
  const reassign = useBulkReassign();

  const [rawOrderIds, setRawOrderIds]       = useState("");
  const [newCourierId, setNewCourierId]     = useState<number | null>(null);
  const [courierError, setCourierError]     = useState(false);
  const [orderIdsError, setOrderIdsError]   = useState("");

  function handleClose() {
    setRawOrderIds("");
    setNewCourierId(null);
    setCourierError(false);
    setOrderIdsError("");
    onClose();
  }

  function parseOrderIds(): number[] | null {
    const parts = rawOrderIds.split(/[\s,]+/).filter(Boolean);
    const nums = parts.map(Number);
    if (nums.some(isNaN)) return null;
    return nums;
  }

  function handleConfirm() {
    let valid = true;

    const orderIds = parseOrderIds();
    if (!orderIds || orderIds.length === 0) {
      setOrderIdsError("Enter one or more valid order IDs (comma or space separated).");
      valid = false;
    } else {
      setOrderIdsError("");
    }

    if (!newCourierId) {
      setCourierError(true);
      valid = false;
    }

    if (!valid || !orderIds || !newCourierId) return;

    const dest = couriers.find((c) => c.id === newCourierId);
    reassign.mutate(
      { orderIds, newCourierId },
      {
        onSuccess: () => {
          toast.success(`${orderIds.length} order(s) reassigned to ${dest?.name ?? "courier"}.`);
          handleClose();
        },
        onError: (err) => toast.error(getErrorMessage(err, "Failed to reassign orders")),
      }
    );
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && handleClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Bulk Reassign Orders</DialogTitle>
        </DialogHeader>

        <div className="py-2 space-y-4">
          <div className="space-y-1.5">
            <Label className="text-sm">
              Order IDs <span className="text-destructive">*</span>
            </Label>
            <Textarea
              value={rawOrderIds}
              onChange={(e) => setRawOrderIds(e.target.value)}
              placeholder="e.g. 42, 43, 44"
              rows={3}
              className={orderIdsError ? "border-destructive" : ""}
            />
            {orderIdsError && <p className="text-destructive text-xs">{orderIdsError}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className="text-sm">
              Assign to courier <span className="text-destructive">*</span>
            </Label>
            <Select
              value={newCourierId?.toString() ?? ""}
              onValueChange={(v) => { setNewCourierId(Number(v)); setCourierError(false); }}
            >
              <SelectTrigger className={courierError ? "border-destructive" : ""}>
                <SelectValue placeholder="Select courier…" />
              </SelectTrigger>
              <SelectContent>
                {couriers.map((c) => (
                  <SelectItem key={c.id} value={c.id.toString()}>
                    {c.name}
                    {c.vehicleType && (
                      <span className="ml-1 text-muted-foreground text-xs">— {c.vehicleType}</span>
                    )}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {courierError && <p className="text-destructive text-xs">Please select a courier.</p>}
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={reassign.isPending}>Cancel</Button>
          <Button onClick={handleConfirm} disabled={reassign.isPending}>
            {reassign.isPending ? "Reassigning…" : "Confirm Reassign"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
