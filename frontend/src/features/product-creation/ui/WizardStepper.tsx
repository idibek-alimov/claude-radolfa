"use client";

import { Check } from "lucide-react";
import { cn } from "@/shared/lib/utils";

const STEPS = [
  { label: "Classification" },
  { label: "Media" },
  { label: "Variants" },
  { label: "Attributes" },
  { label: "Review" },
];

interface Props {
  currentStep: number; // 1-based
  completedSteps: Set<number>;
  onStepClick?: (step: number) => void;
}

export function WizardStepper({ currentStep, completedSteps, onStepClick }: Props) {
  return (
    <nav aria-label="Product creation steps">
      <ol className="flex items-center gap-0">
        {STEPS.map((step, idx) => {
          const stepNum = idx + 1;
          const isComplete = completedSteps.has(stepNum);
          const isCurrent = stepNum === currentStep;
          const isPending = !isComplete && !isCurrent;

          const circle = (
            <div className="flex flex-col items-center gap-1.5">
              <div
                className={cn(
                  "flex h-8 w-8 items-center justify-center rounded-full border-2 text-xs font-semibold transition-colors",
                  isComplete &&
                    "border-primary bg-primary text-primary-foreground",
                  isCurrent && "border-primary bg-background text-primary",
                  isPending &&
                    "border-muted-foreground/30 bg-background text-muted-foreground"
                )}
              >
                {isComplete ? <Check className="h-3.5 w-3.5" /> : stepNum}
              </div>
              <span
                className={cn(
                  "text-xs font-medium whitespace-nowrap",
                  isCurrent && "text-primary",
                  isPending && "text-muted-foreground",
                  isComplete && "text-primary"
                )}
              >
                {step.label}
              </span>
            </div>
          );

          return (
            <li key={stepNum} className="flex items-center flex-1 last:flex-none">
              {isComplete ? (
                <button
                  type="button"
                  onClick={() => onStepClick?.(stepNum)}
                  className="hover:opacity-75 transition-opacity"
                  aria-label={`Go back to step ${stepNum}: ${step.label}`}
                >
                  {circle}
                </button>
              ) : (
                circle
              )}

              {/* Connector line */}
              {stepNum < STEPS.length && (
                <div
                  className={cn(
                    "flex-1 h-[2px] mx-2 mt-[-16px] transition-colors",
                    isComplete ? "bg-primary" : "bg-muted-foreground/20"
                  )}
                />
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
