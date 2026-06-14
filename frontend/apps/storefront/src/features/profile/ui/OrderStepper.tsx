"use client";

import { useTranslations } from "next-intl";
import { cn } from "@radolfa/shared/lib/utils";
import type { OrderStep } from "@/features/profile/lib/orderSteps";

/**
 * 5-step order-progress stepper — Tailwind reproduction of the design's
 * `.ostep` custom CSS (dot + connector + label). `done` steps and the
 * connector behind them render emerald; the `current` step renders magenta
 * with a soft ring; future steps stay neutral ink/20.
 */
export function OrderStepper({ steps }: { steps: OrderStep[] }) {
  const t = useTranslations("profile");

  return (
    <div className="flex">
      {steps.map((step, i) => (
        <div key={step.labelKey} className="flex-1 relative text-center">
          {i > 0 && (
            <div
              className={cn(
                "absolute top-2 left-[calc(-50%+9px)] h-0.5 w-[calc(100%-18px)]",
                step.done || step.current ? "bg-emerald" : "bg-ink/15"
              )}
            />
          )}
          <div
            className={cn(
              "relative z-10 mx-auto h-3.5 w-3.5 lg:h-[18px] lg:w-[18px] rounded-full border-2 bg-white",
              step.current
                ? "border-mag bg-mag ring-4 ring-mag/15"
                : step.done
                  ? "border-emerald bg-emerald"
                  : "border-ink/20"
            )}
          />
          <div
            className={cn(
              "mt-1 text-[9px] lg:text-[10.5px] font-bold leading-tight",
              step.done || step.current ? "text-ink" : "text-ink/42"
            )}
          >
            {t(step.labelKey)}
          </div>
        </div>
      ))}
    </div>
  );
}
