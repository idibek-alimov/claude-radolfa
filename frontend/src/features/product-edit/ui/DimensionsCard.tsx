"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { useDraft } from "../model/ProductCardDraftContext";
import type { VariantDraft } from "../model/useProductCardDraft";

interface Props {
  variantId: number;
}

type DimensionKey = "weightKg" | "widthCm" | "heightCm" | "depthCm";

interface FieldConfig {
  key: DimensionKey;
  labelKey: "fieldWeight" | "fieldWidth" | "fieldHeight" | "fieldDepth";
  unit: string;
}

const FIELDS: FieldConfig[] = [
  { key: "weightKg", labelKey: "fieldWeight", unit: "kg" },
  { key: "widthCm",  labelKey: "fieldWidth",  unit: "cm" },
  { key: "heightCm", labelKey: "fieldHeight", unit: "cm" },
  { key: "depthCm",  labelKey: "fieldDepth",  unit: "cm" },
];

function toInputValue(v: number | null | undefined): string {
  return v != null ? String(v) : "";
}

function toNumber(s: string): number | null {
  const n = parseFloat(s);
  return isNaN(n) ? null : n;
}

export function DimensionsCard({ variantId }: Props) {
  const t = useTranslations("manage");
  const { draft, updateVariantField } = useDraft();

  const variantDraft = draft.variants[variantId];

  function handleChange(key: DimensionKey, rawValue: string) {
    updateVariantField(
      variantId,
      key as keyof Omit<VariantDraft, "skus">,
      toNumber(rawValue)
    );
  }

  return (
    <div className="bg-card rounded-xl border shadow-sm p-6 space-y-5">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
        {t("dimensionsTitle")}
      </h2>

      <div className="grid grid-cols-2 gap-4">
        {FIELDS.map(({ key, labelKey, unit }) => (
          <div key={key} className="space-y-1.5">
            <Label htmlFor={`${variantId}-${key}`}>
              {t(labelKey)}{" "}
              <span className="text-xs text-muted-foreground font-normal">({unit})</span>
            </Label>
            <Input
              id={`${variantId}-${key}`}
              type="number"
              min={0}
              step="0.01"
              value={toInputValue(variantDraft?.[key])}
              onChange={(e) => handleChange(key, e.target.value)}
              placeholder="—"
            />
          </div>
        ))}
      </div>

      <p className="text-xs text-muted-foreground">{t("dimensionsHint")}</p>
    </div>
  );
}
