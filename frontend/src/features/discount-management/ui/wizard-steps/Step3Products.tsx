"use client";

import { SkuPicker } from "@/entities/product";
import type { DiscountWizardState } from "../DiscountCreationWizard";
import { ShoppingBag, PackageCheck, CheckCircle2 } from "lucide-react";

interface Props {
  state: DiscountWizardState;
  update: (patch: Partial<DiscountWizardState>) => void;
  submitted: boolean;
}

export function Step3Products({ state, update, submitted }: Props) {
  const count = state.selectedCodes.length;
  const noCodesError = submitted && count === 0;

  return (
    <div className="flex-1 flex flex-col space-y-8">
      <div>
        <h2 className="text-xl font-semibold tracking-tight">Product Selection</h2>
        <p className="text-sm text-muted-foreground mt-1">
          Search for products and select the sizes (SKUs) to include in this campaign.
        </p>
      </div>

      {/* Summary strip */}
      <div
        className={`flex items-center gap-3 rounded-xl border px-6 py-4 transition-colors ${
          count > 0
            ? "border-primary/30 bg-primary/5"
            : noCodesError
            ? "border-destructive/40 bg-destructive/5"
            : "border-border bg-muted/30"
        }`}
      >
        <div
          className={`flex h-10 w-10 items-center justify-center rounded-full shrink-0 ${
            count > 0 ? "bg-primary/15" : "bg-muted"
          }`}
        >
          {count > 0 ? (
            <PackageCheck className="h-5 w-5 text-primary" />
          ) : (
            <ShoppingBag className="h-5 w-5 text-muted-foreground" />
          )}
        </div>
        <div>
          {count > 0 ? (
            <p className="text-sm font-semibold text-foreground flex items-center gap-1.5">
              <CheckCircle2 className="h-4 w-4 text-primary shrink-0" />
              <span className="text-primary font-bold tabular-nums">{count}</span>{" "}
              SKU{count !== 1 ? "s" : ""} selected
            </p>
          ) : (
            <p className={`text-sm ${noCodesError ? "text-destructive" : "text-muted-foreground"}`}>
              {noCodesError
                ? "At least one SKU must be selected to continue."
                : "No SKUs selected yet — search below to add products."}
            </p>
          )}
        </div>
      </div>

      {/* SkuPicker — flex-1 wrapper gives it a defined height to fill */}
      <div className="flex-1 min-h-0 flex flex-col">
        <SkuPicker
          selectedCodes={state.selectedCodes}
          onSelectionChange={(codes) => update({ selectedCodes: codes })}
        />
      </div>
    </div>
  );
}
