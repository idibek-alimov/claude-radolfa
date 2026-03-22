"use client";

import { useQuery } from "@tanstack/react-query";
import { AlertCircle } from "lucide-react";
import { fetchCategoryTree } from "@/entities/product/api";
import { fetchColors } from "@/entities/color";
import { validateStep3 } from "../../model/types";
import type { WizardState } from "../../model/types";
import type { CategoryTree } from "@/entities/product/model/types";

interface Props {
  state: WizardState;
}

function findCategory(
  nodes: CategoryTree[],
  id: number
): CategoryTree | undefined {
  for (const node of nodes) {
    if (node.id === id) return node;
    const found = findCategory(node.children, id);
    if (found) return found;
  }
}

export function Step5Review({ state }: Props) {
  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategoryTree,
  });

  const { data: colors } = useQuery({
    queryKey: ["colors"],
    queryFn: fetchColors,
  });

  const colorMap = Object.fromEntries((colors ?? []).map((c) => [c.id, c]));
  const categoryNode =
    categories && state.categoryId
      ? findCategory(categories, state.categoryId)
      : undefined;

  const step3Errors = validateStep3(state);
  const hasBlockingErrors =
    step3Errors.emptySize.size > 0 || step3Errors.emptyBarcode.size > 0;

  // Build per-color error messages
  const colorErrors: string[] = [];
  if (hasBlockingErrors) {
    for (const colorId of state.colorIds) {
      const color = colorMap[colorId];
      const colorName =
        color?.displayName ?? color?.colorKey ?? `Color #${colorId}`;
      const rows = state.skuRows.filter((r) => r.colorId === colorId);

      const noBarcode = rows.filter((r) =>
        step3Errors.emptyBarcode.has(r._key)
      ).length;
      const noSize = rows.filter((r) =>
        step3Errors.emptySize.has(r._key)
      ).length;

      if (noBarcode > 0)
        colorErrors.push(
          `${colorName} has ${noBarcode} SKU${noBarcode > 1 ? "s" : ""} with no barcode.`
        );
      if (noSize > 0)
        colorErrors.push(
          `${colorName} has ${noSize} SKU${noSize > 1 ? "s" : ""} with no size label.`
        );
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-lg font-semibold">Review & Submit</h2>
        <p className="text-sm text-muted-foreground mt-0.5">
          Check the details below before creating the product.
        </p>
      </div>

      {/* Blocking errors */}
      {hasBlockingErrors && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-4 space-y-1">
          <div className="flex items-center gap-2 text-sm font-medium text-destructive">
            <AlertCircle className="h-4 w-4 shrink-0" />
            Fix these issues before submitting:
          </div>
          <ul className="ml-6 list-disc space-y-0.5">
            {colorErrors.map((msg, i) => (
              <li key={i} className="text-sm text-destructive">
                {msg}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Product summary */}
      <div className="rounded-lg border divide-y">
        <SummaryRow label="Product name" value={state.name || "—"} />
        <SummaryRow
          label="Category"
          value={categoryNode?.name ?? (state.categoryId ? `ID ${state.categoryId}` : "—")}
        />
        <SummaryRow
          label="Colors"
          value={`${state.colorIds.length} selected`}
        />
        <SummaryRow
          label="Total SKUs"
          value={String(state.skuRows.length)}
        />
        {state.webDescription && (
          <SummaryRow label="Description" value={state.webDescription} truncate />
        )}
        {state.attributes.filter((a) => a.key && a.value).length > 0 && (
          <SummaryRow
            label="Attributes"
            value={`${state.attributes.filter((a) => a.key && a.value).length} filled`}
          />
        )}
      </div>

      {/* Per-color breakdown */}
      <div className="space-y-3">
        <p className="text-sm font-medium">Per-color breakdown</p>
        {state.colorIds.map((colorId) => {
          const color = colorMap[colorId];
          const rows = state.skuRows.filter((r) => r.colorId === colorId);
          const imageCount = (state.imagesByColorId[colorId] ?? []).length;
          const sizes = rows.map((r) => r.sizeLabel || "—").join(", ");

          return (
            <div key={colorId} className="rounded-lg border p-4 space-y-2">
              <div className="flex items-center gap-2">
                <span
                  className="h-4 w-4 rounded-full border border-black/10 shrink-0"
                  style={{ backgroundColor: color?.hexCode ?? "#e5e7eb" }}
                />
                <span className="text-sm font-medium">
                  {color?.displayName ?? color?.colorKey ?? `Color #${colorId}`}
                </span>
              </div>
              <div className="text-sm text-muted-foreground space-y-0.5 ml-6">
                <p>
                  {imageCount} image{imageCount !== 1 ? "s" : ""}
                </p>
                <p>
                  {rows.length} SKU{rows.length !== 1 ? "s" : ""}: {sizes}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function SummaryRow({
  label,
  value,
  truncate,
}: {
  label: string;
  value: string;
  truncate?: boolean;
}) {
  return (
    <div className="flex items-baseline gap-4 px-4 py-3">
      <span className="text-sm text-muted-foreground w-32 shrink-0">{label}</span>
      <span
        className={`text-sm font-medium ${truncate ? "truncate" : ""}`}
        title={truncate ? value : undefined}
      >
        {value}
      </span>
    </div>
  );
}
