"use client";

import { Plus, X } from "lucide-react";
import { cn } from "@/shared/lib/utils";

export interface VariantTabBarItem<TId extends string | number> {
  id: TId;
  colorHex: string;
  label: string;
  subtitle?: string;
  errored?: boolean;
}

interface Props<TId extends string | number> {
  items: VariantTabBarItem<TId>[];
  activeId: TId;
  onSelect: (id: TId) => void;
  onRemove?: (id: TId) => void;
  onAdd?: () => void;
  addLabel?: string;
  className?: string;
}

export function VariantTabBar<TId extends string | number>({
  items,
  activeId,
  onSelect,
  onRemove,
  onAdd,
  addLabel = "Add Color",
  className,
}: Props<TId>) {
  return (
    <div className={cn("flex flex-wrap items-center gap-2", className)}>
      {items.map((item) => {
        const isActive = item.id === activeId;
        return (
          <div key={item.id} className="relative group">
            <button
              type="button"
              onClick={() => onSelect(item.id)}
              className={cn(
                "flex items-center gap-2 text-sm font-medium transition-all border rounded-lg",
                onRemove ? "pl-3 pr-8 py-2" : "px-3 py-2",
                isActive
                  ? "border-primary bg-primary/5 text-primary shadow-sm"
                  : "border-border bg-white hover:border-primary/40 text-foreground"
              )}
            >
              <span
                className="h-3.5 w-3.5 rounded-full border border-black/10 shrink-0"
                style={{ backgroundColor: item.colorHex || "#e5e7eb" }}
              />
              <span className="max-w-[120px] truncate">{item.label}</span>
              {item.subtitle && (
                <span
                  className={cn(
                    "text-xs",
                    isActive ? "text-primary/70" : "text-muted-foreground"
                  )}
                >
                  {item.subtitle}
                </span>
              )}
              {item.errored && (
                <span className="h-2 w-2 rounded-full bg-destructive shrink-0" />
              )}
            </button>

            {onRemove && (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  onRemove(item.id);
                }}
                className="absolute right-1.5 top-1/2 -translate-y-1/2 h-5 w-5 flex items-center justify-center rounded text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
                aria-label={`Remove ${item.label}`}
              >
                <X className="h-3 w-3" />
              </button>
            )}
          </div>
        );
      })}

      {onAdd && (
        <button
          type="button"
          onClick={onAdd}
          className="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-dashed border-muted-foreground/40 text-sm text-muted-foreground hover:border-primary/50 hover:text-primary hover:bg-primary/5 transition-colors"
        >
          <Plus className="h-3.5 w-3.5" />
          {addLabel}
        </button>
      )}
    </div>
  );
}
