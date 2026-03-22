"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { AnimatePresence, motion } from "framer-motion";
import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { useWizardState } from "../model/useWizardState";
import {
  validateStep1,
  validateStep3,
  isStep3Valid,
  validateStep4,
  WIZARD_DRAFT_KEY,
} from "../model/types";
import { fetchBlueprint } from "../api/blueprint";
import { createProduct } from "../api/createProduct";
import { getErrorMessage } from "@/shared/lib/utils";
import { WizardStepper } from "./WizardStepper";
import { WizardFooter } from "./WizardFooter";
import { Step1Classification } from "./steps/Step1Classification";
import { Step2Media } from "./steps/Step2Media";
import { Step3VariantMatrix } from "./steps/Step3VariantMatrix";
import { Step4Attributes } from "./steps/Step4Attributes";
import { Step5Review } from "./steps/Step5Review";

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
  const router = useRouter();
  const { state, update, hydrated } = useWizardState();
  const [currentStep, setCurrentStep] = useState(1);
  const [completedSteps, setCompletedSteps] = useState<Set<number>>(new Set());
  const [direction, setDirection] = useState(1); // 1 = forward, -1 = backward

  // Per-step "submitted" flags so each step shows its own errors
  const [step1Submitted, setStep1Submitted] = useState(false);
  const [step3Submitted, setStep3Submitted] = useState(false);
  const [step4Submitted, setStep4Submitted] = useState(false);

  // Blueprint pre-fetched here so validateStep4 can run in goNext
  const { data: blueprint = [] } = useQuery({
    queryKey: ["blueprint", state.categoryId],
    queryFn: () => fetchBlueprint(state.categoryId!),
    enabled: state.categoryId !== null,
  });

  const submitMutation = useMutation({
    mutationFn: () => createProduct(state),
    onSuccess: () => {
      localStorage.removeItem(WIZARD_DRAFT_KEY);
      toast.success("Product created successfully!");
      router.push("/manage");
    },
    onError: (err) => {
      toast.error(getErrorMessage(err, "Failed to create product"));
    },
  });

  function goNext() {
    if (currentStep === 1) {
      setStep1Submitted(true);
      const errs = validateStep1(state);
      if (Object.keys(errs).length > 0) return;
    }

    if (currentStep === 3) {
      setStep3Submitted(true);
      if (!isStep3Valid(validateStep3(state))) return;
    }

    if (currentStep === 4) {
      setStep4Submitted(true);
      if (validateStep4(state, blueprint).size > 0) return;
    }

    if (currentStep === 5) {
      // Final step — submit
      if (!isStep3Valid(validateStep3(state))) return; // guard against back-nav edits
      submitMutation.mutate();
      return;
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

            {currentStep === 3 && (
              <Step3VariantMatrix
                state={state}
                update={update}
                submitted={step3Submitted}
                errors={validateStep3(state)}
              />
            )}

            {currentStep === 4 && (
              <Step4Attributes
                state={state}
                update={update}
                submitted={step4Submitted}
                failingKeys={validateStep4(state, blueprint)}
              />
            )}

            {currentStep === 5 && <Step5Review state={state} />}
          </motion.div>
        </AnimatePresence>
      </div>

      {/* Sticky footer nav */}
      <WizardFooter
        step={currentStep}
        totalSteps={TOTAL_STEPS}
        onPrev={goPrev}
        onNext={goNext}
        isNextDisabled={
          currentStep === 5 && !isStep3Valid(validateStep3(state))
        }
        isSubmitting={submitMutation.isPending}
      />
    </div>
  );
}
