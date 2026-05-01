"use client";

import * as React from "react";
import { cn } from "@/shared/lib/utils";

interface SliderProps {
  min?: number;
  max?: number;
  step?: number;
  value?: number[];
  onValueChange?: (value: number[]) => void;
  className?: string;
  disabled?: boolean;
}

const Slider = React.forwardRef<HTMLInputElement, SliderProps>(
  ({ min = 0, max = 100, step = 1, value, onValueChange, className, disabled }, ref) => {
    const current = value?.[0] ?? min;
    const pct = max > min ? ((current - min) / (max - min)) * 100 : 0;

    function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
      onValueChange?.([Number(e.target.value)]);
    }

    return (
      <div className={cn("relative flex w-full touch-none select-none items-center", className)}>
        <div className="relative h-2 w-full grow overflow-hidden rounded-full bg-muted">
          <div
            className="absolute h-full bg-primary rounded-full"
            style={{ width: `${pct}%` }}
          />
        </div>
        <input
          ref={ref}
          type="range"
          min={min}
          max={max}
          step={step}
          value={current}
          onChange={handleChange}
          disabled={disabled}
          className="absolute w-full opacity-0 h-2 cursor-pointer disabled:cursor-not-allowed"
        />
        <div
          className="absolute h-4 w-4 rounded-full border-2 border-primary bg-background shadow transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          style={{ left: `calc(${pct}% - 8px)` }}
        />
      </div>
    );
  }
);
Slider.displayName = "Slider";

export { Slider };
