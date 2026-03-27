"use client";

import { ArrowLeft, ArrowRight, Loader2 } from "lucide-react";
import { Button } from "@/shared/ui/button";

interface Props {
  step: number;
  totalSteps: number;
  onPrev: () => void;
  onNext: () => void;
  isNextDisabled?: boolean;
  isSubmitting?: boolean;
}

export function WizardFooter({
  step,
  totalSteps,
  onPrev,
  onNext,
  isNextDisabled = false,
  isSubmitting = false,
}: Props) {
  const isLastStep = step === totalSteps;

  return (
    <div className="fixed bottom-0 inset-x-0 z-40 border-t bg-white shadow-[0_-1px_8px_rgba(0,0,0,0.06)]">
      <div className="flex items-center justify-between pl-[220px] pr-8 py-3">
        <Button
          variant="ghost"
          onClick={onPrev}
          disabled={step <= 1 || isSubmitting}
          className="gap-2 text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>

        <span className="text-xs text-muted-foreground">
          Step <span className="font-semibold text-foreground">{step}</span> of {totalSteps}
        </span>

        <Button
          onClick={onNext}
          disabled={isNextDisabled || isSubmitting}
          className="gap-2 min-w-[120px]"
        >
          {isSubmitting ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              Creating…
            </>
          ) : isLastStep ? (
            "Create Product"
          ) : (
            <>
              Continue
              <ArrowRight className="h-4 w-4" />
            </>
          )}
        </Button>
      </div>
    </div>
  );
}
