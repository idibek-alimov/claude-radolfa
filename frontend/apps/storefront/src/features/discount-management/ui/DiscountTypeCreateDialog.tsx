"use client";

import { useState, useEffect } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { createDiscountType, updateDiscountType } from "../api";
import type { DiscountType, StackingPolicy } from "../model/types";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";
import { Loader2, Tag, ListOrdered, Layers } from "lucide-react";
import { RadioGroup, RadioGroupItem } from "@/shared/ui/radio-group";
import { cn } from "@/shared/lib/utils";

const ACCENT_COLORS = [
  "#F97316",
  "#8B5CF6",
  "#06B6D4",
  "#10B981",
  "#F59E0B",
  "#EF4444",
];

function SectionHeading({
  icon: Icon,
  title,
}: {
  icon: React.ElementType;
  title: string;
}) {
  return (
    <div className="flex items-center gap-2 pb-2 border-b border-border">
      <Icon className="h-4 w-4 text-muted-foreground" />
      <h3 className="text-sm font-semibold text-foreground">{title}</h3>
    </div>
  );
}

interface DiscountTypeCreateDialogProps {
  open: boolean;
  onClose: () => void;
  editTarget?: DiscountType;
}

export function DiscountTypeCreateDialog({
  open,
  onClose,
  editTarget,
}: DiscountTypeCreateDialogProps) {
  const [name, setName] = useState("");
  const [rank, setRank] = useState(0);
  const [stackingPolicy, setStackingPolicy] = useState<StackingPolicy>("BEST_WINS");
  const qc = useQueryClient();

  useEffect(() => {
    if (open) {
      setName(editTarget?.name ?? "");
      setRank(editTarget?.rank ?? 0);
      setStackingPolicy(editTarget?.stackingPolicy ?? "BEST_WINS");
    }
  }, [open, editTarget]);

  const mutation = useMutation({
    mutationFn: () =>
      editTarget
        ? updateDiscountType(editTarget.id, { name: name.trim(), rank, stackingPolicy })
        : createDiscountType({ name: name.trim(), rank, stackingPolicy }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["discount-types"] });
      toast.success(editTarget ? "Type updated" : "Discount type created");
      onClose();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const isEditing = !!editTarget;
  const canSubmit = name.trim().length > 0 && !mutation.isPending;
  const accentColor = ACCENT_COLORS[rank % ACCENT_COLORS.length];

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-[620px]">
        <DialogHeader>
          <DialogTitle>
            {isEditing ? "Edit Discount Type" : "New Discount Type"}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-6 py-2">
          {/* ─── Type Identity ─── */}
          <section className="space-y-4">
            <SectionHeading icon={Tag} title="Type Identity" />
            <div className="space-y-2">
              <Label htmlFor="type-name">Name</Label>
              <Input
                id="type-name"
                autoFocus
                value={name}
                onChange={(e) => setName(e.target.value)}
                onKeyDown={(e) =>
                  e.key === "Enter" && canSubmit && mutation.mutate()
                }
                placeholder="e.g. Flash Sale"
                maxLength={128}
                className="h-11"
              />
            </div>
          </section>

          {/* ─── Priority ─── */}
          <section className="space-y-4">
            <SectionHeading icon={ListOrdered} title="Priority" />
            <div className="space-y-2">
              <Label htmlFor="type-rank">Priority rank</Label>
              <p className="text-xs text-muted-foreground -mt-1">
                Lower number = applied first when multiple discounts compete on the
                same SKU.
              </p>
              <Input
                id="type-rank"
                type="number"
                value={rank}
                onChange={(e) => setRank(Number(e.target.value))}
                min={0}
                className="w-28 h-11"
              />
            </div>
          </section>

          {/* ─── Stacking Policy ─── */}
          <section className="space-y-4">
            <SectionHeading icon={Layers} title="Stacking Policy" />
            <p className="text-xs text-muted-foreground -mt-2">
              Controls what happens when multiple discounts of this type apply to the same SKU.
            </p>
            <RadioGroup
              value={stackingPolicy}
              onValueChange={(v) => setStackingPolicy(v as StackingPolicy)}
              className="grid grid-cols-2 gap-3"
            >
              {(
                [
                  {
                    value: "BEST_WINS" as const,
                    title: "Best wins",
                    body: "Only the highest-ranked discount of this type applies; others are evicted.",
                  },
                  {
                    value: "STACKABLE" as const,
                    title: "Stackable",
                    body: "Combines multiplicatively with other STACKABLE discounts on the same SKU.",
                  },
                ] as const
              ).map(({ value, title, body }) => (
                <label
                  key={value}
                  htmlFor={`stacking-${value}`}
                  className={cn(
                    "flex flex-col gap-1.5 rounded-xl border p-4 cursor-pointer transition-colors",
                    stackingPolicy === value
                      ? "border-primary/40 bg-primary/5"
                      : "border-border bg-muted/30 hover:bg-muted/50"
                  )}
                >
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value={value} id={`stacking-${value}`} />
                    <span className="text-sm font-medium">{title}</span>
                  </div>
                  <p className="text-xs text-muted-foreground pl-6">{body}</p>
                </label>
              ))}
            </RadioGroup>
          </section>

          {/* ─── Live preview chip ─── */}
          {name.trim().length > 0 && (
            <div className="rounded-xl border border-border bg-muted/30 p-4">
              <p className="text-xs font-medium text-muted-foreground mb-3 uppercase tracking-wide">
                Card preview
              </p>
              <span
                className="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold text-white"
                style={{ backgroundColor: accentColor }}
              >
                <span className="h-1.5 w-1.5 rounded-full bg-white/60 shrink-0" />
                {name.trim()} · Priority {rank}
              </span>
            </div>
          )}
        </div>

        {/* ─── Footer ─── */}
        <div className="flex items-center justify-between pt-2">
          <Button
            variant="ghost"
            onClick={onClose}
            disabled={mutation.isPending}
          >
            Cancel
          </Button>
          <Button onClick={() => mutation.mutate()} disabled={!canSubmit}>
            {mutation.isPending && (
              <Loader2 className="mr-2 h-3.5 w-3.5 animate-spin" />
            )}
            {isEditing ? "Save changes" : "Create type"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
