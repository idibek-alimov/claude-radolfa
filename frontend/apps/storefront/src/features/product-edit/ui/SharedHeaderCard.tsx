"use client";

import { useQuery } from "@tanstack/react-query";
import { Lock, Users } from "lucide-react";
import { useTranslations } from "next-intl";
import { Input } from "@/shared/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/shared/ui/tooltip";
import { fetchCategoryTree } from "@/entities/product/api";
import { useDraft } from "../model/ProductCardDraftContext";
import type { ProductCard, CategoryTree } from "@/entities/product/model/types";

interface Props {
  card: ProductCard;
}

function flattenTree(
  nodes: CategoryTree[],
  depth = 0
): { id: number; name: string; depth: number }[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, depth },
    ...flattenTree(node.children, depth + 1),
  ]);
}

function SharedBadge() {
  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <span className="inline-flex items-center gap-0.5 text-xs text-muted-foreground bg-muted/60 rounded px-1 py-0.5 cursor-default">
            <Users className="h-3 w-3" />
            shared
          </span>
        </TooltipTrigger>
        <TooltipContent>Applies to all colors</TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}

export function SharedHeaderCard({ card }: Props) {
  const t = useTranslations("manage");
  const { draft, updateName, updateCategory } = useDraft();

  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategoryTree,
  });

  const flatCategories = flattenTree(categories ?? []);

  return (
    <div className="bg-card border-b shadow-sm px-8 py-5">
      <div className="flex items-center gap-2 mb-4">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
          {t("generalInfo")}
        </h2>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {/* Product name */}
        <div className="space-y-1.5">
          <label className="text-sm font-medium flex items-center gap-1.5">
            {t("productName")} <SharedBadge />
          </label>
          <Input
            value={draft.name}
            onChange={(e) => updateName(e.target.value)}
            placeholder={t("productNamePlaceholder")}
          />
        </div>

        {/* Category */}
        <div className="space-y-1.5">
          <label className="text-sm font-medium flex items-center gap-1.5">
            {t("category")} <SharedBadge />
          </label>
          <Select
            value={draft.categoryId != null ? String(draft.categoryId) : ""}
            onValueChange={(v) => updateCategory(v ? Number(v) : null)}
          >
            <SelectTrigger className="flex-1">
              <SelectValue placeholder={t("selectCategory")} />
            </SelectTrigger>
            <SelectContent>
              {flatCategories.map((c) => (
                <SelectItem
                  key={c.id}
                  value={String(c.id)}
                  style={{ paddingLeft: `${12 + c.depth * 16}px` }}
                >
                  {c.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Brand — read-only (no edit endpoint) */}
        <div className="space-y-1.5">
          <label className="text-sm font-medium flex items-center gap-1.5">
            <Lock className="h-3.5 w-3.5 text-muted-foreground" />
            Brand <SharedBadge />
          </label>
          <div className="flex h-9 items-center px-3 rounded-md border border-input bg-muted/40 text-sm text-muted-foreground">
            {card.brand ?? <span className="italic">— Catalog Data</span>}
          </div>
        </div>
      </div>
    </div>
  );
}
