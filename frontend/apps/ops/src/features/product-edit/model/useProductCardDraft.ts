"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { ProductCard, ProductCardVariant, ProductCardSku } from "@/entities/product/model/types";

// ── Draft shape ───────────────────────────────────────────────────────────────

export interface SkuDraft {
  sizeLabel: string;
  price: number;          // maps to ProductCardSku.originalPrice
  stockQuantity: number;
}

export interface VariantDraft {
  webDescription: string;
  weightKg: number | null;
  widthCm: number | null;
  heightCm: number | null;
  depthCm: number | null;
  skus: Record<number, SkuDraft>; // keyed by skuId
}

export interface Draft {
  name: string;
  categoryId: number | null;
  variants: Record<number, VariantDraft>; // keyed by variantId
}

// ── Diff ops ──────────────────────────────────────────────────────────────────

export type DiffOp =
  | { kind: "name"; value: string }
  | { kind: "category"; value: number }
  | { kind: "webDescription"; variantId: number; slug: string; value: string }
  | {
      kind: "dimensions";
      variantId: number;
      slug: string;
      weightKg: number | null;
      widthCm: number | null;
      heightCm: number | null;
      depthCm: number | null;
    }
  | { kind: "sizeLabel"; skuId: number; value: string }
  | { kind: "price"; skuId: number; value: number }
  | { kind: "stock"; skuId: number; value: number };

// ── Helpers ───────────────────────────────────────────────────────────────────

function skuToDraft(sku: ProductCardSku): SkuDraft {
  return {
    sizeLabel: sku.sizeLabel,
    price: sku.originalPrice,
    stockQuantity: sku.stockQuantity,
  };
}

function variantToDraft(v: ProductCardVariant): VariantDraft {
  const skus: Record<number, SkuDraft> = {};
  for (const sku of v.skus) {
    skus[sku.skuId] = skuToDraft(sku);
  }
  return {
    webDescription: v.webDescription ?? "",
    weightKg: v.weightKg,
    widthCm: v.widthCm,
    heightCm: v.heightCm,
    depthCm: v.depthCm,
    skus,
  };
}

function toDraft(card: ProductCard): Draft {
  const variants: Record<number, VariantDraft> = {};
  for (const v of card.variants) {
    variants[v.variantId] = variantToDraft(v);
  }
  return { name: card.name, categoryId: card.categoryId, variants };
}

/**
 * Reconcile draft with a new server snapshot.
 * - New SKUs that don't exist in draft → seed from server.
 * - Existing draft SKUs → keep draft values (user may be editing).
 * - New variants → seed from server.
 * - Removed variants → drop from draft.
 */
function reconcile(prev: Draft, lastServer: ProductCard, next: ProductCard): Draft {
  // Build a fast lookup of which fields were dirty vs server at the last snapshot
  const lastDiff = computeDiff(lastServer, prev);
  const dirtyKinds = new Set(lastDiff.map((op) => op.kind));

  const newVariants: Record<number, VariantDraft> = {};

  for (const v of next.variants) {
    const prevVariant = prev.variants[v.variantId];
    if (!prevVariant) {
      // Brand new variant (e.g. just added via Add Color) — seed from server
      newVariants[v.variantId] = variantToDraft(v);
      continue;
    }

    // Reconcile SKUs
    const newSkus: Record<number, SkuDraft> = {};
    for (const sku of v.skus) {
      const prevSku = prevVariant.skus[sku.skuId];
      if (!prevSku) {
        // Brand new SKU (e.g. just added via Add Size) — seed from server
        newSkus[sku.skuId] = skuToDraft(sku);
      } else {
        // Keep whatever user typed
        newSkus[sku.skuId] = prevSku;
      }
    }

    newVariants[v.variantId] = {
      // Keep draft values for variant-level fields
      webDescription: prevVariant.webDescription,
      weightKg: prevVariant.weightKg,
      widthCm: prevVariant.widthCm,
      heightCm: prevVariant.heightCm,
      depthCm: prevVariant.depthCm,
      skus: newSkus,
    };
  }

  return {
    // Keep draft values for base fields
    name: dirtyKinds.has("name") ? prev.name : next.name,
    categoryId: dirtyKinds.has("category") ? prev.categoryId : next.categoryId,
    variants: newVariants,
  };
}

function computeDiff(server: ProductCard, draft: Draft): DiffOp[] {
  const ops: DiffOp[] = [];

  // Base fields
  if (draft.name.trim() !== server.name && draft.name.trim().length > 0) {
    ops.push({ kind: "name", value: draft.name.trim() });
  }
  if (
    draft.categoryId !== null &&
    draft.categoryId !== server.categoryId
  ) {
    ops.push({ kind: "category", value: draft.categoryId });
  }

  // Per-variant fields
  for (const sv of server.variants) {
    const dv = draft.variants[sv.variantId];
    if (!dv) continue;

    if (dv.webDescription !== (sv.webDescription ?? "")) {
      ops.push({ kind: "webDescription", variantId: sv.variantId, slug: sv.slug, value: dv.webDescription });
    }

    const dimChanged =
      dv.weightKg !== sv.weightKg ||
      dv.widthCm  !== sv.widthCm  ||
      dv.heightCm !== sv.heightCm ||
      dv.depthCm  !== sv.depthCm;

    if (dimChanged) {
      ops.push({
        kind: "dimensions",
        variantId: sv.variantId,
        slug: sv.slug,
        weightKg: dv.weightKg,
        widthCm:  dv.widthCm,
        heightCm: dv.heightCm,
        depthCm:  dv.depthCm,
      });
    }

    // Per-SKU fields
    for (const ss of sv.skus) {
      const ds = dv.skus[ss.skuId];
      if (!ds) continue;

      if (ds.sizeLabel.trim() !== ss.sizeLabel && ds.sizeLabel.trim().length > 0) {
        ops.push({ kind: "sizeLabel", skuId: ss.skuId, value: ds.sizeLabel.trim() });
      }
      if (ds.price !== ss.originalPrice) {
        ops.push({ kind: "price", skuId: ss.skuId, value: ds.price });
      }
      if (ds.stockQuantity !== ss.stockQuantity) {
        ops.push({ kind: "stock", skuId: ss.skuId, value: ds.stockQuantity });
      }
    }
  }

  return ops;
}

// ── Hook ──────────────────────────────────────────────────────────────────────

export function useProductCardDraft(card: ProductCard) {
  const [draft, setDraft] = useState<Draft>(() => toDraft(card));
  const lastServerRef = useRef<ProductCard>(card);

  // Reconcile when TanStack Query delivers a new server snapshot
  useEffect(() => {
    setDraft((prev) => reconcile(prev, lastServerRef.current, card));
    lastServerRef.current = card;
  }, [card]);

  const diff = useMemo(() => computeDiff(card, draft), [card, draft]);
  const isDirty = diff.length > 0;

  const updateName = useCallback(
    (value: string) => setDraft((d) => ({ ...d, name: value })),
    []
  );

  const updateCategory = useCallback(
    (value: number | null) => setDraft((d) => ({ ...d, categoryId: value })),
    []
  );

  const updateVariantField = useCallback(
    <K extends keyof Omit<VariantDraft, "skus">>(
      variantId: number,
      key: K,
      value: VariantDraft[K]
    ) =>
      setDraft((d) => ({
        ...d,
        variants: {
          ...d.variants,
          [variantId]: { ...d.variants[variantId], [key]: value },
        },
      })),
    []
  );

  const updateSkuField = useCallback(
    (
      variantId: number,
      skuId: number,
      key: keyof SkuDraft,
      value: string | number
    ) =>
      setDraft((d) => {
        const variant = d.variants[variantId];
        if (!variant) return d;
        return {
          ...d,
          variants: {
            ...d.variants,
            [variantId]: {
              ...variant,
              skus: {
                ...variant.skus,
                [skuId]: { ...variant.skus[skuId], [key]: value },
              },
            },
          },
        };
      }),
    []
  );

  const reset = useCallback(() => setDraft(toDraft(card)), [card]);

  return {
    draft,
    diff,
    isDirty,
    updateName,
    updateCategory,
    updateVariantField,
    updateSkuField,
    reset,
  };
}

export type ProductCardDraftApi = ReturnType<typeof useProductCardDraft>;
