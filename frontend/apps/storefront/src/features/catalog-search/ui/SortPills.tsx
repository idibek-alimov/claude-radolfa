"use client";

import { cn } from "@radolfa/shared/lib/utils";
import type { ListingSort } from "@/entities/product";

const SORT_OPTIONS: { value: ListingSort; label: string }[] = [
  { value: "POPULAR", label: "Popular" },
  { value: "CHEAPEST", label: "Cheapest" },
  { value: "DEAREST", label: "Dearest" },
  { value: "RATING", label: "Rating" },
  { value: "NEWEST", label: "Newest" },
];

interface SortPillsProps {
  value: ListingSort;
  onChange: (sort: ListingSort) => void;
  variant?: "desktop" | "mobile";
}

export function SortPills({ value, onChange, variant = "desktop" }: SortPillsProps) {
  if (variant === "mobile") {
    return (
      <div className="flex gap-2 overflow-x-auto scrollbar-hide">
        {SORT_OPTIONS.map((option) => (
          <button
            key={option.value}
            onClick={() => onChange(option.value)}
            className={cn(
              "shrink-0 h-8 px-3.5 rounded-full text-[12px]",
              option.value === value
                ? "bg-mag text-white font-bold"
                : "bg-plum/50 hover:bg-plum font-semibold"
            )}
          >
            {option.label}
          </button>
        ))}
      </div>
    );
  }

  return (
    <div className="flex items-center gap-2 text-[13px]">
      <span className="text-ink/55">Sort:</span>
      {SORT_OPTIONS.map((option) => (
        <button
          key={option.value}
          onClick={() => onChange(option.value)}
          className={cn(
            "h-9 px-3.5 rounded-full",
            option.value === value
              ? "bg-mag text-white font-bold"
              : "bg-plum/50 hover:bg-plum font-semibold"
          )}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
