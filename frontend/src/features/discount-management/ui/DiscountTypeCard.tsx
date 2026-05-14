"use client";

import { Button } from "@/shared/ui/button";
import type { DiscountType } from "../model/types";
import { Pencil, Trash2, Layers } from "lucide-react";
import { cn } from "@/shared/lib/utils";

const ACCENT_COLORS = [
  "#F97316", // orange
  "#8B5CF6", // violet
  "#06B6D4", // cyan
  "#10B981", // emerald
  "#F59E0B", // amber
  "#EF4444", // rose
];

interface DiscountTypeCardProps {
  discountType: DiscountType;
  /** Position in the sorted list — drives accent color cycling */
  index: number;
  onEdit: () => void;
  onDelete: () => void;
}

export function DiscountTypeCard({
  discountType,
  index,
  onEdit,
  onDelete,
}: DiscountTypeCardProps) {
  const accentColor = ACCENT_COLORS[index % ACCENT_COLORS.length];

  return (
    <div className="relative overflow-hidden rounded-xl border bg-card group hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
      {/* Left accent bar */}
      <div
        className="absolute inset-y-0 left-0 w-[3px]"
        style={{ backgroundColor: accentColor }}
      />

      {/* Ghost rank watermark */}
      <span className="absolute bottom-0 right-3 text-[80px] font-black leading-none text-foreground/[0.04] select-none pointer-events-none">
        {discountType.rank}
      </span>

      <div className="px-5 py-4 pl-6">
        {/* Hover actions — top right */}
        <div className="absolute top-3 right-3 flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity duration-150">
          <Button
            size="icon"
            variant="ghost"
            className="h-7 w-7"
            onClick={onEdit}
          >
            <Pencil className="h-3.5 w-3.5" />
            <span className="sr-only">Edit {discountType.name}</span>
          </Button>
          <Button
            size="icon"
            variant="ghost"
            className="h-7 w-7 text-destructive hover:text-destructive hover:bg-destructive/10"
            onClick={onDelete}
          >
            <Trash2 className="h-3.5 w-3.5" />
            <span className="sr-only">Delete {discountType.name}</span>
          </Button>
        </div>

        {/* Name */}
        <p className="text-xl font-bold tracking-tight pr-16 truncate">
          {discountType.name}
        </p>

        {/* Badges row */}
        <div className="mt-2.5 flex flex-wrap items-center gap-1.5">
          <span className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium bg-muted text-muted-foreground">
            <span
              className="h-1.5 w-1.5 rounded-full shrink-0"
              style={{ backgroundColor: accentColor }}
            />
            Priority {discountType.rank}
          </span>
          <span
            className={cn(
              "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-medium border",
              discountType.stackingPolicy === "STACKABLE"
                ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                : "bg-slate-50 text-slate-600 border-slate-200"
            )}
          >
            <Layers className="h-2.5 w-2.5" />
            {discountType.stackingPolicy === "STACKABLE" ? "Stackable" : "Best wins"}
          </span>
        </div>
      </div>
    </div>
  );
}
