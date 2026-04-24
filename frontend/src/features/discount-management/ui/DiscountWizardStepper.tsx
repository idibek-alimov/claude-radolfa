"use client";

import { Check } from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { isCouponsEnabled } from "@/shared/lib";

const BASE_STEPS = [
  { label: "Campaign Details", description: "Title, type, discount & color" },
  { label: "Schedule",          description: "Start & end dates" },
  { label: "Product Selection", description: "Choose SKUs" },
  { label: "Review & Submit",   description: "Confirm and save" },
];

const COUPON_STEP = { label: "Coupon Code", description: "Optional promo code" };

const STEPS = isCouponsEnabled
  ? [BASE_STEPS[0], BASE_STEPS[1], COUPON_STEP, BASE_STEPS[2], BASE_STEPS[3]]
  : BASE_STEPS;

interface Props {
  currentStep: number; // 1-based
  completedSteps: Set<number>;
  onStepClick?: (step: number) => void;
}

export function DiscountWizardStepper({ currentStep, completedSteps, onStepClick }: Props) {
  return (
    <nav className="p-6" aria-label="Discount creation steps">
      <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground px-1 mb-5">
        Steps
      </p>

      <ol className="space-y-1">
        {STEPS.map((step, idx) => {
          const stepNum = idx + 1;
          const isComplete = completedSteps.has(stepNum);
          const isCurrent = stepNum === currentStep;
          const isPending = !isComplete && !isCurrent;

          return (
            <li key={stepNum}>
              {isComplete ? (
                <button
                  type="button"
                  onClick={() => onStepClick?.(stepNum)}
                  className="group w-full flex items-center gap-3 px-2 py-2.5 rounded-lg text-left transition-colors hover:bg-muted/60"
                >
                  <StepCircle stepNum={stepNum} isComplete={isComplete} isCurrent={isCurrent} />
                  <StepLabel step={step} isComplete={isComplete} isCurrent={isCurrent} isPending={isPending} />
                </button>
              ) : (
                <div
                  className={cn(
                    "flex items-center gap-3 px-2 py-2.5 rounded-lg",
                    isCurrent && "bg-primary/10"
                  )}
                >
                  <StepCircle stepNum={stepNum} isComplete={isComplete} isCurrent={isCurrent} />
                  <StepLabel step={step} isComplete={isComplete} isCurrent={isCurrent} isPending={isPending} />
                </div>
              )}

              {stepNum < STEPS.length && (
                <div className="flex justify-start pl-[18px] py-0.5">
                  <div
                    className={cn(
                      "w-px h-4 transition-colors",
                      isComplete ? "bg-primary/40" : "bg-muted-foreground/20"
                    )}
                  />
                </div>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

function StepCircle({
  stepNum,
  isComplete,
  isCurrent,
}: {
  stepNum: number;
  isComplete: boolean;
  isCurrent: boolean;
}) {
  const isPending = !isComplete && !isCurrent;
  return (
    <div
      className={cn(
        "flex h-7 w-7 shrink-0 items-center justify-center rounded-full border-2 text-[11px] font-bold transition-all",
        isComplete && "border-primary bg-primary text-primary-foreground",
        isCurrent && "border-primary bg-background text-primary shadow-[0_0_0_3px_hsl(var(--primary)/0.12)]",
        isPending && "border-muted-foreground/25 bg-background text-muted-foreground"
      )}
    >
      {isComplete ? <Check className="h-3.5 w-3.5" /> : stepNum}
    </div>
  );
}

function StepLabel({
  step,
  isComplete,
  isCurrent,
  isPending,
}: {
  step: { label: string; description: string };
  isComplete: boolean;
  isCurrent: boolean;
  isPending: boolean;
}) {
  return (
    <div className="min-w-0 flex-1">
      <p
        className={cn(
          "text-sm font-medium leading-tight truncate",
          isCurrent && "text-primary",
          isComplete && "text-foreground",
          isPending && "text-muted-foreground"
        )}
      >
        {step.label}
      </p>
      <p className="text-[11px] text-muted-foreground truncate mt-0.5 leading-tight">
        {step.description}
      </p>
    </div>
  );
}
