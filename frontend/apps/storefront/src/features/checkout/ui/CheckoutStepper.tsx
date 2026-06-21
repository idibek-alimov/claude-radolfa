"use client";

import { Fragment } from "react";
import { useTranslations } from "next-intl";
import { cn } from "@radolfa/shared/lib/utils";
import { CHECKOUT_STEPS, type CheckoutStep } from "../model/types";

interface CheckoutStepperProps {
  step: CheckoutStep;
  onStepClick?: (step: CheckoutStep) => void;
}

export function CheckoutStepper({ step, onStepClick }: CheckoutStepperProps) {
  const t = useTranslations("checkout.steps");
  const currentIdx = CHECKOUT_STEPS.indexOf(step);

  return (
    <div className="bg-white border-b border-ink/8">
      {/* Desktop */}
      <div className="hidden md:block max-w-[1240px] mx-auto px-6 py-4">
        <div className="flex items-center gap-3 max-w-2xl">
          {CHECKOUT_STEPS.map((s, idx) => {
            const done = idx < currentIdx;
            const active = idx === currentIdx;
            const clickable = done || active;

            return (
              <Fragment key={s}>
                <div
                  className={cn("flex items-center gap-2", clickable && "cursor-pointer")}
                  onClick={clickable ? () => onStepClick?.(s) : undefined}
                >
                  <div
                    className={cn(
                      "w-7 h-7 rounded-full flex items-center justify-center text-[12px] font-bold shrink-0",
                      done && "bg-emerald text-white",
                      active && "bg-mag text-white",
                      !done && !active && "bg-ink/8 text-ink/40"
                    )}
                  >
                    {done ? (
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                        <path d="M20 6 9 17l-5-5" />
                      </svg>
                    ) : (
                      idx + 1
                    )}
                  </div>
                  <span
                    className={cn(
                      "text-[13px] font-semibold whitespace-nowrap",
                      active && "text-ink",
                      done && "text-ink/70",
                      !done && !active && "text-ink/40"
                    )}
                  >
                    {t(s)}
                  </span>
                </div>
                {idx < CHECKOUT_STEPS.length - 1 && (
                  <div className={cn("flex-1 h-0.5 rounded", done ? "bg-emerald" : "bg-ink/10")} />
                )}
              </Fragment>
            );
          })}
        </div>
      </div>

      {/* Mobile */}
      <div className="flex md:hidden px-4 pb-3 items-center gap-1.5">
        {CHECKOUT_STEPS.map((s, idx) => {
          const done = idx < currentIdx;
          const active = idx === currentIdx;
          return (
            <div
              key={s}
              className={cn(
                "h-1.5 flex-1 rounded-full",
                done && "bg-emerald",
                active && "bg-mag",
                !done && !active && "bg-ink/10"
              )}
            />
          );
        })}
      </div>
    </div>
  );
}
