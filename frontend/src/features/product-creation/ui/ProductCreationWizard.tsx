"use client";

import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { useWizardState } from "../model/useWizardState";
import { validateStep1 } from "../model/types";
import { WizardStepper } from "./WizardStepper";
import { WizardFooter } from "./WizardFooter";
import { Step1Classification } from "./steps/Step1Classification";
import { Step2Media } from "./steps/Step2Media";

const TOTAL_STEPS = 5;

const slideVariants = {
  enter: (direction: number) => ({
    x: direction > 0 ? 40 : -40,
    opacity: 0,
  }),
  center: { x: 0, opacity: 1 },
  exit: (direction: number) => ({
    x: direction > 0 ? -40 : 40,
    opacity: 0,
  }),
};

export function ProductCreationWizard() {
  const { state, update, hydrated } = useWizardState();
  const [currentStep, setCurrentStep] = useState(1);
  const [completedSteps, setCompletedSteps] = useState<Set<number>>(new Set());
  const [direction, setDirection] = useState(1); // 1 = forward, -1 = backward

  // Per-step "submitted" flags so each step shows its own errors
  const [step1Submitted, setStep1Submitted] = useState(false);

  function goNext() {
    if (currentStep === 1) {
      setStep1Submitted(true);
      const errs = validateStep1(state);
      if (Object.keys(errs).length > 0) return;
    }

    setCompletedSteps((prev) => new Set(prev).add(currentStep));
    setDirection(1);
    setCurrentStep((s) => Math.min(s + 1, TOTAL_STEPS));
  }

  function goPrev() {
    setDirection(-1);
    setCurrentStep((s) => Math.max(s - 1, 1));
  }

  if (!hydrated) {
    // Avoid localStorage hydration mismatch flash
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-muted/30 pb-24">
      {/* Sticky stepper header */}
      <div className="sticky top-0 z-30 bg-background/95 backdrop-blur-sm border-b">
        <div className="max-w-3xl mx-auto px-4 py-4">
          <WizardStepper
            currentStep={currentStep}
            completedSteps={completedSteps}
          />
        </div>
      </div>

      {/* Step content */}
      <div className="max-w-3xl mx-auto px-4 py-8">
        <AnimatePresence mode="wait" custom={direction}>
          <motion.div
            key={currentStep}
            custom={direction}
            variants={slideVariants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={{ duration: 0.18, ease: "easeInOut" }}
          >
            {currentStep === 1 && (
              <Step1Classification
                state={state}
                update={update}
                submitted={step1Submitted}
              />
            )}

            {currentStep === 2 && (
              <Step2Media state={state} update={update} />
            )}

            {/* Steps 3–5 will be added in subsequent iterations */}
            {currentStep > 2 && (
              <div className="flex items-center justify-center min-h-[40vh] text-muted-foreground">
                Step {currentStep} — coming soon
              </div>
            )}
          </motion.div>
        </AnimatePresence>
      </div>

      {/* Sticky footer nav */}
      <WizardFooter
        step={currentStep}
        totalSteps={TOTAL_STEPS}
        onPrev={goPrev}
        onNext={goNext}
      />
    </div>
  );
}
