"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Lock, Save, Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { fetchCategoryTree } from "@/entities/product/api";
import { updateProductName, updateProductCategory } from "@/entities/product/api/admin";
import { getErrorMessage } from "@/shared/lib";
import type { ListingVariantDetail, CategoryTree } from "@/entities/product/model/types";

interface Props {
  detail: ListingVariantDetail;
}

function flattenTree(nodes: CategoryTree[], depth = 0): { id: number; name: string; depth: number }[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, depth },
    ...flattenTree(node.children, depth + 1),
  ]);
}

export function GeneralInfoCard({ detail }: Props) {
  const t = useTranslations("manage");
  const queryClient = useQueryClient();

  const [name, setName] = useState(detail.colorDisplayName);
  const [categoryId, setCategoryId] = useState<number | "">(detail.categoryId ?? "");

  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategoryTree,
  });

  const flatCategories = flattenTree(categories ?? []);

  const nameMutation = useMutation({
    mutationFn: () => updateProductName(detail.productBaseId, name.trim()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listing", detail.slug] });
      queryClient.invalidateQueries({ queryKey: ["listings"] });
      toast.success(t("productNameUpdated"));
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const categoryMutation = useMutation({
    mutationFn: () => updateProductCategory(detail.productBaseId, categoryId as number),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listing", detail.slug] });
      queryClient.invalidateQueries({ queryKey: ["listings"] });
      toast.success(t("productCategoryUpdated"));
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className="bg-card rounded-xl border shadow-sm p-6 space-y-5">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
        {t("generalInfo")}
      </h2>

      {/* Product Name */}
      <div className="space-y-1.5">
        <label className="text-sm font-medium">{t("productName")}</label>
        <div className="flex gap-2">
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder={t("productNamePlaceholder")}
          />
          <Button
            size="sm"
            onClick={() => nameMutation.mutate()}
            disabled={nameMutation.isPending || !name.trim() || name.trim() === detail.colorDisplayName}
          >
            {nameMutation.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
          </Button>
        </div>
      </div>

      {/* Category */}
      <div className="space-y-1.5">
        <label className="text-sm font-medium">{t("category")}</label>
        <div className="flex gap-2">
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
          <Button
            size="sm"
            onClick={() => categoryMutation.mutate()}
            disabled={categoryMutation.isPending || categoryId === "" || categoryId === detail.categoryId}
          >
            {categoryMutation.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
          </Button>
        </div>
      </div>

      {/* Color — read-only */}
      <div className="space-y-1.5">
        <label className="text-sm font-medium flex items-center gap-1.5">
          <Lock className="h-3.5 w-3.5 text-muted-foreground" />
          {t("color")}
        </label>
        <div className="flex items-center gap-2 h-9 px-3 rounded-md border border-input bg-muted/40 text-sm text-muted-foreground">
          {detail.colorHex && (
            <span
              className="h-4 w-4 rounded-full border border-border flex-shrink-0"
              style={{ background: detail.colorHex }}
            />
          )}
          {detail.colorDisplayName} ({detail.colorKey})
          <span className="ml-auto text-xs">
            {t("colorPermanentHint")}
          </span>
        </div>
      </div>

      {/* Slug — read-only */}
      <div className="space-y-1.5">
        <label className="text-sm font-medium flex items-center gap-1.5">
          <Lock className="h-3.5 w-3.5 text-muted-foreground" />
          {t("slug")}
        </label>
        <code className="flex h-9 items-center px-3 rounded-md border border-input bg-muted/40 text-xs text-muted-foreground">
          {detail.slug}
        </code>
      </div>
    </div>
  );
}
