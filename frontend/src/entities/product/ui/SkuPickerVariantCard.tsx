"use client";

import Image from "next/image";
import { Checkbox } from "@/shared/ui/checkbox";
import { cn } from "@/shared/lib";
import type { ListingVariant } from "../model/types";

interface Props {
  variant: ListingVariant;
  selectedCodes: Set<string>;
  onToggleSku: (code: string) => void;
  onToggleAllSkus: (variant: ListingVariant) => void;
}

export function SkuPickerVariantCard({
  variant,
  selectedCodes,
  onToggleSku,
  onToggleAllSkus,
}: Props) {
  const totalSkus = variant.skus.length;
  const selectedCount = variant.skus.filter((s) =>
    selectedCodes.has(s.skuCode)
  ).length;
  const allSelected = selectedCount === totalSkus;
  const someSelected = selectedCount > 0 && selectedCount < totalSkus;

  const thumbnail = variant.images[0] ?? null;

  return (
    <div className="rounded-lg border border-border bg-card p-3 space-y-2 hover:border-primary/30 transition-colors">
      {/* Variant header */}
      <div className="flex items-center gap-3">
        <div
          className="flex items-center justify-center cursor-pointer"
          onClick={() => onToggleAllSkus(variant)}
        >
          <Checkbox
            checked={allSelected ? true : someSelected ? "indeterminate" : false}
            onCheckedChange={() => onToggleAllSkus(variant)}
          />
        </div>
        {thumbnail ? (
          <div className="relative h-12 w-12 rounded overflow-hidden flex-shrink-0 bg-muted">
            <Image
              src={thumbnail}
              alt={variant.colorDisplayName}
              fill
              className="object-cover"
              unoptimized
            />
          </div>
        ) : (
          <div className="h-12 w-12 rounded bg-muted flex-shrink-0" />
        )}
        <div className="min-w-0 flex-1">
          <p className="text-sm font-medium truncate">{variant.productCode}</p>
          <div className="flex items-center gap-1.5 mt-0.5">
            {variant.colorHex && (
              <span
                className="h-3 w-3 rounded-full border border-border flex-shrink-0"
                style={{ backgroundColor: `#${variant.colorHex}` }}
              />
            )}
            <span className="text-xs text-muted-foreground truncate">
              {variant.colorDisplayName}
            </span>
          </div>
        </div>
        {selectedCount > 0 && (
          <span className="text-xs font-medium text-primary bg-primary/10 rounded-full px-2 py-0.5 flex-shrink-0">
            {selectedCount}/{totalSkus}
          </span>
        )}
      </div>

      {/* SKU rows */}
      <div className="pl-7 space-y-1">
        {variant.skus.map((sku) => {
          const isSelected = selectedCodes.has(sku.skuCode);
          return (
            <div
              key={sku.skuCode}
              className={cn(
                "flex items-center gap-2 rounded px-2 py-1 cursor-pointer hover:bg-muted/50 transition-colors",
                isSelected && "bg-primary/5"
              )}
              onClick={() => onToggleSku(sku.skuCode)}
            >
              <Checkbox
                checked={isSelected}
                onCheckedChange={() => onToggleSku(sku.skuCode)}
              />
              <span className="text-sm flex-1">{sku.sizeLabel}</span>
              <span className="text-xs font-mono text-muted-foreground">
                {sku.skuCode}
              </span>
              <span className="text-xs text-muted-foreground">
                {sku.originalPrice.toLocaleString()} сом
              </span>
              {sku.stockQuantity === 0 && (
                <span className="text-xs text-destructive/70">нет</span>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
