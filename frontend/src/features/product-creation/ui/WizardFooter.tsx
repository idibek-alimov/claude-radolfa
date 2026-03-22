"use client";

import { Loader2 } from "lucide-react";
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
    <div className="fixed bottom-0 left-0 right-0 z-40 border-t bg-background/95 backdrop-blur-sm">
      <div className="max-w-3xl mx-auto px-4 py-3 flex items-center justify-between">
        <Button
          variant="outline"
          onClick={onPrev}
          disabled={step <= 1 || isSubmitting}
        >
          Previous
        </Button>

        <span className="text-sm text-muted-foreground">
          Step {step} of {totalSteps}
        </span>

        <Button onClick={onNext} disabled={isNextDisabled || isSubmitting}>
          {isSubmitting ? (
            <>
              <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
              Creating…
            </>
          ) : isLastStep ? (
            "Create Product"
          ) : (
            "Next"
          )}
        </Button>
      </div>
    </div>
  );
}
