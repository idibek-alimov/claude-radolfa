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
import { useActivePickpoints } from "@/entities/pickpoint/api";
import { useRedirectToPickpoint } from "@/features/fleet/api";
import { getErrorMessage } from "@/shared/lib";

interface Props {
  open: boolean;
  onClose: () => void;
  orderId: number;
}

export function RedirectToPickpointDialog({ open, onClose, orderId }: Props) {
  const { data: pickpoints = [] } = useActivePickpoints();
  const redirect = useRedirectToPickpoint();
  const [pickpointId, setPickpointId] = useState<number | null>(null);
  const [selectError, setSelectError] = useState(false);

  function handleClose() {
    setPickpointId(null);
    setSelectError(false);
    onClose();
  }

  function handleConfirm() {
    if (!pickpointId) {
      setSelectError(true);
      return;
    }
    const selected = pickpoints.find((p) => p.id === pickpointId);
    redirect.mutate(
      { orderId, pickpointId },
      {
        onSuccess: () => {
          toast.success(`Redirected to ${selected?.name ?? "pickpoint"}`);
          handleClose();
        },
        onError: (err) => toast.error(getErrorMessage(err, "Failed to redirect order")),
      }
    );
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && handleClose()}>
      <DialogContent className="max-w-sm" style={{ "--overlay-bg": "bg-black/30" } as React.CSSProperties}>
        <DialogHeader>
          <DialogTitle>Redirect to Pickpoint</DialogTitle>
        </DialogHeader>

        <div className="py-2 space-y-2">
          <Label className="text-sm">
            Select pickpoint <span className="text-destructive">*</span>
          </Label>
          <Select
            value={pickpointId?.toString() ?? ""}
            onValueChange={(v) => {
              setPickpointId(Number(v));
              setSelectError(false);
            }}
          >
            <SelectTrigger className={selectError ? "border-destructive" : ""}>
              <SelectValue placeholder="Choose a pickpoint…" />
            </SelectTrigger>
            <SelectContent>
              {pickpoints.map((p) => (
                <SelectItem key={p.id} value={p.id.toString()}>
                  {p.name}
                  {p.address && (
                    <span className="ml-1 text-muted-foreground text-xs">— {p.address}</span>
                  )}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {selectError && (
            <p className="text-destructive text-xs">Please select a pickpoint.</p>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={redirect.isPending}>
            Cancel
          </Button>
          <Button onClick={handleConfirm} disabled={redirect.isPending}>
            {redirect.isPending ? "Redirecting…" : "Confirm Redirect"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
