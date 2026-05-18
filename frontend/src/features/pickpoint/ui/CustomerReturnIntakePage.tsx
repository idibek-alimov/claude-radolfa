"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Search } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { useLookUpOrderForReturn, useCreateCustomerReturn } from "@/features/pickpoint/api";
import { getErrorMessage } from "@/shared/lib";
import type { ReturnReason, ReturnableItem } from "@/entities/pickpoint";

const RETURN_REASONS: { value: ReturnReason; label: string }[] = [
  { value: "DAMAGED",        label: "Damaged" },
  { value: "WRONG_ITEM",     label: "Wrong item" },
  { value: "NOT_AS_DESCRIBED", label: "Not as described" },
  { value: "CHANGED_MIND",   label: "Changed mind" },
  { value: "OTHER",          label: "Other" },
];

interface ItemSelection {
  checked: boolean;
  quantity: number;
  reason: ReturnReason | "";
  notes: string;
}

function classifyLookupError(err: unknown): string {
  const msg = getErrorMessage(err, "").toLowerCase();
  if (msg.includes("not delivered") || msg.includes("ordernotdelivered")) {
    return "This order hasn't been delivered yet.";
  }
  if (msg.includes("not at pickpoint") || msg.includes("ordernotatpickpoint")) {
    return "This order isn't assigned to your pickpoint.";
  }
  return "Order not found. Check the ID and try again.";
}

export function CustomerReturnIntakePage() {
  const router = useRouter();
  const [orderIdInput, setOrderIdInput] = useState("");
  const [submittedId, setSubmittedId]   = useState<number | null>(null);
  const [selections, setSelections]     = useState<Record<number, ItemSelection>>({});
  const [returnNotes, setReturnNotes]   = useState("");

  const lookup = useLookUpOrderForReturn(submittedId);
  const create = useCreateCustomerReturn();

  function handleFind() {
    const id = parseInt(orderIdInput, 10);
    if (isNaN(id) || id <= 0) return;
    setSelections({});
    setReturnNotes("");
    setSubmittedId(id);
  }

  function toggleItem(item: ReturnableItem) {
    setSelections((prev) => {
      const existing = prev[item.orderItemId];
      if (existing?.checked) {
        const next = { ...prev };
        delete next[item.orderItemId];
        return next;
      }
      return {
        ...prev,
        [item.orderItemId]: { checked: true, quantity: 1, reason: "", notes: "" },
      };
    });
  }

  function updateSelection(
    orderItemId: number,
    field: keyof ItemSelection,
    value: string | number | boolean,
  ) {
    setSelections((prev) => ({
      ...prev,
      [orderItemId]: { ...prev[orderItemId], [field]: value },
    }));
  }

  const selectedItems = Object.entries(selections).filter(([, s]) => s.checked);
  const canSubmit =
    selectedItems.length > 0 &&
    selectedItems.every(([, s]) => s.reason !== "" && s.quantity >= 1);

  function handleSubmit() {
    if (!submittedId || !canSubmit) return;

    const payload = {
      orderId: submittedId,
      notes: returnNotes.trim() || undefined,
      items: selectedItems.map(([id, s]) => ({
        orderItemId: Number(id),
        quantity: s.quantity,
        reason: s.reason as ReturnReason,
        notes: s.notes.trim() || undefined,
      })),
    };

    create.mutate(payload, {
      onSuccess: () => {
        toast.success("Customer return logged successfully.");
        router.push("/pickpoint");
      },
      onError: (err) =>
        toast.error(getErrorMessage(err, "Failed to log return")),
    });
  }

  const orderItems = lookup.data?.items ?? [];

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-2xl mx-auto px-4 py-6 space-y-6">
        {/* Header */}
        <div className="flex items-center gap-3">
          <Link href="/pickpoint" className="text-muted-foreground hover:text-foreground transition-colors">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <h1 className="text-xl font-bold">Log Customer Return</h1>
        </div>

        {/* Step 1 — Order lookup */}
        <div className="rounded-xl border bg-card p-5 space-y-4">
          <p className="text-sm font-semibold">Step 1 — Find Order</p>
          <div className="flex gap-2">
            <Input
              type="number"
              inputMode="numeric"
              placeholder="Order ID"
              value={orderIdInput}
              onChange={(e) => setOrderIdInput(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleFind()}
              className="flex-1"
            />
            <Button
              onClick={handleFind}
              disabled={!orderIdInput || lookup.isFetching}
              variant="outline"
            >
              <Search className="h-4 w-4 mr-1.5" />
              {lookup.isFetching ? "Searching…" : "Find"}
            </Button>
          </div>

          {lookup.isError && (
            <p className="text-destructive text-sm">{classifyLookupError(lookup.error)}</p>
          )}

          {lookup.isFetching && (
            <Skeleton className="h-24 w-full rounded-lg" />
          )}

          {lookup.data && (
            <div className="bg-muted/30 rounded-lg p-3 text-sm">
              <p className="font-medium">Order #{lookup.data.orderId}</p>
              <p className="text-muted-foreground text-xs mt-0.5">
                {orderItems.length} item{orderItems.length !== 1 ? "s" : ""}
              </p>
            </div>
          )}
        </div>

        {/* Step 2 — Item selection */}
        {lookup.data && orderItems.length > 0 && (
          <div className="rounded-xl border bg-card p-5 space-y-4">
            <p className="text-sm font-semibold">Step 2 — Select Items to Return</p>

            <div className="space-y-3">
              {orderItems.map((item) => {
                const sel = selections[item.orderItemId];
                const checked = sel?.checked ?? false;

                return (
                  <div
                    key={item.orderItemId}
                    className="rounded-lg border p-4 space-y-3"
                  >
                    <label className="flex items-start gap-3 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => toggleItem(item)}
                        className="mt-0.5 accent-primary"
                      />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium">{item.productName}</p>
                        <p className="text-xs text-muted-foreground">
                          {item.skuCode ?? ""} · Ordered qty: {item.quantity}
                        </p>
                      </div>
                    </label>

                    {checked && (
                      <div className="space-y-3 pl-6">
                        <div className="flex items-center gap-2">
                          <label className="text-xs text-muted-foreground w-20">Quantity</label>
                          <Input
                            type="number"
                            min={1}
                            max={item.quantity}
                            value={sel.quantity}
                            onChange={(e) =>
                              updateSelection(
                                item.orderItemId,
                                "quantity",
                                Math.min(item.quantity, Math.max(1, parseInt(e.target.value) || 1)),
                              )
                            }
                            className="w-20 h-8 text-sm"
                          />
                        </div>

                        <div className="flex items-center gap-2">
                          <label className="text-xs text-muted-foreground w-20">Reason</label>
                          <Select
                            value={sel.reason}
                            onValueChange={(v) =>
                              updateSelection(item.orderItemId, "reason", v)
                            }
                          >
                            <SelectTrigger className="flex-1 h-8 text-sm">
                              <SelectValue placeholder="Select reason…" />
                            </SelectTrigger>
                            <SelectContent>
                              {RETURN_REASONS.map((r) => (
                                <SelectItem key={r.value} value={r.value}>
                                  {r.label}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>

                        <Input
                          placeholder="Item notes (optional)"
                          value={sel.notes}
                          onChange={(e) =>
                            updateSelection(item.orderItemId, "notes", e.target.value)
                          }
                          className="h-8 text-sm"
                        />
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            <div className="space-y-2">
              <label className="text-xs text-muted-foreground">Return notes (optional)</label>
              <textarea
                value={returnNotes}
                onChange={(e) => setReturnNotes(e.target.value)}
                maxLength={500}
                rows={2}
                placeholder="Overall notes about this return…"
                className="w-full rounded-lg border bg-background px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-ring"
              />
            </div>

            <Button
              className="w-full h-12"
              onClick={handleSubmit}
              disabled={!canSubmit || create.isPending}
            >
              {create.isPending ? "Logging Return…" : "Log Return"}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
