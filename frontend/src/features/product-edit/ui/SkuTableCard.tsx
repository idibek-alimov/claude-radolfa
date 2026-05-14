"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Lock, Loader2, Plus, X, Check } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { addSkuToVariant } from "@/entities/product/api/admin";
import { getErrorMessage } from "@/shared/lib";
import { useDraft } from "../model/ProductCardDraftContext";
import type { ProductCardSku } from "@/entities/product/model/types";

interface Props {
  slug: string;
  productBaseId: number;
  variantId: number;
  skus: ProductCardSku[];
  isAdmin: boolean;
}

export function SkuTableCard({ slug, productBaseId, variantId, skus, isAdmin }: Props) {
  const t = useTranslations("manage");
  const queryClient = useQueryClient();
  const { draft, updateSkuField } = useDraft();

  // Add Size form state (stays local — creation action, not an edit)
  const [addingSize, setAddingSize] = useState(false);
  const [newSizeLabel, setNewSizeLabel] = useState("");
  const [newPrice, setNewPrice] = useState("0");
  const [newStock, setNewStock] = useState("0");

  const variantDraft = draft.variants[variantId];

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["admin-product", productBaseId] });
    queryClient.invalidateQueries({ queryKey: ["listing", slug] });
    queryClient.invalidateQueries({ queryKey: ["listings"] });
  };

  const addSkuMutation = useMutation({
    mutationFn: ({
      sizeLabel,
      price,
      stockQuantity,
    }: {
      sizeLabel: string;
      price: number;
      stockQuantity: number;
    }) => addSkuToVariant(productBaseId, variantId, { sizeLabel, price, stockQuantity }),
    onSuccess: () => {
      invalidate();
      toast.success("Size added");
      setAddingSize(false);
      setNewSizeLabel("");
      setNewPrice("0");
      setNewStock("0");
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const handleAddSkuSubmit = () => {
    const trimmedLabel = newSizeLabel.trim();
    if (!trimmedLabel) return;
    const price = parseFloat(newPrice);
    const stockQuantity = parseInt(newStock, 10);
    addSkuMutation.mutate({
      sizeLabel: trimmedLabel,
      price: isNaN(price) || price < 0 ? 0 : price,
      stockQuantity: isNaN(stockQuantity) || stockQuantity < 0 ? 0 : stockQuantity,
    });
  };

  const cancelAddSize = () => {
    setAddingSize(false);
    setNewSizeLabel("");
    setNewPrice("0");
    setNewStock("0");
  };

  return (
    <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
          {t("sizesAndStock")}
        </h2>
        {!isAdmin && (
          <span className="flex items-center gap-1 text-xs text-muted-foreground">
            <Lock className="h-3 w-3" />
            {t("priceStockAdminOnly")}
          </span>
        )}
      </div>

      <div className="rounded-md border text-sm overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b bg-muted/50">
              <th className="text-left px-3 py-2 text-xs font-medium text-muted-foreground w-[180px]">
                {t("size")}
              </th>
              <th className={`px-3 py-2 text-xs font-medium text-muted-foreground ${isAdmin ? "text-left" : "text-right"}`}>
                {t("price")} (TJS)
              </th>
              <th className={`px-3 py-2 text-xs font-medium text-muted-foreground ${isAdmin ? "text-left" : "text-right"}`}>
                {t("stock")}
              </th>
            </tr>
          </thead>
          <tbody>
            {skus.map((sku) => {
              const skuDraft = variantDraft?.skus[sku.skuId];
              return (
                <tr key={sku.skuId} className="border-b last:border-0">
                  {/* Size label — editable for everyone */}
                  <td className="px-2 py-1.5">
                    <Input
                      value={skuDraft?.sizeLabel ?? sku.sizeLabel}
                      onChange={(e) =>
                        updateSkuField(variantId, sku.skuId, "sizeLabel", e.target.value)
                      }
                      className="h-7 text-sm"
                    />
                  </td>

                  {isAdmin ? (
                    <>
                      {/* Price — ADMIN only */}
                      <td className="px-2 py-1.5">
                        <Input
                          type="number"
                          min={0}
                          step={0.01}
                          value={skuDraft?.price ?? sku.originalPrice}
                          onChange={(e) =>
                            updateSkuField(
                              variantId,
                              sku.skuId,
                              "price",
                              parseFloat(e.target.value) || 0
                            )
                          }
                          className="h-7 w-28 text-sm"
                        />
                      </td>

                      {/* Stock — ADMIN only */}
                      <td className="px-2 py-1.5">
                        <Input
                          type="number"
                          min={0}
                          value={skuDraft?.stockQuantity ?? sku.stockQuantity}
                          onChange={(e) =>
                            updateSkuField(
                              variantId,
                              sku.skuId,
                              "stockQuantity",
                              parseInt(e.target.value, 10) || 0
                            )
                          }
                          className="h-7 w-20 text-sm"
                        />
                      </td>
                    </>
                  ) : (
                    <>
                      <td className="px-3 py-1.5 text-right text-muted-foreground">
                        <span className="flex items-center justify-end gap-1">
                          <Lock className="h-3 w-3" />
                          {sku.originalPrice.toFixed(2)} TJS
                        </span>
                      </td>
                      <td
                        className={`px-3 py-1.5 text-right font-medium ${
                          sku.stockQuantity === 0 ? "text-destructive" : ""
                        }`}
                      >
                        <span className="flex items-center justify-end gap-1">
                          <Lock className="h-3 w-3 text-muted-foreground" />
                          {sku.stockQuantity}
                        </span>
                      </td>
                    </>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Add Size — ADMIN only */}
      {isAdmin && (
        <div>
          {addingSize ? (
            <div className="border rounded-lg p-3 bg-muted/30 space-y-3">
              <p className="text-xs font-medium text-muted-foreground">New size</p>
              <div className="grid grid-cols-[1fr_auto_auto] gap-2 items-end">
                <div className="space-y-1">
                  <label className="text-xs text-muted-foreground">Size label *</label>
                  <Input
                    autoFocus
                    placeholder="e.g. S / 42 / ONE_SIZE"
                    value={newSizeLabel}
                    onChange={(e) => setNewSizeLabel(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") handleAddSkuSubmit();
                      if (e.key === "Escape") cancelAddSize();
                    }}
                    className="h-8 text-sm"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-xs text-muted-foreground">Price (TJS)</label>
                  <Input
                    type="number"
                    min={0}
                    step={0.01}
                    value={newPrice}
                    onChange={(e) => setNewPrice(e.target.value)}
                    className="h-8 w-28 text-sm"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-xs text-muted-foreground">Stock</label>
                  <Input
                    type="number"
                    min={0}
                    value={newStock}
                    onChange={(e) => setNewStock(e.target.value)}
                    className="h-8 w-20 text-sm"
                  />
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  onClick={handleAddSkuSubmit}
                  disabled={!newSizeLabel.trim() || addSkuMutation.isPending}
                >
                  {addSkuMutation.isPending ? (
                    <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />
                  ) : (
                    <Check className="h-3.5 w-3.5 mr-1.5" />
                  )}
                  Add
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={cancelAddSize}
                  disabled={addSkuMutation.isPending}
                >
                  <X className="h-3.5 w-3.5 mr-1.5" />
                  Cancel
                </Button>
              </div>
            </div>
          ) : (
            <Button
              size="sm"
              variant="outline"
              onClick={() => setAddingSize(true)}
              className="w-full border-dashed"
            >
              <Plus className="h-3.5 w-3.5 mr-1.5" />
              Add Size
            </Button>
          )}
        </div>
      )}
    </div>
  );
}
