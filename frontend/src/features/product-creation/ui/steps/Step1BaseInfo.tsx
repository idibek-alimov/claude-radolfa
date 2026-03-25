"use client";

import { useEffect, useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import { Plus, X, Loader2 } from "lucide-react";
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
  /** Shows field errors after the user clicks Next */
  submitted: boolean;
  /** Required blueprint keys with empty values — derived by the orchestrator */
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
  // Preserves existing values and keeps free-form keys the user added.
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

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Base Information</h2>
        <p className="text-sm text-muted-foreground mt-0.5">
          Product details and category characteristics — shared across all color variants.
        </p>
      </div>

      {/* Product Name */}
      <div className="space-y-1.5">
        <label className="text-sm font-medium">
          Product Name <span className="text-destructive">*</span>
        </label>
        <Input
          value={state.name}
          onChange={(e) => update({ name: e.target.value })}
          placeholder="e.g. Classic Oxford Shirt"
          className={cn(errors.name && "border-destructive")}
        />
        {errors.name && (
          <p className="text-xs text-destructive">{errors.name}</p>
        )}
      </div>

      {/* Category */}
      <div className="space-y-1.5">
        <label className="text-sm font-medium">
          Category <span className="text-destructive">*</span>
        </label>
        <select
          value={state.categoryId ?? ""}
          onChange={(e) => {
            const val = e.target.value ? Number(e.target.value) : null;
            update({ categoryId: val });
          }}
          className={cn(
            "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm",
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
        <label className="text-sm font-medium">
          Product Description
          <span className="ml-1.5 text-xs text-muted-foreground font-normal">
            (shared across all color variants)
          </span>
        </label>
        <textarea
          value={state.webDescription}
          onChange={(e) => update({ webDescription: e.target.value })}
          placeholder="Describe the product for customers…"
          rows={4}
          className="flex w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring resize-none"
        />
      </div>

      {/* Blueprint Attributes — appear after category is selected */}
      {state.categoryId !== null && (
        <div className="space-y-4 border-t pt-6">
          <div>
            <p className="text-sm font-semibold">Category Characteristics</p>
            <p className="text-xs text-muted-foreground mt-0.5">
              These attributes apply to all variants of this product.
            </p>
          </div>

          {blueprintLoading ? (
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              Loading category attributes…
            </div>
          ) : (
            <>
              {/* Blueprint-driven fields */}
              {blueprintAttrs.length > 0 ? (
                <div className="space-y-4">
                  {blueprintAttrs.map(({ entry, attr }) => {
                    const hasError =
                      submitted && entry.required && failingKeys.has(entry.key);
                    return (
                      <div key={entry.key} className="space-y-1">
                        <label className="text-sm font-medium">
                          {entry.key}
                          {entry.required && (
                            <span className="text-destructive ml-0.5">*</span>
                          )}
                        </label>
                        <Input
                          value={attr?.value ?? ""}
                          onChange={(e) => setValue(entry.key, e.target.value)}
                          placeholder={
                            entry.suggestedValues?.length
                              ? `e.g. ${entry.suggestedValues.slice(0, 3).join(", ")}`
                              : undefined
                          }
                          className={cn(
                            hasError &&
                              "border-destructive focus-visible:ring-destructive"
                          )}
                        />
                        {hasError && (
                          <p className="text-xs text-destructive">
                            This attribute is required
                          </p>
                        )}
                      </div>
                    );
                  })}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">
                  This category has no required attributes.
                </p>
              )}

              {/* Free-form additions */}
              {freeFormAttrs.length > 0 && (
                <div className="space-y-3">
                  <p className="text-sm font-medium text-muted-foreground">
                    Additional attributes
                  </p>
                  {freeFormAttrs.map(({ attr, index }) => (
                    <div key={index} className="flex items-center gap-2">
                      <Input
                        value={attr.key}
                        onChange={(e) => updateFreeFormKey(index, e.target.value)}
                        placeholder="Attribute name"
                        className="w-40 shrink-0"
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

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={addFreeForm}
                className="gap-1.5"
              >
                <Plus className="h-4 w-4" />
                Add Attribute
              </Button>
            </>
          )}
        </div>
      )}
    </div>
  );
}
