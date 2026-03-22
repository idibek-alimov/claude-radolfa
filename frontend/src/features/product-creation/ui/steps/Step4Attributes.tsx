"use client";

import { useEffect, useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import { Plus, X, Loader2 } from "lucide-react";
import { fetchBlueprint } from "../../api/blueprint";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { cn } from "@/shared/lib/utils";
import type { WizardState, WizardAttribute } from "../../model/types";

interface Props {
  state: WizardState;
  update: (patch: Partial<WizardState>) => void;
  submitted: boolean;
  // Set of attribute keys that are required but empty (derived by orchestrator)
  failingKeys: Set<string>;
}

export function Step4Attributes({ state, update, submitted, failingKeys }: Props) {
  const { data: blueprint = [], isLoading } = useQuery({
    queryKey: ["blueprint", state.categoryId],
    queryFn: () => fetchBlueprint(state.categoryId!),
    enabled: state.categoryId !== null,
  });

  const synced = useRef(false);

  // On blueprint load, ensure every blueprint key has an entry in state.attributes.
  // Preserve any values already typed, and keep any free-form keys the user added.
  useEffect(() => {
    if (isLoading || blueprint.length === 0) return;
    // Run once per blueprint load (key change resets the ref via component remount)
    if (synced.current) return;
    synced.current = true;

    const existingKeys = new Set(state.attributes.map((a) => a.key));
    const toAdd: WizardAttribute[] = blueprint
      .filter((entry) => !existingKeys.has(entry.key))
      .map((entry, i) => ({
        key: entry.key,
        value: "",
        sortOrder: i,
      }));

    if (toAdd.length > 0) {
      update({ attributes: [...state.attributes, ...toAdd] });
    }
  }, [blueprint, isLoading, state.attributes, update]);

  function setValue(key: string, value: string) {
    update({
      attributes: state.attributes.map((a) =>
        a.key === key ? { ...a, value } : a
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
    update({
      attributes: state.attributes.filter((_, i) => i !== index),
    });
  }

  const blueprintKeys = new Set(blueprint.map((e) => e.key));

  // Blueprint-driven entries, sorted
  const blueprintAttrs = blueprint
    .slice()
    .sort((a, b) => (a.key > b.key ? 1 : -1))
    .map((entry) => ({
      entry,
      attr: state.attributes.find((a) => a.key === entry.key),
    }));

  // Free-form entries (keys not in blueprint)
  const freeFormAttrs = state.attributes
    .map((a, i) => ({ attr: a, index: i }))
    .filter(({ attr }) => !blueprintKeys.has(attr.key));

  if (isLoading) {
    return (
      <div className="space-y-6">
        <StepHeader />
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="h-4 w-4 animate-spin" />
          Loading category attributes…
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <StepHeader />

      {/* Blueprint-driven fields */}
      {blueprintAttrs.length > 0 ? (
        <div className="space-y-4">
          <p className="text-sm font-medium text-muted-foreground">
            Category attributes
          </p>
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
        <div className="rounded-lg border border-dashed p-6 text-center text-sm text-muted-foreground">
          No required attributes for this category.
        </div>
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
                onChange={(e) => setValue(attr.key, e.target.value)}
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
    </div>
  );
}

function StepHeader() {
  return (
    <div>
      <h2 className="text-lg font-semibold">Attributes</h2>
      <p className="text-sm text-muted-foreground mt-0.5">
        Fill in category-specific characteristics. These will be applied to all
        color variants.
      </p>
    </div>
  );
}
