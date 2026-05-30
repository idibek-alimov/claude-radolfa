"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  updateProductName,
  updateProductCategory,
  updateSkuSizeLabel,
  updateSkuPrice,
  updateSkuStock,
} from "@/entities/product/api/admin";
import { updateListing, updateListingDimensions } from "@/entities/product/api";
import type { DiffOp } from "./useProductCardDraft";

export function useCommitDraft(productBaseId: number) {
  const qc = useQueryClient();
  const [isSaving, setIsSaving] = useState(false);

  async function commit(diff: DiffOp[], variantSlugs: Set<string>): Promise<boolean> {
    if (diff.length === 0) return true;
    setIsSaving(true);

    const results = await Promise.allSettled(diff.map((op) => runOp(op)));

    const failCount = results.filter((r) => r.status === "rejected").length;
    setIsSaving(false);

    if (failCount === 0) {
      toast.success(
        `Saved ${diff.length} change${diff.length !== 1 ? "s" : ""}`
      );
    } else {
      toast.error(`${failCount} of ${diff.length} changes failed`);
    }

    // Always invalidate — successfully-saved ops need to reflect in the cache
    qc.invalidateQueries({ queryKey: ["admin-product", productBaseId] });
    qc.invalidateQueries({ queryKey: ["listings"] });
    for (const slug of variantSlugs) {
      qc.invalidateQueries({ queryKey: ["listing", slug] });
    }

    return failCount === 0;
  }

  function runOp(op: DiffOp): Promise<unknown> {
    switch (op.kind) {
      case "name":
        return updateProductName(productBaseId, op.value);
      case "category":
        return updateProductCategory(productBaseId, op.value);
      case "webDescription":
        return updateListing(op.slug, { webDescription: op.value });
      case "dimensions":
        return updateListingDimensions(op.slug, {
          weightKg: op.weightKg ?? undefined,
          widthCm:  op.widthCm  ?? undefined,
          heightCm: op.heightCm ?? undefined,
          depthCm:  op.depthCm  ?? undefined,
        });
      case "sizeLabel":
        return updateSkuSizeLabel(op.skuId, op.value);
      case "price":
        return updateSkuPrice(op.skuId, op.value);
      case "stock":
        return updateSkuStock(op.skuId, { quantity: op.value });
    }
  }

  return { commit, isSaving };
}
