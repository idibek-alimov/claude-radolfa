"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Save, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { updateListingDimensions } from "@/entities/product/api";
import { getErrorMessage } from "@/shared/lib";
import type { ListingVariantDetail } from "@/entities/product/model/types";

interface Props {
  detail: ListingVariantDetail;
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

function toNumber(s: string): number | undefined {
  const n = parseFloat(s);
  return isNaN(n) ? undefined : n;
}

type FormValues = Record<DimensionKey, string>;

export function DimensionsCard({ detail }: Props) {
  const t = useTranslations("manage");
  const queryClient = useQueryClient();

  const [values, setValues] = useState<FormValues>({
    weightKg: toInputValue(detail.weightKg),
    widthCm:  toInputValue(detail.widthCm),
    heightCm: toInputValue(detail.heightCm),
    depthCm:  toInputValue(detail.depthCm),
  });

  const mutation = useMutation({
    mutationFn: () =>
      updateListingDimensions(detail.slug, {
        weightKg: toNumber(values.weightKg),
        widthCm:  toNumber(values.widthCm),
        heightCm: toNumber(values.heightCm),
        depthCm:  toNumber(values.depthCm),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listing", detail.slug] });
      toast.success(t("dimensionsSaved"));
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const isDirty =
    values.weightKg !== toInputValue(detail.weightKg) ||
    values.widthCm  !== toInputValue(detail.widthCm)  ||
    values.heightCm !== toInputValue(detail.heightCm) ||
    values.depthCm  !== toInputValue(detail.depthCm);

  return (
    <div className="bg-card rounded-xl border shadow-sm p-6 space-y-5">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
        {t("dimensionsTitle")}
      </h2>

      <div className="grid grid-cols-2 gap-4">
        {FIELDS.map(({ key, labelKey, unit }) => (
          <div key={key} className="space-y-1.5">
            <Label htmlFor={key}>
              {t(labelKey)}{" "}
              <span className="text-xs text-muted-foreground font-normal">({unit})</span>
            </Label>
            <Input
              id={key}
              type="number"
              min={0}
              step="0.01"
              value={values[key]}
              onChange={(e) =>
                setValues((prev) => ({ ...prev, [key]: e.target.value }))
              }
              placeholder="—"
            />
          </div>
        ))}
      </div>

      <p className="text-xs text-muted-foreground">{t("dimensionsHint")}</p>

      <div className="flex justify-end">
        <Button
          onClick={() => mutation.mutate()}
          disabled={mutation.isPending || !isDirty}
        >
          {mutation.isPending ? (
            <>
              <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
              {t("saving")}
            </>
          ) : (
            <>
              <Save className="h-4 w-4 mr-1.5" />
              {t("save")}
            </>
          )}
        </Button>
      </div>
    </div>
  );
}
