"use client";

import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetClose,
} from "@radolfa/shared/ui/sheet";
import { FilterPanel } from "./FilterPanel";
import type { CatalogCriteria, CatalogFacets, ListingSort } from "@/entities/product";

const SORT_OPTIONS: { value: ListingSort; label: string }[] = [
  { value: "POPULAR", label: "Popular" },
  { value: "CHEAPEST", label: "Cheapest" },
  { value: "DEAREST", label: "Dearest" },
  { value: "RATING", label: "By rating" },
  { value: "NEWEST", label: "Newest" },
  { value: "BIGGEST_DISCOUNT", label: "Biggest discount" },
];

interface CatalogSheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mode: "filter" | "sort";
  resultCount: number;
  // Filter mode
  facets?: CatalogFacets;
  draftValue?: CatalogCriteria;
  onDraftChange?: (patch: Partial<CatalogCriteria>) => void;
  onReset?: () => void;
  selectedCategorySlug?: string | null;
  onCategorySelect?: (slug: string | null) => void;
  onApplyFilters?: () => void;
  // Sort mode
  sortValue?: ListingSort;
  onSortChange?: (sort: ListingSort) => void;
}

export function CatalogSheet({
  open,
  onOpenChange,
  mode,
  resultCount,
  facets,
  draftValue,
  onDraftChange,
  onReset,
  selectedCategorySlug,
  onCategorySelect,
  onApplyFilters,
  sortValue,
  onSortChange,
}: CatalogSheetProps) {
  const title = mode === "filter" ? "Filters" : "Sort";

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="bottom"
        className="rounded-t-2xl p-0 gap-0 [&>button]:hidden"
        style={{ maxHeight: "82%" }}
      >
        <SheetHeader className="px-4 py-3 flex-row items-center justify-between border-b border-ink/8 space-y-0">
          <SheetTitle className="font-black text-[16px]">{title}</SheetTitle>
          <SheetClose asChild>
            <button className="w-8 h-8 rounded-full bg-plum/50 flex items-center justify-center">
              ✕
            </button>
          </SheetClose>
        </SheetHeader>

        <div className="p-4 overflow-y-auto" style={{ maxHeight: "64vh" }}>
          {mode === "filter" && draftValue && onDraftChange && onReset && (
            <FilterPanel
              facets={facets}
              value={draftValue}
              onChange={onDraftChange}
              onReset={onReset}
              selectedCategorySlug={selectedCategorySlug}
              onCategorySelect={onCategorySelect}
              showApply={false}
            />
          )}

          {mode === "sort" && sortValue && onSortChange && (
            <ul className="space-y-1 text-[14px]">
              {SORT_OPTIONS.map((option) => (
                <li key={option.value}>
                  <label
                    className="flex items-center justify-between py-2.5 cursor-pointer"
                    onClick={() => onSortChange(option.value)}
                  >
                    <span>{option.label}</span>
                    {option.value === sortValue ? (
                      <span className="text-mag text-lg">●</span>
                    ) : (
                      <span className="w-4 h-4 rounded-full border border-ink/25" />
                    )}
                  </label>
                </li>
              ))}
            </ul>
          )}
        </div>

        {mode === "filter" && (
          <div className="px-4 pb-4">
            <SheetClose asChild>
              <button
                onClick={onApplyFilters}
                className="mt-4 w-full h-11 rounded-full bg-mag text-white font-bold text-[14px]"
              >
                Show {resultCount} results
              </button>
            </SheetClose>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
