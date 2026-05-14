"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchDiscountTypes } from "../../api";
import { DiscountWizardPreview } from "../DiscountWizardPreview";
import type { DiscountWizardState } from "../DiscountCreationWizard";
import {
  Settings2,
  CalendarDays,
  ShoppingBag,
  Tag,
  Percent,
  Palette,
  Clock,
  List,
  Users,
  Layers,
  SlidersHorizontal,
} from "lucide-react";

function ReviewCell({
  icon: Icon,
  label,
  value,
}: {
  icon: React.ElementType;
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="flex items-start gap-3 py-5 px-6">
      <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary/10 shrink-0 mt-0.5">
        <Icon className="h-3.5 w-3.5 text-primary" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs text-muted-foreground">{label}</p>
        <div className="text-sm font-medium text-foreground mt-0.5">{value}</div>
      </div>
    </div>
  );
}

function getDurationLabel(from: string, to: string): string | null {
  if (!from || !to) return null;
  const diff = new Date(to).getTime() - new Date(from).getTime();
  if (diff <= 0) return null;
  const days = Math.round(diff / (1000 * 60 * 60 * 24));
  return days === 1 ? "1 day" : `${days} days`;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface Props {
  state: DiscountWizardState;
  isEdit: boolean;
}

export function Step4Review({ state, isEdit }: Props) {
  const { data: types = [] } = useQuery({
    queryKey: ["discount-types"],
    queryFn: fetchDiscountTypes,
  });

  const selectedType = types.find((t) => t.id === state.typeId);
  const typeName = selectedType?.name ?? "—";
  const stackingPolicy = selectedType?.stackingPolicy ?? "BEST_WINS";
  const duration = getDurationLabel(state.validFrom, state.validUpto);

  const targetSummary = (() => {
    if (state.targetMode === "SKU") {
      return state.selectedCodes.length > 0 ? (
        <span>
          <span className="font-bold">{state.selectedCodes.length}</span>{" "}
          SKU{state.selectedCodes.length !== 1 ? "s" : ""}
        </span>
      ) : (
        <span className="text-muted-foreground">None</span>
      );
    }
    if (state.targetMode === "CATEGORY") {
      return state.selectedCategoryIds.length > 0 ? (
        <span>
          <span className="font-bold">{state.selectedCategoryIds.length}</span>{" "}
          {state.selectedCategoryIds.length !== 1 ? "categories" : "category"}
          {state.includeDescendants ? " + subcategories" : ""}
        </span>
      ) : (
        <span className="text-muted-foreground">None</span>
      );
    }
    // SEGMENT
    if (!state.selectedSegment) return <span className="text-muted-foreground">None</span>;
    if (state.selectedSegment.type === "NEW_CUSTOMER") return <span>New customers</span>;
    return <span>Loyalty tier (ID {state.selectedSegment.tierId})</span>;
  })();

  const TargetIcon =
    state.targetMode === "CATEGORY" ? Tag : state.targetMode === "SEGMENT" ? Users : List;

  return (
    <div className="flex-1 flex flex-col space-y-10">
      {/* Heading */}
      <div>
        <h2 className="text-xl font-semibold tracking-tight">
          {isEdit ? "Review Changes" : "Review & Submit"}
        </h2>
        <p className="text-sm text-muted-foreground mt-1">
          Everything looks correct? Hit{" "}
          <strong>{isEdit ? "Save Changes" : "Create Discount"}</strong> to apply.
        </p>
      </div>

      {/* Shopper preview */}
      {state.title && state.colorHex.length === 6 && (
        <div className="rounded-xl border border-border bg-muted/30 p-6">
          <p className="text-xs font-medium text-muted-foreground mb-4 uppercase tracking-wide">
            Shopper Preview
          </p>
          <div className="flex items-start gap-6">
            <DiscountWizardPreview
              title={state.title}
              colorHex={state.colorHex}
              discountValue={state.discountValue}
              amountType={state.amountType}
            />
            <p className="text-sm text-muted-foreground max-w-sm pt-2">
              This is how the product card looks to shoppers during the campaign.
            </p>
          </div>
        </div>
      )}

      {/* Summary card — 2-column grid */}
      <div
        className="rounded-xl border border-border bg-card overflow-hidden"
        style={
          state.colorHex.length === 6
            ? { borderLeftWidth: "4px", borderLeftColor: `#${state.colorHex}` }
            : undefined
        }
      >
        {/* Header */}
        <div className="px-6 py-5 border-b border-border">
          <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground">
            Campaign Summary
          </p>
        </div>

        {/* Grid rows */}
        <div className="divide-y divide-border">
          <div className="grid grid-cols-2 divide-x divide-border">
            <ReviewCell
              icon={Settings2}
              label="Title"
              value={state.title || "—"}
            />
            <ReviewCell
              icon={Tag}
              label="Discount Type"
              value={
                <span className="inline-flex items-center gap-1.5 rounded-md bg-muted px-2 py-0.5 text-xs font-medium">
                  {typeName}
                </span>
              }
            />
          </div>

          <div className="grid grid-cols-2 divide-x divide-border">
            <ReviewCell
              icon={Percent}
              label="Discount"
              value={
                <span className="text-rose-600 dark:text-rose-400 font-bold tabular-nums">
                  −{state.discountValue}{state.amountType === "FIXED" ? " TJS" : "%"}
                </span>
              }
            />
            <ReviewCell
              icon={Palette}
              label="Badge Color"
              value={
                <span className="flex items-center gap-2">
                  <span
                    className="inline-block h-4 w-4 rounded border border-border"
                    style={{ backgroundColor: `#${state.colorHex}` }}
                  />
                  <span className="font-mono text-xs">#{state.colorHex.toUpperCase()}</span>
                </span>
              }
            />
          </div>

          <div className="grid grid-cols-2 divide-x divide-border">
            <ReviewCell
              icon={CalendarDays}
              label="Valid from"
              value={state.validFrom ? formatDate(state.validFrom) : "—"}
            />
            <ReviewCell
              icon={CalendarDays}
              label="Valid until"
              value={state.validUpto ? formatDate(state.validUpto) : "—"}
            />
          </div>

          <div className="grid grid-cols-2 divide-x divide-border">
            <ReviewCell
              icon={Clock}
              label="Duration"
              value={duration ?? "—"}
            />
            <ReviewCell
              icon={TargetIcon}
              label="Target"
              value={targetSummary}
            />
          </div>

          <div className="grid grid-cols-2 divide-x divide-border">
            <ReviewCell
              icon={SlidersHorizontal}
              label="Min basket"
              value={state.minBasketAmount ? `${Number(state.minBasketAmount).toLocaleString()} TJS` : "—"}
            />
            <ReviewCell
              icon={ShoppingBag}
              label="Usage cap"
              value={
                state.usageCapTotal || state.usageCapPerCustomer ? (
                  <span className="space-x-2">
                    {state.usageCapTotal && <span>Total: {state.usageCapTotal}</span>}
                    {state.usageCapPerCustomer && <span>Per customer: {state.usageCapPerCustomer}</span>}
                  </span>
                ) : "—"
              }
            />
          </div>

          <div className="grid grid-cols-2 divide-x divide-border">
            <ReviewCell
              icon={Tag}
              label="Coupon code"
              value={
                state.couponCode ? (
                  <span className="font-mono text-xs bg-muted px-2 py-0.5 rounded">
                    {state.couponCode}
                  </span>
                ) : "—"
              }
            />
            <ReviewCell
              icon={Layers}
              label="Stacking policy"
              value={
                <span className="text-xs font-medium">
                  {stackingPolicy === "STACKABLE" ? "Stackable" : "Best wins"}
                </span>
              }
            />
          </div>
        </div>
      </div>
    </div>
  );
}
