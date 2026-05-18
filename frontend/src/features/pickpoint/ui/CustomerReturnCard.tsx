"use client";

import { toast } from "sonner";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { useConfirmCustomerReturnSent } from "@/features/pickpoint/api";
import { getErrorMessage } from "@/shared/lib";
import type { CustomerReturn } from "@/entities/pickpoint";

interface Props {
  customerReturn: CustomerReturn;
}

export function CustomerReturnCard({ customerReturn: r }: Props) {
  const confirm = useConfirmCustomerReturnSent();

  const isSent = r.status === "SENT_TO_WAREHOUSE";

  const reasons = [...new Set(r.items.map((i) => i.reason))].join(", ");

  function handleConfirmSent() {
    confirm.mutate(r.id, {
      onSuccess: () =>
        toast.success(`Return #${r.id} confirmed — sent to warehouse.`),
      onError: (err) =>
        toast.error(getErrorMessage(err, "Failed to confirm return sent")),
    });
  }

  return (
    <div className="rounded-xl border bg-card p-5 shadow-sm space-y-3">
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold">Return #{r.id}</span>
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium",
            isSent
              ? "bg-green-50 text-green-700 border border-green-200"
              : "bg-amber-50 text-amber-700 border border-amber-200",
          )}
        >
          <span className={cn("h-1.5 w-1.5 rounded-full", isSent ? "bg-green-600" : "bg-amber-500")} />
          {isSent ? "Sent to warehouse" : "Awaiting carrier"}
        </span>
      </div>

      <div className="text-xs text-muted-foreground space-y-0.5">
        <p>Order #{r.orderId} · {r.customerName ?? "—"}</p>
        <p>{r.items.length} item{r.items.length !== 1 ? "s" : ""} · {reasons}</p>
        <p>Received {new Date(r.receivedAt).toLocaleDateString()}</p>
      </div>

      {r.notes && (
        <p className="text-xs text-muted-foreground italic">&ldquo;{r.notes}&rdquo;</p>
      )}

      {!isSent && (
        <Button
          className="w-full"
          onClick={handleConfirmSent}
          disabled={confirm.isPending}
        >
          {confirm.isPending ? "Confirming…" : "Confirm Sent to Carrier"}
        </Button>
      )}
    </div>
  );
}
