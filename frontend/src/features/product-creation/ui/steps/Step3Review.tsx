"use client";

import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { AlertCircle, CheckCircle2, ImageIcon, Layers } from "lucide-react";
import { fetchCategoryTree } from "@/entities/product/api";
import { fetchColors } from "@/entities/color";
import { validateStep2 } from "../../model/types";
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
          `${colorName}: ${noBarcode} SKU${noBarcode > 1 ? "s" : ""} missing barcode`
        );
      if (noSize > 0)
        blockingMessages.push(
          `${colorName}: ${noSize} SKU${noSize > 1 ? "s" : ""} missing size label`
        );
    }
  }

  const totalSkus = state.variants.flatMap((v) => v.skus).length;
  const totalImages = state.variants.flatMap((v) => v.images).length;
  const filledAttributes = state.attributes.filter((a) => a.key && a.value).length;

  return (
    <div className="max-w-3xl space-y-5">
      {/* Step header */}
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-foreground">Review &amp; Submit</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Check everything before publishing. You can still edit after creation.
        </p>
      </div>

      {/* ── Error banner ─────────────────────────────────────────── */}
      {hasBlockingErrors && (
        <div className="rounded-xl border border-destructive/40 bg-destructive/8 p-4">
          <div className="flex items-center gap-2.5 text-destructive mb-2">
            <AlertCircle className="h-4.5 w-4.5 shrink-0" />
            <span className="text-sm font-semibold">
              Fix these issues before submitting
            </span>
          </div>
          <ul className="space-y-1 ml-7">
            {blockingMessages.map((msg, i) => (
              <li key={i} className="text-sm text-destructive flex items-start gap-1.5">
                <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-destructive shrink-0" />
                {msg}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* ── Ready banner ─────────────────────────────────────────── */}
      {!hasBlockingErrors && state.variants.length > 0 && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 flex items-center gap-2.5">
          <CheckCircle2 className="h-4.5 w-4.5 text-emerald-600 shrink-0" />
          <span className="text-sm font-medium text-emerald-800">
            All required fields are filled — ready to publish
          </span>
        </div>
      )}

      {/* ── Product summary card ─────────────────────────────────── */}
      <div className="bg-white rounded-xl border shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b bg-gray-50/60">
          <h2 className="text-sm font-semibold text-foreground">Product Summary</h2>
        </div>

        <div className="divide-y">
          <SummaryRow label="Product name" value={state.name || "—"} />
          <SummaryRow
            label="Category"
            value={
              categoryNode?.name ??
              (state.categoryId ? `ID ${state.categoryId}` : "—")
            }
          />
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
          <div className="grid grid-cols-3 divide-x">
            <StatCell
              icon={<Layers className="h-4 w-4" />}
              label="Variants"
              value={state.variants.length}
            />
            <StatCell
              icon={<span className="text-xs font-bold">SKU</span>}
              label="Total SKUs"
              value={totalSkus}
            />
            <StatCell
              icon={<ImageIcon className="h-4 w-4" />}
              label="Photos"
              value={totalImages}
            />
          </div>
        </div>
      </div>

      {/* ── Per-variant cards ────────────────────────────────────── */}
      {state.variants.length > 0 && (
        <div className="space-y-3">
          <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Color variants
          </p>
          {state.variants.map((variant) => {
            const color = colorMap[variant.colorId];
            const colorName =
              color?.displayName ??
              color?.colorKey ??
              `Color #${variant.colorId}`;
            const sizes = variant.skus.map((r) => r.sizeLabel || "—").join(", ");
            const hasVariantErrors = variant.skus.some(
              (r) =>
                step2Errors.emptySize.has(r._key) ||
                step2Errors.emptyBarcode.has(r._key)
            );

            return (
              <div
                key={variant.colorId}
                className="bg-white rounded-xl border shadow-sm overflow-hidden"
              >
                {/* Variant header */}
                <div className="flex items-center justify-between gap-4 px-5 py-3.5 border-b bg-gray-50/60">
                  <div className="flex items-center gap-2.5">
                    <span
                      className="h-5 w-5 rounded-full border border-black/10 shadow-sm shrink-0"
                      style={{
                        backgroundColor: color?.hexCode ?? "#e5e7eb",
                      }}
                    />
                    <span className="text-sm font-semibold text-foreground">
                      {colorName}
                    </span>
                  </div>
                  {hasVariantErrors && (
                    <span className="flex items-center gap-1.5 text-xs text-destructive">
                      <AlertCircle className="h-3.5 w-3.5" />
                      Incomplete
                    </span>
                  )}
                </div>

                <div className="px-5 py-4 flex gap-5">
                  {/* Image strip */}
                  <div className="flex gap-1.5 shrink-0">
                    {variant.images.length === 0 ? (
                      <div className="w-14 h-14 rounded-lg border bg-gray-50 flex items-center justify-center">
                        <ImageIcon className="h-5 w-5 text-muted-foreground/40" />
                      </div>
                    ) : (
                      <>
                        {variant.images.slice(0, 3).map((url, i) => (
                          <div
                            key={url}
                            className="relative w-14 h-14 rounded-lg overflow-hidden border bg-gray-50 shrink-0"
                          >
                            <Image
                              src={url}
                              alt="Product image"
                              fill
                              className="object-cover"
                              unoptimized
                            />
                            {i === 0 && (
                              <span className="absolute bottom-0 inset-x-0 bg-amber-500/85 text-white text-[8px] font-bold text-center py-0.5 uppercase tracking-wide">
                                Main
                              </span>
                            )}
                          </div>
                        ))}
                        {variant.images.length > 3 && (
                          <div className="w-14 h-14 rounded-lg border bg-gray-50 flex items-center justify-center text-xs font-semibold text-muted-foreground">
                            +{variant.images.length - 3}
                          </div>
                        )}
                      </>
                    )}
                  </div>

                  {/* Stats */}
                  <div className="flex-1 min-w-0 space-y-2">
                    <div className="flex items-center gap-4 text-sm">
                      <span className="text-muted-foreground">Photos</span>
                      <span className="font-medium">
                        {variant.images.length}
                      </span>
                    </div>
                    <div className="flex items-start gap-4 text-sm">
                      <span className="text-muted-foreground shrink-0">Sizes</span>
                      <span className="font-medium text-foreground truncate">
                        {sizes}
                      </span>
                    </div>
                    <div className="flex items-center gap-4 text-sm">
                      <span className="text-muted-foreground">SKUs</span>
                      <span className="font-medium">{variant.skus.length}</span>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
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
    <div className="grid grid-cols-[1fr_2fr] gap-6 px-6 py-3.5 items-baseline">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span
        className={`text-sm font-medium text-foreground ${truncate ? "truncate" : ""}`}
        title={truncate ? value : undefined}
      >
        {value}
      </span>
    </div>
  );
}

// ── StatCell ─────────────────────────────────────────────────────────

function StatCell({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: number;
}) {
  return (
    <div className="flex flex-col items-center gap-1 py-4 px-3 text-center">
      <div className="text-muted-foreground">{icon}</div>
      <span className="text-xl font-bold text-foreground">{value}</span>
      <span className="text-xs text-muted-foreground">{label}</span>
    </div>
  );
}
