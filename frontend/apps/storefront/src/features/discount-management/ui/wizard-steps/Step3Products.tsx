"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { SkuPicker } from "@/entities/product";
import type { DiscountWizardState } from "../DiscountCreationWizard";
import { ShoppingBag, PackageCheck, CheckCircle2, AlertTriangle } from "lucide-react";
import { fetchDiscountOverlaps, fetchDiscountTypes } from "../../api";
import type { CampaignSummary } from "../../model/types";

interface Props {
  state: DiscountWizardState;
  update: (patch: Partial<DiscountWizardState>) => void;
  submitted: boolean;
  editId?: number;
}

const MAX_VISIBLE_WARNINGS = 8;

export function Step3Products({ state, update, submitted, editId }: Props) {
  const count = state.selectedCodes.length;
  const noCodesError = submitted && count === 0;

  const { data: overlaps } = useQuery({
    queryKey: ["discount-overlaps"],
    queryFn: fetchDiscountOverlaps,
    staleTime: 60_000,
  });

  const { data: types } = useQuery({
    queryKey: ["discount-types"],
    queryFn: fetchDiscountTypes,
  });

  const warnings = useMemo(() => {
    if (!overlaps || !types || state.typeId === 0) return [];
    const myType = types.find((t) => t.id === state.typeId);
    if (!myType) return [];
    const myRank = myType.rank;

    const result: {
      skuCode: string;
      otherCampaign: CampaignSummary;
      willWin: boolean;
      myRank: number;
    }[] = [];

    for (const code of state.selectedCodes) {
      const row = overlaps.find((o) => o.skuCode === code);
      if (!row) continue;
      const allCampaigns = [row.winningCampaign, ...row.losingCampaigns];
      const others = editId !== undefined
        ? allCampaigns.filter((c) => c.id !== editId)
        : allCampaigns;
      if (others.length === 0) continue;
      // Pick the strongest rival: lowest rank, then lowest id
      const strongest = others.reduce((best, c) => {
        if (c.type.rank < best.type.rank) return c;
        if (c.type.rank === best.type.rank && c.id < best.id) return c;
        return best;
      });
      result.push({
        skuCode: code,
        otherCampaign: strongest,
        willWin: myRank < strongest.type.rank,
        myRank,
      });
    }
    return result;
  }, [overlaps, types, state.selectedCodes, state.typeId, editId]);

  const visibleWarnings = warnings.slice(0, MAX_VISIBLE_WARNINGS);
  const extraWarnings = warnings.length - visibleWarnings.length;

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

      {/* Overlap warning callout — advisory only, does not block Next */}
      {warnings.length > 0 && (
        <div className="rounded-xl border border-amber-200 bg-amber-50/60 p-4 space-y-2">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
            <p className="text-sm font-semibold text-amber-800">
              {warnings.length} SKU{warnings.length !== 1 ? "s" : ""} overlap with another active campaign
            </p>
          </div>
          <div className="space-y-1 pl-6">
            {visibleWarnings.map((w) => (
              <p key={w.skuCode} className="text-xs text-amber-700">
                <span className="font-mono font-medium">{w.skuCode}</span>
                {" — already in "}
                <span className="font-medium">{w.otherCampaign.title}</span>
                {` (rank ${w.otherCampaign.type.rank}). Yours (rank ${w.myRank}) would `}
                <span className={w.willWin ? "font-semibold text-green-700" : "font-semibold text-red-700"}>
                  {w.willWin ? "win" : "lose"}
                </span>
                {"."}
              </p>
            ))}
            {extraWarnings > 0 && (
              <p className="text-xs text-amber-600/70">+{extraWarnings} more</p>
            )}
          </div>
        </div>
      )}

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
