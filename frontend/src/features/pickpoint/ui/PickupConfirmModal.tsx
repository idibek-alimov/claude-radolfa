"use client";

import { useState, useEffect } from "react";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { useConfirmPickup } from "@/features/pickpoint/api";
import { getErrorMessage } from "@/shared/lib";

interface Props {
  open: boolean;
  onClose: () => void;
  orderId: number;
}

function classifyError(err: unknown): string {
  const msg = getErrorMessage(err, "").toLowerCase();
  if (msg.includes("mismatch") || msg.includes("incorrect")) return "Incorrect code. Try again.";
  if (msg.includes("expired")) return "This code has expired. Ask an admin to regenerate it.";
  if (msg.includes("max") || msg.includes("attempts")) return "Too many failed attempts. Contact an admin.";
  if (msg.includes("already used") || msg.includes("used")) return "This code has already been used.";
  return getErrorMessage(err, "Failed to confirm pickup");
}

export function PickupConfirmModal({ open, onClose, orderId }: Props) {
  const confirm = useConfirmPickup();
  const [code, setCode]   = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) { setCode(""); setError(null); }
  }, [open]);

  function handleConfirm() {
    if (code.length !== 6) { setError("Enter the 6-digit code."); return; }
    setError(null);
    confirm.mutate(
      { orderId, code },
      {
        onSuccess: () => { toast.success("Pickup confirmed!"); onClose(); },
        onError: (err) => setError(classifyError(err)),
      }
    );
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Confirm Pickup</DialogTitle>
        </DialogHeader>

        <div className="py-3 space-y-4">
          <p className="text-sm text-muted-foreground text-center">
            Ask the customer for their 6-digit pickup code.
          </p>
          <Input
            type="text"
            inputMode="numeric"
            maxLength={6}
            pattern="[0-9]*"
            autoFocus
            value={code}
            onChange={(e) => { setCode(e.target.value.replace(/\D/g, "")); setError(null); }}
            onKeyDown={(e) => e.key === "Enter" && handleConfirm()}
            className="text-center text-2xl tracking-[0.5em] h-14 font-mono"
            placeholder="000000"
          />
          {error && <p className="text-destructive text-sm text-center">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={confirm.isPending}>Cancel</Button>
          <Button onClick={handleConfirm} disabled={confirm.isPending || code.length !== 6}>
            {confirm.isPending ? "Confirming…" : "Confirm"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
