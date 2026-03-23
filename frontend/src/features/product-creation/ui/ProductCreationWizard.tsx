"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { AnimatePresence, motion } from "framer-motion";
import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { useWizardState } from "../model/useWizardState";
import {
  validateStep1,
  validateStep2,
  isStep2Valid,
  validateStep4,
  WIZARD_DRAFT_KEY,
} from "../model/types";
import { fetchBlueprint } from "../api/blueprint";
import { createProduct } from "../api/createProduct";
import { getErrorMessage } from "@/shared/lib/utils";
import { WizardStepper } from "./WizardStepper";
import { WizardFooter } from "./WizardFooter";
import { Step1BaseInfo } from "./steps/Step1BaseInfo";
import { Step2Variants } from "./steps/Step2Variants";
import { Step3Review } from "./steps/Step3Review";

const TOTAL_STEPS = 3;

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
  const [direction, setDirection] = useState(1);

  const [step1Submitted, setStep1Submitted] = useState(false);
  const [step2Submitted, setStep2Submitted] = useState(false);

  // Blueprint fetched here so validateStep4 can run in goNext
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
      const baseErrors = validateStep1(state);
      const attrErrors = validateStep4(state, blueprint);
      if (Object.keys(baseErrors).length > 0 || attrErrors.size > 0) return;
    }

    if (currentStep === 2) {
      setStep2Submitted(true);
      if (!isStep2Valid(validateStep2(state))) return;
    }

    if (currentStep === 3) {
      if (!isStep2Valid(validateStep2(state))) return;
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

  function goToStep(n: number) {
    setDirection(n < currentStep ? -1 : 1);
    setCurrentStep(n);
  }

  if (!hydrated) {
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
        <div className="max-w-7xl mx-auto px-8 py-4">
          <WizardStepper
            currentStep={currentStep}
            completedSteps={completedSteps}
            onStepClick={goToStep}
          />
        </div>
      </div>

      {/* Step content */}
      <div className="max-w-7xl mx-auto px-8 py-8">
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
              <Step1BaseInfo
                state={state}
                update={update}
                submitted={step1Submitted}
                failingKeys={validateStep4(state, blueprint)}
              />
            )}

            {currentStep === 2 && (
              <Step2Variants
                state={state}
                update={update}
                submitted={step2Submitted}
                errors={validateStep2(state)}
              />
            )}

            {currentStep === 3 && <Step3Review state={state} />}
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
          currentStep === 3 && !isStep2Valid(validateStep2(state))
        }
        isSubmitting={submitMutation.isPending}
      />
    </div>
  );
}
