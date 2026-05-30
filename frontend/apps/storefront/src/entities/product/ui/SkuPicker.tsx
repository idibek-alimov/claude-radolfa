"use client";

import { useEffect, useMemo, useState } from "react";
import { Search, ChevronLeft, ChevronRight, Loader2 } from "lucide-react";
import { Input } from "@/shared/ui/input";
import { Button } from "@/shared/ui/button";
import { SkuPickerVariantCard } from "./SkuPickerVariantCard";
import { SkuPickerSelectedPanel } from "./SkuPickerSelectedPanel";
import { useSkuSearch } from "../api/useSkuSearch";
import type { ListingVariant } from "../model/types";

interface SkuPickerProps {
  selectedCodes: string[];
  onSelectionChange: (codes: string[]) => void;
}

export function SkuPicker({ selectedCodes, onSelectionChange }: SkuPickerProps) {
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(1);
  const { data, isLoading, isFetching } = useSkuSearch(query, page);

  // Set for O(1) lookup
  const selectedSet = useMemo(() => new Set(selectedCodes), [selectedCodes]);

  // Enrichment map: as user browses, we record display info for each encountered SKU
  const [enriched, setEnriched] = useState<Map<string, {
    skuCode: string;
    sizeLabel: string;
    productCode: string;
    colorDisplayName: string;
    thumbnail: string | null;
  }>>(new Map());

  // Hydrate the enrichment map whenever new results arrive (effect, not render body)
  const results = data?.content ?? [];
  useEffect(() => {
    if (!data?.content || data.content.length === 0) return;
    setEnriched((prev) => {
      const next = new Map(prev);
      let changed = false;
      for (const variant of data.content) {
        for (const sku of variant.skus) {
          if (!next.has(sku.skuCode)) {
            next.set(sku.skuCode, {
              skuCode: sku.skuCode,
              sizeLabel: sku.sizeLabel,
              productCode: variant.productCode,
              colorDisplayName: variant.colorDisplayName,
              thumbnail: variant.images[0] ?? null,
            });
            changed = true;
          }
        }
      }
      return changed ? next : prev;
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data?.content]);

  const toggle = (code: string) => {
    const next = new Set(selectedSet);
    if (next.has(code)) {
      next.delete(code);
    } else {
      next.add(code);
    }
    onSelectionChange(Array.from(next));
  };

  const toggleAll = (variant: ListingVariant) => {
    const variantCodes = variant.skus.map((s) => s.skuCode);
    const allSelected = variantCodes.every((c) => selectedSet.has(c));
    const next = new Set(selectedSet);
    if (allSelected) {
      variantCodes.forEach((c) => next.delete(c));
    } else {
      variantCodes.forEach((c) => next.add(c));
    }
    onSelectionChange(Array.from(next));
  };

  const remove = (code: string) => {
    onSelectionChange(selectedCodes.filter((c) => c !== code));
  };

  const clearAll = () => onSelectionChange([]);

  const handleQueryChange = (value: string) => {
    setQuery(value);
    setPage(1);
  };

  return (
    <div className="flex flex-col md:grid md:grid-cols-[1fr_300px] gap-4 h-full min-h-[480px]">
      {/* Left panel: search + results */}
      <div className="flex flex-col min-h-0">
        {/* Search input */}
        <div className="relative mb-3 flex-shrink-0">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
          <Input
            value={query}
            onChange={(e) => handleQueryChange(e.target.value)}
            placeholder="Search by product name or SKU code…"
            className="pl-9"
          />
          {isFetching && (
            <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 animate-spin text-muted-foreground" />
          )}
        </div>

        {/* Results count */}
        {!isLoading && data && (
          <p className="text-xs text-muted-foreground mb-2 flex-shrink-0">
            {data.totalElements} product{data.totalElements !== 1 ? "s" : ""} found
          </p>
        )}

        {/* Result cards */}
        <div className="flex-1 overflow-y-auto space-y-2 pr-1">
          {isLoading ? (
            <div className="flex items-center justify-center py-12 text-muted-foreground">
              <Loader2 className="h-6 w-6 animate-spin" />
            </div>
          ) : results.length === 0 ? (
            <div className="flex items-center justify-center py-12 text-sm text-muted-foreground">
              No products found.
            </div>
          ) : (
            results.map((variant) => (
              <SkuPickerVariantCard
                key={variant.variantId}
                variant={variant}
                selectedCodes={selectedSet}
                onToggleSku={toggle}
                onToggleAllSkus={toggleAll}
              />
            ))
          )}
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between pt-2 flex-shrink-0">
            <Button
              variant="outline"
              size="sm"
              disabled={page <= 1}
              onClick={() => setPage((p) => p - 1)}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="text-xs text-muted-foreground">
              {page} / {data.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={data.last}
              onClick={() => setPage((p) => p + 1)}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        )}
      </div>

      {/* Right panel: selected */}
      <div className="border-t md:border-t-0 md:border-l border-border pt-4 md:pt-0 md:pl-4 min-h-0 flex flex-col">
        <SkuPickerSelectedPanel
          selectedCodes={selectedCodes}
          enriched={enriched}
          onRemove={remove}
          onClearAll={clearAll}
        />
      </div>
    </div>
  );
}
