"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchCategoryTree } from "@/entities/product/api";
import { fetchColors } from "@/entities/color";
import { fetchBlueprint } from "../../api/blueprint";
import type { WizardState } from "../../model/types";
import { validateStep1 } from "../../model/types";
import type { CategoryTree, Color } from "@/entities/product/model/types";
import { Input } from "@/shared/ui/input";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/shared/ui/alert-dialog";
import { cn } from "@/shared/lib/utils";

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
  /** Set to true by the orchestrator when user clicks Next to show inline errors */
  submitted: boolean;
}

export function Step1Classification({ state, update, submitted }: Props) {
  const [colorToRemove, setColorToRemove] = useState<number | null>(null);

  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategoryTree,
  });

  const { data: colors } = useQuery({
    queryKey: ["colors"],
    queryFn: fetchColors,
  });

  // Prefetch blueprint as soon as category is selected (used in Step 4)
  useQuery({
    queryKey: ["blueprint", state.categoryId],
    queryFn: () => fetchBlueprint(state.categoryId!),
    enabled: state.categoryId !== null,
    staleTime: 5 * 60 * 1000,
  });

  const flatCategories = categories ? flattenTree(categories) : [];

  // Only show errors after the user attempted to advance
  const errors = submitted ? validateStep1(state) : {};

  // ── Color toggle ───────────────────────────────────────────────────
  function handleColorToggle(colorId: number) {
    if (state.colorIds.includes(colorId)) {
      const uploadedCount = (state.imagesByColorId[colorId] ?? []).length;
      if (uploadedCount > 0) {
        setColorToRemove(colorId);
        return;
      }
      update({ colorIds: state.colorIds.filter((id) => id !== colorId) });
    } else {
      update({ colorIds: [...state.colorIds, colorId] });
    }
  }

  function confirmRemoveColor() {
    if (colorToRemove === null) return;
    const newImagesByColorId = { ...state.imagesByColorId };
    delete newImagesByColorId[colorToRemove];
    update({
      colorIds: state.colorIds.filter((id) => id !== colorToRemove),
      imagesByColorId: newImagesByColorId,
    });
    setColorToRemove(null);
  }

  const colorToRemoveData = colors?.find((c) => c.id === colorToRemove);
  const uploadedCountForRemoval = colorToRemove
    ? (state.imagesByColorId[colorToRemove] ?? []).length
    : 0;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Classification &amp; Setup</h2>
        <p className="text-sm text-muted-foreground mt-0.5">
          Basic product information — you can come back and edit before submitting.
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

      {/* Color Multi-Select */}
      <div className="space-y-2">
        <label className="text-sm font-medium">
          Colors <span className="text-destructive">*</span>
          <span className="ml-1.5 text-xs text-muted-foreground font-normal">
            (select all that apply — each gets its own image gallery)
          </span>
        </label>

        {colors && colors.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {colors.map((color) => (
              <ColorChip
                key={color.id}
                color={color}
                selected={state.colorIds.includes(color.id)}
                onToggle={() => handleColorToggle(color.id)}
              />
            ))}
          </div>
        ) : (
          <div className="flex flex-wrap gap-2">
            {Array.from({ length: 6 }).map((_, i) => (
              <div
                key={i}
                className="h-9 w-28 rounded-full bg-muted animate-pulse"
              />
            ))}
          </div>
        )}

        {errors.colorIds && (
          <p className="text-xs text-destructive">{errors.colorIds}</p>
        )}
      </div>

      {/* Confirm color removal (has uploads) */}
      <AlertDialog
        open={colorToRemove !== null}
        onOpenChange={(open) => !open && setColorToRemove(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Remove color?</AlertDialogTitle>
            <AlertDialogDescription>
              Removing{" "}
              <strong>
                {colorToRemoveData?.displayName ?? colorToRemoveData?.colorKey}
              </strong>{" "}
              will discard {uploadedCountForRemoval} uploaded{" "}
              {uploadedCountForRemoval === 1 ? "image" : "images"}. This cannot
              be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Keep color</AlertDialogCancel>
            <AlertDialogAction
              onClick={confirmRemoveColor}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Remove anyway
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

// ── ColorChip ────────────────────────────────────────────────────────

function ColorChip({
  color,
  selected,
  onToggle,
}: {
  color: Color;
  selected: boolean;
  onToggle: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onToggle}
      className={cn(
        "flex items-center gap-2 rounded-full border px-3 py-1.5 text-sm transition-all",
        selected
          ? "border-primary bg-primary/10 font-medium text-primary ring-1 ring-primary"
          : "border-border bg-background text-foreground hover:border-primary/50"
      )}
    >
      <span
        className="h-3.5 w-3.5 rounded-full border border-black/10 shrink-0"
        style={{ backgroundColor: color.hexCode ?? "#e5e7eb" }}
      />
      {color.displayName ?? color.colorKey}
    </button>
  );
}
