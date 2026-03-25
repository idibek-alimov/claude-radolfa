"use client";

import { useEffect, useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import { Plus, X, Loader2, Info } from "lucide-react";
import { fetchCategoryTree } from "@/entities/product/api";
import { fetchBlueprint } from "../../api/blueprint";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { cn } from "@/shared/lib/utils";
import type { WizardState, WizardAttribute } from "../../model/types";
import { validateStep1 } from "../../model/types";
import type { CategoryTree } from "@/entities/product/model/types";

function flattenTree(
  nodes: CategoryTree[],
  depth = 0
): { id: number; name: string; depth: number }[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, depth },
    ...flattenTree(node.children, depth + 1),
  ]);
}

interface Props {
  state: WizardState;
  update: (patch: Partial<WizardState>) => void;
  submitted: boolean;
  failingKeys: Set<string>;
}

export function Step1BaseInfo({ state, update, submitted, failingKeys }: Props) {
  const synced = useRef(false);

  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategoryTree,
  });

  const { data: blueprint = [], isLoading: blueprintLoading } = useQuery({
    queryKey: ["blueprint", state.categoryId],
    queryFn: () => fetchBlueprint(state.categoryId!),
    enabled: state.categoryId !== null,
    staleTime: 5 * 60 * 1000,
  });

  // Reset sync state and clear attributes when category changes
  useEffect(() => {
    synced.current = false;
    update({ attributes: [] });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.categoryId]);

  // Sync blueprint keys into state.attributes on blueprint load.
  useEffect(() => {
    if (blueprintLoading || blueprint.length === 0) return;
    if (synced.current) return;
    synced.current = true;

    const existingKeys = new Set(state.attributes.map((a) => a.key));
    const toAdd: WizardAttribute[] = blueprint
      .filter((entry) => !existingKeys.has(entry.key))
      .map((entry, i) => ({ key: entry.key, value: "", sortOrder: i }));

    if (toAdd.length > 0) {
      update({ attributes: [...state.attributes, ...toAdd] });
    }
  }, [blueprint, blueprintLoading, state.attributes, update]);

  const flatCategories = categories ? flattenTree(categories) : [];
  const errors = submitted ? validateStep1(state) : {};

  const blueprintKeys = new Set(blueprint.map((e) => e.key));

  const blueprintAttrs = blueprint
    .slice()
    .sort((a, b) => (a.key > b.key ? 1 : -1))
    .map((entry) => ({
      entry,
      attr: state.attributes.find((a) => a.key === entry.key),
    }));

  const freeFormAttrs = state.attributes
    .map((a, i) => ({ attr: a, index: i }))
    .filter(({ attr }) => !blueprintKeys.has(attr.key));

  function setValue(key: string, value: string) {
    update({
      attributes: state.attributes.map((a) =>
        a.key === key ? { ...a, value } : a
      ),
    });
  }

  function setValueByIndex(index: number, value: string) {
    update({
      attributes: state.attributes.map((a, i) =>
        i === index ? { ...a, value } : a
      ),
    });
  }

  function addFreeForm() {
    const nextOrder = state.attributes.length;
    update({
      attributes: [
        ...state.attributes,
        { key: "", value: "", sortOrder: nextOrder },
      ],
    });
  }

  function updateFreeFormKey(index: number, key: string) {
    update({
      attributes: state.attributes.map((a, i) =>
        i === index ? { ...a, key } : a
      ),
    });
  }

  function removeFreeForm(index: number) {
    update({ attributes: state.attributes.filter((_, i) => i !== index) });
  }

  const requiredCount = blueprint.filter((e) => e.required).length;

  return (
    <div className="max-w-3xl space-y-5">
      {/* Step header */}
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-foreground">Basic Information</h1>
        <p className="text-sm text-muted-foreground mt-1">
          These details are shared across all color variants of this product.
        </p>
      </div>

      {/* ── Card 1: Main Details ─────────────────────────────────── */}
      <div className="bg-white rounded-xl border shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b bg-gray-50/60">
          <h2 className="text-sm font-semibold text-foreground">Main Details</h2>
        </div>

        <div className="px-6 py-5 space-y-5">
          {/* Product Name */}
          <div className="space-y-1.5">
            <label className="block text-sm font-medium text-foreground">
              Product Name <span className="text-destructive">*</span>
            </label>
            <Input
              value={state.name}
              onChange={(e) => update({ name: e.target.value })}
              placeholder="e.g. Classic Oxford Shirt"
              className={cn(
                "max-w-lg",
                errors.name && "border-destructive focus-visible:ring-destructive"
              )}
            />
            {errors.name && (
              <p className="text-xs text-destructive">{errors.name}</p>
            )}
          </div>

          {/* Category */}
          <div className="space-y-1.5">
            <label className="block text-sm font-medium text-foreground">
              Category <span className="text-destructive">*</span>
            </label>
            <select
              value={state.categoryId ?? ""}
              onChange={(e) => {
                const val = e.target.value ? Number(e.target.value) : null;
                update({ categoryId: val });
              }}
              className={cn(
                "flex h-9 w-full max-w-lg rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm",
                "focus:outline-none focus:ring-1 focus:ring-ring",
                errors.categoryId && "border-destructive"
              )}
            >
              <option value="">Select a category…</option>
              {flatCategories.map((c) => (
                <option key={c.id} value={c.id}>
                  {"\u00a0\u00a0".repeat(c.depth)}
                  {c.name}
                </option>
              ))}
            </select>
            {errors.categoryId && (
              <p className="text-xs text-destructive">{errors.categoryId}</p>
            )}
          </div>

          {/* Web Description */}
          <div className="space-y-1.5">
            <label className="block text-sm font-medium text-foreground">
              Product Description
            </label>
            <textarea
              value={state.webDescription}
              onChange={(e) => update({ webDescription: e.target.value })}
              placeholder="Describe the product for customers…"
              rows={4}
              className="flex w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring resize-none"
            />
            <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
              <Info className="h-3 w-3 shrink-0" />
              Shared across all color variants
            </p>
          </div>
        </div>
      </div>

      {/* ── Card 2: Category Characteristics ─────────────────────── */}
      {state.categoryId !== null && (
        <div className="bg-white rounded-xl border shadow-sm overflow-hidden">
          <div className="px-6 py-4 border-b bg-gray-50/60 flex items-center justify-between gap-4">
            <div>
              <h2 className="text-sm font-semibold text-foreground">
                Category Characteristics
              </h2>
              {!blueprintLoading && requiredCount > 0 && (
                <p className="text-xs text-muted-foreground mt-0.5">
                  {requiredCount} required field{requiredCount !== 1 ? "s" : ""}
                </p>
              )}
            </div>
            {blueprintLoading && (
              <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                Loading…
              </div>
            )}
          </div>

          <div className="px-6 py-5">
            {blueprintLoading ? (
              <div className="flex items-center gap-2 text-sm text-muted-foreground py-4">
                <Loader2 className="h-4 w-4 animate-spin" />
                Loading category attributes…
              </div>
            ) : (
              <>
                {/* Blueprint-driven attribute rows */}
                {blueprintAttrs.length > 0 ? (
                  <div className="divide-y -mx-6 px-6">
                    {blueprintAttrs.map(({ entry, attr }) => {
                      const hasError =
                        submitted && entry.required && failingKeys.has(entry.key);
                      return (
                        <div
                          key={entry.key}
                          className="grid grid-cols-[1fr_2fr] gap-6 py-4 items-start first:pt-0 last:pb-0"
                        >
                          {/* Left: label + hint */}
                          <div className="pt-1">
                            <span className="text-sm font-medium text-foreground">
                              {entry.key}
                              {entry.required && (
                                <span className="text-destructive ml-0.5">*</span>
                              )}
                            </span>
                            {(entry.suggestedValues?.length ?? 0) > 0 && (
                              <p className="text-xs text-muted-foreground mt-1 leading-relaxed">
                                e.g.{" "}
                                {entry.suggestedValues!.slice(0, 3).join(", ")}
                              </p>
                            )}
                          </div>

                          {/* Right: input */}
                          <div>
                            <Input
                              value={attr?.value ?? ""}
                              onChange={(e) => setValue(entry.key, e.target.value)}
                              className={cn(
                                hasError &&
                                  "border-destructive focus-visible:ring-destructive"
                              )}
                            />
                            {hasError && (
                              <p className="text-xs text-destructive mt-1">
                                This attribute is required
                              </p>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground py-2">
                    This category has no required attributes.
                  </p>
                )}

                {/* Free-form additions */}
                {freeFormAttrs.length > 0 && (
                  <div className="mt-6 pt-5 border-t space-y-3">
                    <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                      Additional Attributes
                    </p>
                    {freeFormAttrs.map(({ attr, index }) => (
                      <div key={index} className="flex items-center gap-2">
                        <Input
                          value={attr.key}
                          onChange={(e) => updateFreeFormKey(index, e.target.value)}
                          placeholder="Attribute name"
                          className="w-44 shrink-0"
                        />
                        <Input
                          value={attr.value}
                          onChange={(e) => setValueByIndex(index, e.target.value)}
                          placeholder="Value"
                          className="flex-1"
                        />
                        <button
                          type="button"
                          onClick={() => removeFreeForm(index)}
                          className="h-9 w-9 flex items-center justify-center rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors shrink-0"
                          aria-label="Remove attribute"
                        >
                          <X className="h-4 w-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {/* Add attribute button */}
                <div className={cn("pt-4", blueprintAttrs.length > 0 && "mt-2")}>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={addFreeForm}
                    className="gap-1.5 text-muted-foreground hover:text-foreground"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    Add Custom Attribute
                  </Button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
