"use client";

import { useQuery } from "@tanstack/react-query";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { fetchDiscountTypes } from "../../api";
import { DiscountStatusBadge } from "../DiscountStatusBadge";
import type { DiscountWizardState } from "../DiscountCreationWizard";
import { cn } from "@/shared/lib/utils";
import { Settings2, Palette, Tag } from "lucide-react";

const ACCENT_COLORS = [
  "#F97316",
  "#8B5CF6",
  "#06B6D4",
  "#10B981",
  "#F59E0B",
  "#EF4444",
];

const PRESET_COLORS = [
  "E74C3C",
  "E67E22",
  "F1C40F",
  "2ECC71",
  "1ABC9C",
  "3498DB",
  "9B59B6",
  "E91E63",
  "FF5722",
  "607D8B",
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

interface Props {
  state: DiscountWizardState;
  update: (patch: Partial<DiscountWizardState>) => void;
  submitted: boolean;
}

export function Step1Details({ state, update, submitted }: Props) {
  const { data: types = [] } = useQuery({
    queryKey: ["discount-types"],
    queryFn: fetchDiscountTypes,
  });

  const showPreview = state.title.trim().length > 0 && state.colorHex.length === 6;

  const titleError = submitted && state.title.trim().length === 0;
  const typeError = submitted && state.typeId === 0;
  const valueError =
    submitted && (state.discountValue < 1 || state.discountValue > 99);
  const colorError = submitted && state.colorHex.length !== 6;

  return (
    <div className="flex-1 flex flex-col space-y-10">
      {/* Heading */}
      <div>
        <h2 className="text-xl font-semibold tracking-tight">Campaign Details</h2>
        <p className="text-sm text-muted-foreground mt-1">
          Define the name, type, and visual identity of this discount campaign.
        </p>
      </div>

      {/* Two-column grid: Campaign Info (left) + Discount % (right) */}
      <div className="grid grid-cols-2 gap-8 items-start">

        {/* LEFT — Campaign Info */}
        <section className="space-y-6">
          <SectionHeading icon={Settings2} title="Campaign Info" />

          <div className="space-y-1.5">
            <Label htmlFor="title">
              Campaign title <span className="text-destructive">*</span>
            </Label>
            <Input
              id="title"
              value={state.title}
              onChange={(e) => update({ title: e.target.value })}
              placeholder="e.g. Winter Sale"
              autoFocus
              className={cn(
                "h-11 text-base",
                titleError ? "border-destructive focus-visible:ring-destructive" : ""
              )}
            />
            {titleError && (
              <p className="text-xs text-destructive">Title is required.</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="type">
              Discount type <span className="text-destructive">*</span>
            </Label>
            <Select
              value={state.typeId === 0 ? "" : String(state.typeId)}
              onValueChange={(v) => update({ typeId: Number(v) })}
            >
              <SelectTrigger
                id="type"
                className={cn(
                  "h-11",
                  typeError ? "border-destructive focus:ring-destructive" : ""
                )}
              >
                <SelectValue placeholder="Select type…" />
              </SelectTrigger>
              <SelectContent>
                {types.map((t, i) => (
                  <SelectItem key={t.id} value={String(t.id)}>
                    <span className="flex items-center gap-2">
                      <span
                        className="h-2.5 w-2.5 rounded-full shrink-0"
                        style={{ backgroundColor: ACCENT_COLORS[i % ACCENT_COLORS.length] }}
                      />
                      {t.name}
                      <span className="text-muted-foreground text-xs">· rank {t.rank}</span>
                    </span>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {typeError && (
              <p className="text-xs text-destructive">Please select a type.</p>
            )}
          </div>
        </section>

        {/* RIGHT — Discount % */}
        <section className="space-y-6">
          <SectionHeading icon={Tag} title="Discount %" />

          <div className="space-y-1.5">
            <div className="rounded-xl border border-border bg-muted/30 p-6 space-y-4">
              <div className="flex items-end gap-4">
                <span
                  className={cn(
                    "text-5xl font-black tabular-nums leading-none",
                    valueError ? "text-destructive" : "text-rose-600 dark:text-rose-400"
                  )}
                >
                  {state.discountValue}%
                </span>
                <Input
                  id="discountValue"
                  type="number"
                  min={1}
                  max={99}
                  value={state.discountValue}
                  onChange={(e) => update({ discountValue: Number(e.target.value) })}
                  className={cn(
                    "w-20 h-9 font-mono text-sm mb-0.5",
                    valueError ? "border-destructive focus-visible:ring-destructive" : ""
                  )}
                />
              </div>
              <input
                type="range"
                min={1}
                max={99}
                value={state.discountValue}
                onChange={(e) => update({ discountValue: Number(e.target.value) })}
                className="w-full h-2 accent-rose-600 cursor-pointer rounded-full"
              />
            </div>
            {valueError && (
              <p className="text-xs text-destructive">Must be between 1 and 99.</p>
            )}
          </div>
        </section>

      </div>

      {/* Full-width — Badge Color */}
      <section className="space-y-6">
        <SectionHeading icon={Palette} title="Badge Color" />

        <div className="flex gap-8 items-start">

          {/* Left: controls */}
          <div className="flex-1 space-y-4">
            {/* Preset swatches */}
            <div className="space-y-2">
              <p className="text-xs font-medium text-muted-foreground">Presets</p>
              <div className="flex flex-wrap gap-2">
                {PRESET_COLORS.map((hex) => (
                  <button
                    key={hex}
                    type="button"
                    onClick={() => update({ colorHex: hex })}
                    className={cn(
                      "h-8 w-8 rounded-full border-2 transition-all duration-150 hover:scale-110",
                      state.colorHex.toUpperCase() === hex.toUpperCase()
                        ? "border-foreground ring-2 ring-offset-2 ring-foreground/30 scale-110"
                        : "border-transparent"
                    )}
                    style={{ backgroundColor: `#${hex}` }}
                    aria-label={`Color #${hex}`}
                  />
                ))}
              </div>
            </div>

            {/* Color picker + hex input */}
            <div className="flex items-center gap-3">
              <input
                type="color"
                value={`#${state.colorHex}`}
                onChange={(e) => update({ colorHex: e.target.value.replace("#", "") })}
                className="h-11 w-14 cursor-pointer rounded-lg border border-input bg-transparent p-0.5"
              />
              <div className="space-y-1">
                <Label htmlFor="colorHex">Custom hex</Label>
                <div className="flex items-center gap-1.5">
                  <span className="text-sm text-muted-foreground font-mono">#</span>
                  <Input
                    id="colorHex"
                    value={state.colorHex}
                    onChange={(e) =>
                      update({
                        colorHex: e.target.value
                          .replace(/[^0-9a-fA-F]/g, "")
                          .slice(0, 6),
                      })
                    }
                    maxLength={6}
                    placeholder="E74C3C"
                    className={cn(
                      "font-mono w-28 uppercase h-9",
                      colorError
                        ? "border-destructive focus-visible:ring-destructive"
                        : ""
                    )}
                  />
                </div>
              </div>
            </div>

            {colorError && (
              <p className="text-xs text-destructive">
                Enter a valid 6-character hex color.
              </p>
            )}
          </div>

          {/* Right: live preview */}
          {showPreview && (
            <div className="shrink-0 rounded-xl border border-border bg-muted/30 p-6 min-w-[220px]">
              <p className="text-xs font-medium text-muted-foreground mb-3 uppercase tracking-wide">
                Live preview
              </p>
              <div className="flex items-center gap-4">
                <DiscountStatusBadge
                  discount={{
                    id: 0,
                    type: { id: 0, name: "", rank: 0 },
                    itemCodes: [],
                    discountValue: state.discountValue,
                    validFrom: "",
                    validUpto: "",
                    disabled: false,
                    title: state.title,
                    colorHex: state.colorHex,
                  }}
                />
                <p className="text-sm text-muted-foreground">
                  This badge appears on product cards during the campaign.
                </p>
              </div>
            </div>
          )}

        </div>
      </section>
    </div>
  );
}
