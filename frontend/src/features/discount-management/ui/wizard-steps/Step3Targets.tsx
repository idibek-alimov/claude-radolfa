"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { SkuPicker } from "@/entities/product";
import { useLoyaltyTiers } from "@/entities/loyalty";
import { flattenTree } from "@/features/category-management";
import { fetchCategoryTree } from "@/entities/product/api";
import type { DiscountWizardState, SegmentSelection } from "../DiscountCreationWizard";
import {
  ShoppingBag,
  PackageCheck,
  CheckCircle2,
  AlertTriangle,
  List,
  Tag,
  Users,
  Search,
  Layers,
} from "lucide-react";
import { fetchDiscountOverlaps, fetchDiscountTypes } from "../../api";
import type { CampaignSummary } from "../../model/types";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import { Checkbox } from "@/shared/ui/checkbox";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { RadioGroup, RadioGroupItem } from "@/shared/ui/radio-group";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { Switch } from "@/shared/ui/switch";
import { cn } from "@/shared/lib/utils";

type TargetMode = "SKU" | "CATEGORY" | "SEGMENT";

interface Props {
  state: DiscountWizardState;
  update: (patch: Partial<DiscountWizardState>) => void;
  submitted: boolean;
  editId?: number;
}

const MAX_VISIBLE_WARNINGS = 8;

// ── SKU tab ────────────────────────────────────────────────────────

function SkuTab({
  state,
  update,
  submitted,
  editId,
}: {
  state: DiscountWizardState;
  update: (patch: Partial<DiscountWizardState>) => void;
  submitted: boolean;
  editId?: number;
}) {
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
    const myAmount = state.discountValue;

    const result: {
      skuCode: string;
      otherCampaign: CampaignSummary;
      willWin: boolean;
      myRank: number;
      rankTied: boolean;
    }[] = [];

    for (const code of state.selectedCodes) {
      const row = overlaps.find((o) => o.skuCode === code);
      if (!row) continue;
      const allCampaigns = [row.winningCampaign, ...row.losingCampaigns];
      const others =
        editId !== undefined ? allCampaigns.filter((c) => c.id !== editId) : allCampaigns;
      if (others.length === 0) continue;

      // Mirror backend: rank ASC → amountValue DESC → id ASC
      const strongest = others.reduce((best, c) => {
        if (c.type.rank < best.type.rank) return c;
        if (c.type.rank > best.type.rank) return best;
        if (c.amountValue > best.amountValue) return c;
        if (c.amountValue < best.amountValue) return best;
        return c.id < best.id ? c : best;
      });

      // Predict whether the campaign being created would beat `strongest`
      let willWin: boolean;
      if (myRank < strongest.type.rank) {
        willWin = true;
      } else if (myRank > strongest.type.rank) {
        willWin = false;
      } else if (myAmount > strongest.amountValue) {
        willWin = true;
      } else if (myAmount < strongest.amountValue) {
        willWin = false;
      } else {
        // Equal rank and equal amount — new campaign always gets a higher ID → loses
        willWin = false;
      }

      result.push({
        skuCode: code,
        otherCampaign: strongest,
        willWin,
        myRank,
        rankTied: myRank === strongest.type.rank,
      });
    }
    return result;
  }, [overlaps, types, state.selectedCodes, state.typeId, state.discountValue, editId]);

  const visibleWarnings = warnings.slice(0, MAX_VISIBLE_WARNINGS);
  const extraWarnings = warnings.length - visibleWarnings.length;

  return (
    <div className="flex flex-col space-y-6">
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
            <p
              className={`text-sm ${
                noCodesError ? "text-destructive" : "text-muted-foreground"
              }`}
            >
              {noCodesError
                ? "At least one SKU must be selected to continue."
                : "No SKUs selected yet — search below to add products."}
            </p>
          )}
        </div>
      </div>

      {/* Overlap warning callout */}
      {warnings.length > 0 && (
        <div className="rounded-xl border border-amber-200 bg-amber-50/60 p-4 space-y-2">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
            <p className="text-sm font-semibold text-amber-800">
              {warnings.length} SKU{warnings.length !== 1 ? "s" : ""} overlap with another active
              campaign
            </p>
          </div>
          <div className="space-y-1 pl-6">
            {visibleWarnings.map((w) => {
              const theirAmt = `${w.otherCampaign.amountValue}${w.otherCampaign.amountType === "PERCENT" ? "%" : " fixed"}`;
              const myAmt = `${state.discountValue}${state.amountType === "PERCENT" ? "%" : " fixed"}`;
              const reason = w.rankTied
                ? ` (rank ${w.myRank} tie — ${myAmt} vs their ${theirAmt})`
                : ` (rank ${w.myRank} vs ${w.otherCampaign.type.rank})`;
              return (
                <p key={w.skuCode} className="text-xs text-amber-700">
                  <span className="font-mono font-medium">{w.skuCode}</span>
                  {" — already in "}
                  <span className="font-medium">{w.otherCampaign.title}</span>
                  {`${reason} → yours would `}
                  <span
                    className={
                      w.willWin ? "font-semibold text-green-700" : "font-semibold text-red-700"
                    }
                  >
                    {w.willWin ? "win" : "lose"}
                  </span>
                  {"."}
                </p>
              );
            })}
            {extraWarnings > 0 && (
              <p className="text-xs text-amber-600/70">+{extraWarnings} more</p>
            )}
          </div>
        </div>
      )}

      {/* SkuPicker */}
      <div className="flex-1 min-h-0 flex flex-col">
        <SkuPicker
          selectedCodes={state.selectedCodes}
          onSelectionChange={(codes) => update({ selectedCodes: codes })}
        />
      </div>
    </div>
  );
}

// ── Category tab ───────────────────────────────────────────────────

function CategoryTab({
  state,
  update,
  submitted,
}: {
  state: DiscountWizardState;
  update: (patch: Partial<DiscountWizardState>) => void;
  submitted: boolean;
}) {
  const [search, setSearch] = useState("");

  const { data: tree = [], isLoading } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategoryTree,
    staleTime: 5 * 60_000,
  });

  const flat = useMemo(() => flattenTree(tree), [tree]);

  const filtered = useMemo(() => {
    if (!search.trim()) return flat;
    const q = search.toLowerCase();
    return flat.filter((c) => c.name.toLowerCase().includes(q));
  }, [flat, search]);

  const noCategoryError = submitted && state.selectedCategoryIds.length === 0;
  const count = state.selectedCategoryIds.length;

  const toggleCategory = (id: number) => {
    update({
      selectedCategoryIds: state.selectedCategoryIds.includes(id)
        ? state.selectedCategoryIds.filter((x) => x !== id)
        : [...state.selectedCategoryIds, id],
    });
  };

  return (
    <div className="space-y-5">
      {/* Include-descendants toggle */}
      <div className="flex items-center justify-between rounded-xl border border-border bg-muted/30 px-5 py-3">
        <div>
          <p className="text-sm font-medium">Include subcategories</p>
          <p className="text-xs text-muted-foreground">
            When on, all child categories of a selected category are also targeted.
          </p>
        </div>
        <Switch
          checked={state.includeDescendants}
          onCheckedChange={(v) => update({ includeDescendants: v })}
        />
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search categories…"
          className="pl-8 h-9 text-sm"
        />
      </div>

      {/* Status strip */}
      {noCategoryError && (
        <p className="text-sm text-destructive">
          At least one category must be selected to continue.
        </p>
      )}
      {count > 0 && (
        <p className="text-sm text-primary font-medium">
          <span className="font-bold">{count}</span>{" "}
          {count !== 1 ? "categories" : "category"} selected ·{" "}
          subcategories {state.includeDescendants ? "on" : "off"}
        </p>
      )}

      {/* Category list */}
      <div className="rounded-xl border border-border bg-card overflow-y-auto max-h-[400px] divide-y divide-border">
        {isLoading ? (
          <p className="p-4 text-sm text-muted-foreground">Loading categories…</p>
        ) : filtered.length === 0 ? (
          <p className="p-4 text-sm text-muted-foreground">
            {search ? "No categories match your search." : "No categories available."}
          </p>
        ) : (
          filtered.map((cat) => (
            <label
              key={cat.id}
              className="flex items-center gap-3 px-4 py-2.5 hover:bg-muted/40 cursor-pointer"
              style={{ paddingLeft: `${16 + cat.depth * 20}px` }}
            >
              <Checkbox
                checked={state.selectedCategoryIds.includes(cat.id)}
                onCheckedChange={() => toggleCategory(cat.id)}
                id={`cat-${cat.id}`}
              />
              <span className="text-sm">{cat.name}</span>
            </label>
          ))
        )}
      </div>
    </div>
  );
}

// ── Segment tab ────────────────────────────────────────────────────

function SegmentTab({
  state,
  update,
  submitted,
}: {
  state: DiscountWizardState;
  update: (patch: Partial<DiscountWizardState>) => void;
  submitted: boolean;
}) {
  const { data: tiers = [] } = useLoyaltyTiers();
  const noSegmentError = submitted && !state.selectedSegment;

  return (
    <div className="space-y-6 max-w-lg">
      {noSegmentError && (
        <p className="text-sm text-destructive">Please select a segment to continue.</p>
      )}

      <RadioGroup
        value={state.selectedSegment?.type ?? ""}
        onValueChange={(v) => {
          if (v === "NEW_CUSTOMER") {
            update({ selectedSegment: { type: "NEW_CUSTOMER" } });
          } else if (v === "LOYALTY_TIER") {
            update({ selectedSegment: { type: "LOYALTY_TIER", tierId: 0 } });
          }
        }}
        className="space-y-3"
      >
        {/* Loyalty Tier option */}
        <div
          className={cn(
            "flex flex-col gap-3 rounded-xl border p-5 transition-colors",
            state.selectedSegment?.type === "LOYALTY_TIER"
              ? "border-primary/40 bg-primary/5"
              : "border-border bg-muted/30"
          )}
        >
          <div className="flex items-start gap-3">
            <RadioGroupItem value="LOYALTY_TIER" id="seg-tier" className="mt-0.5" />
            <Label htmlFor="seg-tier" className="cursor-pointer">
              <p className="text-sm font-medium">Loyalty tier</p>
              <p className="text-xs text-muted-foreground">
                Applies only to customers in a specific loyalty tier.
              </p>
            </Label>
          </div>
          {state.selectedSegment?.type === "LOYALTY_TIER" && (
            <div className="pl-6">
              <Select
                value={
                  state.selectedSegment.tierId > 0
                    ? String(state.selectedSegment.tierId)
                    : ""
                }
                onValueChange={(v) =>
                  update({ selectedSegment: { type: "LOYALTY_TIER", tierId: Number(v) } })
                }
              >
                <SelectTrigger className="h-9 w-56">
                  <SelectValue placeholder="Select tier…" />
                </SelectTrigger>
                <SelectContent>
                  {tiers.map((t) => (
                    <SelectItem key={t.id} value={String(t.id)}>
                      <span className="flex items-center gap-2">
                        <span
                          className="h-2.5 w-2.5 rounded-full shrink-0"
                          style={{ backgroundColor: t.color }}
                        />
                        {t.name}
                        <span className="text-xs text-muted-foreground">
                          · {t.discountPercentage}%
                        </span>
                      </span>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {submitted &&
                state.selectedSegment.type === "LOYALTY_TIER" &&
                state.selectedSegment.tierId === 0 && (
                  <p className="text-xs text-destructive mt-1">Please select a tier.</p>
                )}
            </div>
          )}
        </div>

        {/* New customer option */}
        <div
          className={cn(
            "flex items-start gap-3 rounded-xl border p-5 transition-colors",
            state.selectedSegment?.type === "NEW_CUSTOMER"
              ? "border-primary/40 bg-primary/5"
              : "border-border bg-muted/30"
          )}
        >
          <RadioGroupItem value="NEW_CUSTOMER" id="seg-new" className="mt-0.5" />
          <Label htmlFor="seg-new" className="cursor-pointer">
            <p className="text-sm font-medium">New customers</p>
            <p className="text-xs text-muted-foreground">
              Applies to customers who have never placed an order.
            </p>
          </Label>
        </div>
      </RadioGroup>
    </div>
  );
}

// ── Main component ─────────────────────────────────────────────────

export function Step3Targets({ state, update, submitted, editId }: Props) {
  return (
    <div className="flex-1 flex flex-col space-y-6">
      <div>
        <h2 className="text-xl font-semibold tracking-tight">Product Selection</h2>
        <p className="text-sm text-muted-foreground mt-1">
          Choose how to target this campaign — by specific SKUs, a product category, or a customer
          segment.
        </p>
      </div>

      <Tabs
        value={state.targetMode}
        onValueChange={(v) => update({ targetMode: v as TargetMode })}
        className="flex-1 flex flex-col"
      >
        <TabsList className="w-fit">
          <TabsTrigger value="SKU" className="gap-1.5">
            <List className="h-3.5 w-3.5" />
            SKU list
          </TabsTrigger>
          <TabsTrigger value="CATEGORY" className="gap-1.5">
            <Tag className="h-3.5 w-3.5" />
            Category
          </TabsTrigger>
          <TabsTrigger value="SEGMENT" className="gap-1.5">
            <Users className="h-3.5 w-3.5" />
            Segment
          </TabsTrigger>
        </TabsList>

        <TabsContent value="SKU" className="flex-1 min-h-0 mt-6">
          <div className="flex flex-col h-full">
            <SkuTab state={state} update={update} submitted={submitted} editId={editId} />
          </div>
        </TabsContent>

        <TabsContent value="CATEGORY" className="mt-6">
          <CategoryTab state={state} update={update} submitted={submitted} />
        </TabsContent>

        <TabsContent value="SEGMENT" className="mt-6">
          <SegmentTab state={state} update={update} submitted={submitted} />
        </TabsContent>
      </Tabs>
    </div>
  );
}
