"use client";

import { X, Package2 } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { ScrollArea } from "@/shared/ui/scroll-area";

interface SelectedSkuEntry {
  skuCode: string;
  sizeLabel?: string;
  productCode?: string;
  colorDisplayName?: string;
  thumbnail?: string | null;
}

interface Props {
  selectedCodes: string[];
  /** Enriched entries keyed by skuCode — filled in as user browses */
  enriched: Map<string, SelectedSkuEntry>;
  onRemove: (code: string) => void;
  onClearAll: () => void;
}

export function SkuPickerSelectedPanel({
  selectedCodes,
  enriched,
  onRemove,
  onClearAll,
}: Props) {
  if (selectedCodes.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-full py-12 text-center text-muted-foreground gap-3">
        <Package2 className="h-10 w-10 opacity-30" />
        <p className="text-sm">No SKUs selected yet.</p>
        <p className="text-xs opacity-70">Search and pick products on the left.</p>
      </div>
    );
  }

  // Group selected by productCode+color (using enriched data if available)
  const groups = new Map<string, SelectedSkuEntry[]>();
  for (const code of selectedCodes) {
    const entry = enriched.get(code) ?? { skuCode: code };
    const key = entry.productCode
      ? `${entry.productCode}__${entry.colorDisplayName}`
      : "__raw";
    const group = groups.get(key) ?? [];
    group.push(entry);
    groups.set(key, group);
  }

  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center justify-between px-1 pb-2 flex-shrink-0">
        <span className="text-sm font-medium text-foreground">
          {selectedCodes.length} SKU{selectedCodes.length !== 1 ? "s" : ""} selected
        </span>
        <Button
          variant="ghost"
          size="sm"
          className="text-muted-foreground hover:text-destructive h-7 px-2 text-xs"
          onClick={onClearAll}
        >
          Clear all
        </Button>
      </div>

      <ScrollArea className="flex-1 min-h-0">
        <div className="space-y-2 pr-3">
          {Array.from(groups.entries()).map(([key, entries]) => {
            const first = entries[0];
            const isRaw = key === "__raw";
            return (
              <div key={key} className="rounded-lg border border-border p-2.5 bg-muted/30">
                {!isRaw && (
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-xs font-medium truncate">{first.productCode}</span>
                    {first.colorDisplayName && (
                      <span className="text-xs text-muted-foreground truncate">
                        · {first.colorDisplayName}
                      </span>
                    )}
                  </div>
                )}
                <div className="flex flex-wrap gap-1">
                  {entries.map((entry) => (
                    <span
                      key={entry.skuCode}
                      className="inline-flex items-center gap-1 text-xs bg-background border border-border rounded-full pl-2 pr-1 py-0.5"
                    >
                      <span className="font-medium">
                        {!isRaw && entry.sizeLabel ? entry.sizeLabel : entry.skuCode}
                      </span>
                      <button
                        onClick={() => onRemove(entry.skuCode)}
                        className="rounded-full hover:bg-destructive/10 hover:text-destructive transition-colors p-0.5"
                        aria-label={`Remove ${entry.skuCode}`}
                      >
                        <X className="h-2.5 w-2.5" />
                      </button>
                    </span>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      </ScrollArea>
    </div>
  );
}
