"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Lock, Check, Loader2, Plus } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import {
  updateSkuSizeLabel,
  updateSkuPrice,
  updateSkuStock,
  addSku,
} from "@/entities/product/api/admin";
import { getErrorMessage } from "@/shared/lib";
import type { Sku } from "@/entities/product/model/types";

interface Props {
  slug: string;
  skus: Sku[];
  isAdmin: boolean;
  variantId: number;
}

export function SkuTableCard({ slug, skus, isAdmin, variantId }: Props) {
  const t = useTranslations("manage");
  const queryClient = useQueryClient();

  const [pendingSizes, setPendingSizes] = useState<Record<number, string>>({});
  const [pendingPrices, setPendingPrices] = useState<Record<number, string>>({});
  const [pendingStocks, setPendingStocks] = useState<Record<number, string>>({});

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["listing", slug] });
    queryClient.invalidateQueries({ queryKey: ["listings"] });
  };

  const sizeMutation = useMutation({
    mutationFn: ({ skuId, sizeLabel }: { skuId: number; sizeLabel: string }) =>
      updateSkuSizeLabel(skuId, sizeLabel),
    onSuccess: (_, { skuId }) => {
      invalidate();
      toast.success(t("sizeLabelUpdated"));
      setPendingSizes((prev) => { const next = { ...prev }; delete next[skuId]; return next; });
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const priceMutation = useMutation({
    mutationFn: ({ skuId, price }: { skuId: number; price: number }) =>
      updateSkuPrice(skuId, price),
    onSuccess: (_, { skuId }) => {
      invalidate();
      toast.success(t("priceUpdated"));
      setPendingPrices((prev) => { const next = { ...prev }; delete next[skuId]; return next; });
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const stockMutation = useMutation({
    mutationFn: ({ skuId, quantity }: { skuId: number; quantity: number }) =>
      updateSkuStock(skuId, { quantity }),
    onSuccess: (_, { skuId }) => {
      invalidate();
      toast.success(t("stockUpdated"));
      setPendingStocks((prev) => { const next = { ...prev }; delete next[skuId]; return next; });
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const handleSizeBlur = (skuId: number) => {
    const raw = pendingSizes[skuId];
    if (raw === undefined || !raw.trim()) return;
    sizeMutation.mutate({ skuId, sizeLabel: raw.trim() });
  };

  const handlePriceBlur = (skuId: number) => {
    const raw = pendingPrices[skuId];
    if (raw === undefined) return;
    const price = parseFloat(raw);
    if (isNaN(price) || price < 0) return;
    priceMutation.mutate({ skuId, price });
  };

  const handleStockSave = (skuId: number) => {
    const raw = pendingStocks[skuId];
    if (raw === undefined) return;
    const quantity = parseInt(raw, 10);
    if (isNaN(quantity) || quantity < 0) return;
    stockMutation.mutate({ skuId, quantity });
  };

  // ── Add new SKU ──────────────────────────────────────────────────
  const [showAddForm, setShowAddForm] = useState(false);
  const [newSize, setNewSize] = useState("");
  const [newPrice, setNewPrice] = useState("");
  const [newStock, setNewStock] = useState("");

  const addSkuMutation = useMutation({
    mutationFn: () =>
      addSku(variantId, newSize.trim(), parseFloat(newPrice), parseInt(newStock, 10)),
    onSuccess: () => {
      invalidate();
      toast.success(t("sizeAdded") || "Size added");
      setNewSize("");
      setNewPrice("");
      setNewStock("");
      setShowAddForm(false);
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const handleAddSku = () => {
    if (!newSize.trim()) return;
    const price = parseFloat(newPrice);
    const stock = parseInt(newStock, 10);
    if (isNaN(price) || price < 0 || isNaN(stock) || stock < 0) return;
    addSkuMutation.mutate();
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
              {isAdmin && <th className="w-16 px-2 py-2" />}
            </tr>
          </thead>
          <tbody>
            {skus.map((sku) => (
              <tr key={sku.skuId} className="border-b last:border-0">
                {/* Size label — editable for everyone */}
                <td className="px-2 py-1.5">
                  <Input
                    value={pendingSizes[sku.skuId] ?? sku.sizeLabel}
                    onChange={(e) =>
                      setPendingSizes((prev) => ({ ...prev, [sku.skuId]: e.target.value }))
                    }
                    onBlur={() => handleSizeBlur(sku.skuId)}
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
                        value={pendingPrices[sku.skuId] ?? sku.originalPrice.toFixed(2)}
                        onChange={(e) =>
                          setPendingPrices((prev) => ({ ...prev, [sku.skuId]: e.target.value }))
                        }
                        onBlur={() => handlePriceBlur(sku.skuId)}
                        className="h-7 w-28 text-sm"
                      />
                    </td>

                    {/* Stock — ADMIN only */}
                    <td className="px-2 py-1.5">
                      <Input
                        type="number"
                        min={0}
                        value={pendingStocks[sku.skuId] ?? sku.stockQuantity}
                        onChange={(e) =>
                          setPendingStocks((prev) => ({ ...prev, [sku.skuId]: e.target.value }))
                        }
                        className="h-7 w-20 text-sm"
                      />
                    </td>

                    <td className="px-2 py-1.5">
                      {pendingStocks[sku.skuId] !== undefined && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="h-7 px-2 text-xs"
                          disabled={stockMutation.isPending}
                          onClick={() => handleStockSave(sku.skuId)}
                        >
                          {stockMutation.isPending ? (
                            <Loader2 className="h-3 w-3 animate-spin" />
                          ) : (
                            <Check className="h-3 w-3" />
                          )}
                        </Button>
                      )}
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
                    <td className={`px-3 py-1.5 text-right font-medium ${sku.stockQuantity === 0 ? "text-destructive" : ""}`}>
                      <span className="flex items-center justify-end gap-1">
                        <Lock className="h-3 w-3 text-muted-foreground" />
                        {sku.stockQuantity}
                      </span>
                    </td>
                  </>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Add new SKU — ADMIN only */}
      {isAdmin && !showAddForm && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => setShowAddForm(true)}
        >
          <Plus className="h-4 w-4 mr-1" />
          {t("addSize") || "Add Size"}
        </Button>
      )}

      {isAdmin && showAddForm && (
        <div className="rounded-md border p-3 space-y-3 bg-muted/30">
          <p className="text-xs font-medium text-muted-foreground">{t("newSize") || "New Size"}</p>
          <div className="grid grid-cols-3 gap-2">
            <Input
              value={newSize}
              onChange={(e) => setNewSize(e.target.value)}
              placeholder={t("size") || "Size label"}
              className="h-8 text-sm"
            />
            <Input
              type="number"
              min={0}
              step={0.01}
              value={newPrice}
              onChange={(e) => setNewPrice(e.target.value)}
              placeholder={t("price") || "Price"}
              className="h-8 text-sm"
            />
            <Input
              type="number"
              min={0}
              value={newStock}
              onChange={(e) => setNewStock(e.target.value)}
              placeholder={t("stock") || "Stock"}
              className="h-8 text-sm"
            />
          </div>
          <div className="flex gap-2 justify-end">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setShowAddForm(false)}
            >
              {t("cancel") || "Cancel"}
            </Button>
            <Button
              type="button"
              size="sm"
              onClick={handleAddSku}
              disabled={addSkuMutation.isPending || !newSize.trim()}
            >
              {addSkuMutation.isPending ? (
                <Loader2 className="h-3.5 w-3.5 animate-spin mr-1" />
              ) : (
                <Plus className="h-3.5 w-3.5 mr-1" />
              )}
              {t("add") || "Add"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
