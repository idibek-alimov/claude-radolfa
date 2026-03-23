"use client";

import { useQuery } from "@tanstack/react-query";
import { AlertCircle } from "lucide-react";
import { fetchCategoryTree } from "@/entities/product/api";
import { fetchColors } from "@/entities/color";
import { validateStep2 } from "../../model/types";
import type { WizardState } from "../../model/types";
import type { CategoryTree } from "@/entities/product/model/types";
import { cn } from "@/shared/lib/utils";

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

export function Step3Review({ state }: Props) {
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

  const step2Errors = validateStep2(state);
  const hasBlockingErrors =
    step2Errors.emptySize.size > 0 || step2Errors.emptyBarcode.size > 0;

  const blockingMessages: string[] = [];
  if (hasBlockingErrors) {
    for (const variant of state.variants) {
      const color = colorMap[variant.colorId];
      const colorName =
        color?.displayName ?? color?.colorKey ?? `Color #${variant.colorId}`;

      const noBarcode = variant.skus.filter((r) =>
        step2Errors.emptyBarcode.has(r._key)
      ).length;
      const noSize = variant.skus.filter((r) =>
        step2Errors.emptySize.has(r._key)
      ).length;

      if (noBarcode > 0)
        blockingMessages.push(
          `${colorName} has ${noBarcode} SKU${noBarcode > 1 ? "s" : ""} with no barcode.`
        );
      if (noSize > 0)
        blockingMessages.push(
          `${colorName} has ${noSize} SKU${noSize > 1 ? "s" : ""} with no size label.`
        );
    }
  }

  const totalSkus = state.variants.flatMap((v) => v.skus).length;
  const filledAttributes = state.attributes.filter((a) => a.key && a.value).length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-lg font-semibold">Review &amp; Submit</h2>
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
            {blockingMessages.map((msg, i) => (
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
          value={
            categoryNode?.name ??
            (state.categoryId ? `ID ${state.categoryId}` : "—")
          }
        />
        <SummaryRow label="Variants" value={String(state.variants.length)} />
        <SummaryRow label="Total SKUs" value={String(totalSkus)} />
        {state.webDescription && (
          <SummaryRow
            label="Description"
            value={state.webDescription}
            truncate
          />
        )}
        {filledAttributes > 0 && (
          <SummaryRow
            label="Attributes"
            value={`${filledAttributes} filled`}
          />
        )}
      </div>

      {/* Per-variant breakdown */}
      {state.variants.length > 0 && (
        <div className="space-y-3">
          <p className="text-sm font-medium">Per-variant breakdown</p>
          {state.variants.map((variant) => {
            const color = colorMap[variant.colorId];
            const sizes = variant.skus
              .map((r) => r.sizeLabel || "—")
              .join(", ");

            return (
              <div
                key={variant.colorId}
                className="rounded-lg border p-4 space-y-2"
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span
                      className="h-4 w-4 rounded-full border border-black/10 shrink-0"
                      style={{
                        backgroundColor: color?.hexCode ?? "#e5e7eb",
                      }}
                    />
                    <span className="text-sm font-medium">
                      {color?.displayName ??
                        color?.colorKey ??
                        `Color #${variant.colorId}`}
                    </span>
                  </div>
                  <StatusBadge
                    isPublished={variant.isPublished}
                    isActive={variant.isActive}
                  />
                </div>
                <div className="text-sm text-muted-foreground space-y-0.5 ml-6">
                  <p>
                    {variant.images.length} image
                    {variant.images.length !== 1 ? "s" : ""}
                  </p>
                  <p>
                    {variant.skus.length} SKU
                    {variant.skus.length !== 1 ? "s" : ""}: {sizes}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

// ── StatusBadge ───────────────────────────────────────────────────────

function StatusBadge({
  isPublished,
  isActive,
}: {
  isPublished: boolean;
  isActive: boolean;
}) {
  if (!isPublished) {
    return (
      <span
        className={cn(
          "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium",
          "bg-destructive/10 text-destructive"
        )}
      >
        <span className="h-1.5 w-1.5 rounded-full bg-destructive" />
        Draft
      </span>
    );
  }
  if (!isActive) {
    return (
      <span
        className={cn(
          "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium",
          "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400"
        )}
      >
        <span className="h-1.5 w-1.5 rounded-full bg-amber-500" />
        Hidden
      </span>
    );
  }
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium",
        "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400"
      )}
    >
      <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
      Live
    </span>
  );
}

// ── SummaryRow ────────────────────────────────────────────────────────

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
      <span className="text-sm text-muted-foreground w-32 shrink-0">
        {label}
      </span>
      <span
        className={`text-sm font-medium ${truncate ? "truncate" : ""}`}
        title={truncate ? value : undefined}
      >
        {value}
      </span>
    </div>
  );
}
