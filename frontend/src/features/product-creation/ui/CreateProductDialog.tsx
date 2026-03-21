"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus, Trash2, Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { fetchCategoryTree } from "@/entities/product/api";
import { fetchColors } from "@/entities/color";
import { createProduct } from "@/entities/product/api/admin";
import type { SkuDefinition } from "@/entities/product/api/admin";
import { getErrorMessage } from "@/shared/lib";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: (slug: string) => void;
}

interface SkuRow extends SkuDefinition {
  _key: number;
}

let rowCounter = 0;

function makeRow(): SkuRow {
  return { _key: rowCounter++, sizeLabel: "", price: 0, stockQuantity: 0 };
}

export function CreateProductDialog({ open, onOpenChange, onCreated }: Props) {
  const t = useTranslations("manage");
  const queryClient = useQueryClient();

  const [name, setName] = useState("");
  const [categoryId, setCategoryId] = useState<number | "">("");
  const [colorId, setColorId] = useState<number | "">("");
  const [skus, setSkus] = useState<SkuRow[]>([makeRow()]);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategoryTree,
  });

  const { data: colors } = useQuery({
    queryKey: ["colors"],
    queryFn: fetchColors,
  });

  // Flatten category tree into a select-friendly list
  const flatCategories: { id: number; name: string; depth: number }[] = [];
  function flattenTree(nodes: typeof categories, depth = 0) {
    if (!nodes) return;
    for (const node of nodes) {
      flatCategories.push({ id: node.id, name: node.name, depth });
      flattenTree(node.children, depth + 1);
    }
  }
  flattenTree(categories);

  const mutation = useMutation({
    mutationFn: createProduct,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
      toast.success(t("productCreated", { slug: data.slug }));
      handleClose();
      onCreated?.(data.slug);
    },
    onError: (err: unknown) => {
      toast.error(getErrorMessage(err));
    },
  });

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (!name.trim()) errs.name = t("fieldRequired");
    if (categoryId === "") errs.categoryId = t("fieldRequired");
    if (colorId === "") errs.colorId = t("fieldRequired");
    if (skus.length === 0) errs.skus = t("skuAtLeastOne");
    for (let i = 0; i < skus.length; i++) {
      if (!skus[i].sizeLabel.trim()) errs[`sku_size_${i}`] = t("fieldRequired");
      if (skus[i].price < 0) errs[`sku_price_${i}`] = t("fieldNonNegative");
      if (skus[i].stockQuantity < 0) errs[`sku_stock_${i}`] = t("fieldNonNegative");
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function handleSubmit() {
    if (!validate()) return;
    mutation.mutate({
      name: name.trim(),
      categoryId: categoryId as number,
      colorId: colorId as number,
      skus: skus.map(({ sizeLabel, price, stockQuantity }) => ({
        sizeLabel,
        price,
        stockQuantity,
      })),
    });
  }

  function handleClose() {
    setName("");
    setCategoryId("");
    setColorId("");
    setSkus([makeRow()]);
    setErrors({});
    onOpenChange(false);
  }

  function addSkuRow() {
    setSkus((prev) => [...prev, makeRow()]);
  }

  function removeSkuRow(key: number) {
    setSkus((prev) => prev.filter((r) => r._key !== key));
  }

  function updateSkuRow(key: number, field: keyof SkuDefinition, value: string | number) {
    setSkus((prev) =>
      prev.map((r) => (r._key === key ? { ...r, [field]: value } : r))
    );
  }

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-xl px-8">
        <DialogHeader>
          <DialogTitle>{t("createProductTitle")}</DialogTitle>
          <DialogDescription>{t("createProductDesc")}</DialogDescription>
        </DialogHeader>

        <div className="space-y-5 py-2 max-h-[70vh] overflow-y-auto pr-1">
          {/* ── Basic Info ─────────────────────────────────────── */}
          <div className="space-y-3">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              {t("createStepBasicInfo")}
            </p>

            <div className="space-y-1">
              <label className="text-sm font-medium">{t("productName")}</label>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={t("productNamePlaceholder")}
              />
              {errors.name && <p className="text-xs text-destructive">{errors.name}</p>}
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-sm font-medium">{t("category")}</label>
                <select
                  value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : "")}
                  className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                >
                  <option value="">{t("selectCategory")}</option>
                  {flatCategories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {"  ".repeat(c.depth)}{c.name}
                    </option>
                  ))}
                </select>
                {errors.categoryId && (
                  <p className="text-xs text-destructive">{errors.categoryId}</p>
                )}
              </div>

              <div className="space-y-1">
                <label className="text-sm font-medium">{t("color")}</label>
                <select
                  value={colorId}
                  onChange={(e) => setColorId(e.target.value ? Number(e.target.value) : "")}
                  className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                >
                  <option value="">{t("selectColor")}</option>
                  {colors?.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.displayName ?? c.colorKey}
                    </option>
                  ))}
                </select>
                {errors.colorId && (
                  <p className="text-xs text-destructive">{errors.colorId}</p>
                )}
              </div>
            </div>
          </div>

          {/* ── SKUs ───────────────────────────────────────────── */}
          <div className="border-t pt-4 space-y-3">
            <div className="flex items-center justify-between">
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                {t("createStepSkus")}
              </p>
              <Button type="button" variant="outline" size="sm" className="gap-1" onClick={addSkuRow}>
                <Plus className="h-3.5 w-3.5" />
                {t("addSize")}
              </Button>
            </div>

            {errors.skus && <p className="text-xs text-destructive">{errors.skus}</p>}

            <div className="rounded-md border text-sm overflow-hidden">
              <table className="w-full">
                <thead>
                  <tr className="border-b bg-muted/50">
                    <th className="text-left px-3 py-2 text-xs font-medium text-muted-foreground">{t("size")}</th>
                    <th className="text-left px-3 py-2 text-xs font-medium text-muted-foreground">{t("price")} (TJS)</th>
                    <th className="text-left px-3 py-2 text-xs font-medium text-muted-foreground">{t("stock")}</th>
                    <th className="w-8" />
                  </tr>
                </thead>
                <tbody>
                  {skus.map((row, i) => (
                    <tr key={row._key} className="border-b last:border-0">
                      <td className="px-2 py-1.5">
                        <Input
                          value={row.sizeLabel}
                          onChange={(e) => updateSkuRow(row._key, "sizeLabel", e.target.value)}
                          placeholder="S / M / 42..."
                          className={`h-7 text-sm ${errors[`sku_size_${i}`] ? "border-destructive" : ""}`}
                        />
                      </td>
                      <td className="px-2 py-1.5">
                        <Input
                          type="number"
                          min={0}
                          step={0.01}
                          value={row.price}
                          onChange={(e) => updateSkuRow(row._key, "price", parseFloat(e.target.value) || 0)}
                          className={`h-7 text-sm ${errors[`sku_price_${i}`] ? "border-destructive" : ""}`}
                        />
                      </td>
                      <td className="px-2 py-1.5">
                        <Input
                          type="number"
                          min={0}
                          value={row.stockQuantity}
                          onChange={(e) => updateSkuRow(row._key, "stockQuantity", parseInt(e.target.value, 10) || 0)}
                          className={`h-7 text-sm ${errors[`sku_stock_${i}`] ? "border-destructive" : ""}`}
                        />
                      </td>
                      <td className="px-1 py-1.5 text-center">
                        {skus.length > 1 && (
                          <button
                            type="button"
                            onClick={() => removeSkuRow(row._key)}
                            className="text-muted-foreground hover:text-destructive transition-colors"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose}>
            {t("cancel")}
          </Button>
          <Button onClick={handleSubmit} disabled={mutation.isPending}>
            {mutation.isPending ? (
              <>
                <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
                {t("creating")}
              </>
            ) : (
              t("createProduct")
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
