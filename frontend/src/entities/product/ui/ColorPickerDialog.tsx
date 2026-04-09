"use client";

import { useState } from "react";
import { Palette } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { cn } from "@/shared/lib/utils";
import type { Color } from "@/entities/product/model/types";

interface ColorPickerDialogProps {
  open: boolean;
  onClose: () => void;
  colors: Color[];
  usedColorIds: Set<number>;
  onSelect: (colorId: number) => void;
}

export function ColorPickerDialog({
  open,
  onClose,
  colors,
  usedColorIds,
  onSelect,
}: ColorPickerDialogProps) {
  const available = colors.filter((c) => !usedColorIds.has(c.id));
  const [hoveredId, setHoveredId] = useState<number | null>(null);

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent
        className="max-w-[420px] p-0 gap-0 border-0 shadow-2xl rounded-2xl overflow-hidden bg-white"
        overlayClassName="bg-black/20"
      >
        {/* Header */}
        <div className="px-6 py-5 border-b border-gray-100">
          <DialogHeader>
            <DialogTitle className="text-[15px] font-semibold text-gray-900 tracking-tight">
              Choose a color
            </DialogTitle>
            {available.length > 0 && (
              <p className="text-xs text-gray-400 mt-0.5">
                {available.length} color{available.length !== 1 ? "s" : ""} available
              </p>
            )}
          </DialogHeader>
        </div>

        {/* Content */}
        {available.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-4 py-14 px-6">
            <div className="h-16 w-16 rounded-2xl bg-gradient-to-b from-gray-50 to-gray-100 border border-gray-100 flex items-center justify-center">
              <Palette className="h-7 w-7 text-gray-300" />
            </div>
            <div className="text-center space-y-1">
              <p className="text-sm font-semibold text-gray-700">All colors assigned</p>
              <p className="text-xs text-gray-400 leading-relaxed max-w-[200px]">
                Every available color has been added as a variant.
              </p>
            </div>
          </div>
        ) : (
          <div className="p-4 max-h-[360px] overflow-y-auto">
            <div className="grid grid-cols-3 gap-2">
              {available.map((color) => {
                const isHovered = hoveredId === color.id;
                const hex = color.hexCode ?? "#e5e7eb";
                return (
                  <button
                    key={color.id}
                    type="button"
                    onClick={() => onSelect(color.id)}
                    onMouseEnter={() => setHoveredId(color.id)}
                    onMouseLeave={() => setHoveredId(null)}
                    className={cn(
                      "flex flex-col items-center gap-2.5 rounded-xl border bg-white p-3.5 text-center transition-all duration-150 active:scale-[0.96]",
                      isHovered ? "-translate-y-0.5 shadow-lg" : "shadow-sm"
                    )}
                    style={{ borderColor: isHovered ? hex : "#f3f4f6" }}
                  >
                    <span
                      className={cn(
                        "h-10 w-10 rounded-full shrink-0 transition-transform duration-150",
                        isHovered ? "scale-110" : "scale-100"
                      )}
                      style={{
                        backgroundColor: hex,
                        boxShadow: isHovered
                          ? `0 0 0 2px white, 0 0 0 4px ${hex}, 0 6px 16px ${hex}50`
                          : `0 0 0 2px white, 0 0 0 3px ${hex}40, 0 2px 8px rgba(0,0,0,0.08)`,
                      }}
                    />
                    <span
                      className={cn(
                        "text-[11px] font-medium leading-tight transition-colors line-clamp-2 w-full",
                        isHovered ? "text-gray-900" : "text-gray-500"
                      )}
                    >
                      {color.displayName ?? color.colorKey}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="px-5 py-3 border-t border-gray-50 bg-gray-50/60">
          <p className="text-[10px] text-gray-300 text-center tracking-widest uppercase">
            Click a color to add a variant
          </p>
        </div>
      </DialogContent>
    </Dialog>
  );
}
