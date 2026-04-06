"use client";

import { useState, useCallback, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AnimatePresence, motion } from "framer-motion";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronRight, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { fetchDiscountById, createDiscount, updateDiscount } from "../api";
import { getErrorMessage } from "@/shared/lib/utils";
import { DiscountWizardStepper } from "./DiscountWizardStepper";
import { WizardFooter } from "@/features/product-creation/ui/WizardFooter";
import { Step1Details } from "./wizard-steps/Step1Details";
import { Step2Schedule } from "./wizard-steps/Step2Schedule";
import { Step3Products } from "./wizard-steps/Step3Products";
import { Step4Review } from "./wizard-steps/Step4Review";

export interface DiscountWizardState {
  title: string;
  typeId: number;
  discountValue: number;
  colorHex: string;
  validFrom: string;
  validUpto: string;
  selectedCodes: string[];
}

const DEFAULT_STATE: DiscountWizardState = {
  title: "",
  typeId: 0,
  discountValue: 10,
  colorHex: "E74C3C",
  validFrom: "",
  validUpto: "",
  selectedCodes: [],
};

const TOTAL_STEPS = 4;

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

// ── Helpers ────────────────────────────────────────────────────────

function toLocalInput(iso: string): string {
  if (!iso) return "";
  const date = new Date(iso);
  if (isNaN(date.getTime())) return "";
  const tzOffset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - tzOffset).toISOString().slice(0, 16);
}

function toIso(local: string): string {
  if (!local) return "";
  const date = new Date(local);
  if (isNaN(date.getTime())) return "";
  return date.toISOString();
}

function isStep1Valid(s: DiscountWizardState): boolean {
  return (
    s.title.trim().length > 0 &&
    s.typeId > 0 &&
    s.discountValue >= 1 &&
    s.discountValue <= 99 &&
    s.colorHex.length === 6
  );
}

function isStep2Valid(s: DiscountWizardState): boolean {
  if (!s.validFrom || !s.validUpto) return false;
  return new Date(s.validUpto) > new Date(s.validFrom);
}

function isStep3Valid(s: DiscountWizardState): boolean {
  return s.selectedCodes.length > 0;
}

// ── Props ──────────────────────────────────────────────────────────

interface Props {
  /** ID of existing discount to edit */
  editId?: number;
  /** ID of discount to duplicate (pre-fill with "Copy of…" title) */
  fromId?: number;
}

export function DiscountCreationWizard({ editId, fromId }: Props) {
  const router = useRouter();
  const qc = useQueryClient();

  const isEdit = editId !== undefined;
  const isDuplicate = fromId !== undefined;
  const sourceId = editId ?? fromId;

  const [state, setState] = useState<DiscountWizardState>(DEFAULT_STATE);
  const [initialized, setInitialized] = useState(!sourceId);
  const [currentStep, setCurrentStep] = useState(1);
  const [completedSteps, setCompletedSteps] = useState<Set<number>>(new Set());
  const [direction, setDirection] = useState(1);
  const [step1Submitted, setStep1Submitted] = useState(false);
  const [step2Submitted, setStep2Submitted] = useState(false);
  const [step3Submitted, setStep3Submitted] = useState(false);

  const update = useCallback((patch: Partial<DiscountWizardState>) => {
    setState((prev) => ({ ...prev, ...patch }));
  }, []);

  // ── Load existing discount ─────────────────────────────────────

  const { data: sourceDiscount, isLoading: isLoadingSource } = useQuery({
    queryKey: ["discount", sourceId],
    queryFn: () => fetchDiscountById(sourceId!),
    enabled: sourceId !== undefined,
    retry: 1,
    staleTime: 0,
  });

  useEffect(() => {
    if (!sourceDiscount || initialized) return;
    setState({
      title: isDuplicate ? `Copy of ${sourceDiscount.title}` : sourceDiscount.title,
      typeId: sourceDiscount.type.id,
      discountValue: sourceDiscount.discountValue,
      colorHex: sourceDiscount.colorHex.replace(/^#/, ""),
      validFrom: isDuplicate ? "" : toLocalInput(sourceDiscount.validFrom),
      validUpto: isDuplicate ? "" : toLocalInput(sourceDiscount.validUpto),
      selectedCodes: sourceDiscount.itemCodes,
    });
    setInitialized(true);
  }, [sourceDiscount, initialized, isDuplicate]);

  // ── Submit mutation ────────────────────────────────────────────

  const submitMutation = useMutation({
    mutationFn: () => {
      const payload = {
        typeId: state.typeId,
        itemCodes: state.selectedCodes,
        discountValue: state.discountValue,
        validFrom: toIso(state.validFrom),
        validUpto: toIso(state.validUpto),
        title: state.title.trim(),
        colorHex: state.colorHex,
      };
      return isEdit
        ? updateDiscount(editId!, payload)
        : createDiscount(payload);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["discounts"] });
      toast.success(isEdit ? "Discount updated" : "Discount created");
      router.push("/manage");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // ── Navigation ────────────────────────────────────────────────

  function goNext() {
    if (currentStep === 1) {
      setStep1Submitted(true);
      if (!isStep1Valid(state)) return;
    }
    if (currentStep === 2) {
      setStep2Submitted(true);
      if (!isStep2Valid(state)) return;
    }
    if (currentStep === 3) {
      setStep3Submitted(true);
      if (!isStep3Valid(state)) return;
    }
    if (currentStep === TOTAL_STEPS) {
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

  // ── Loading state ─────────────────────────────────────────────

  if (sourceId !== undefined && (isLoadingSource || !initialized)) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50/80">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  // ── Breadcrumb title ──────────────────────────────────────────

  const breadcrumbTitle = isEdit
    ? state.title
      ? `Edit: ${state.title}`
      : "Edit Discount"
    : isDuplicate
    ? "Duplicate Campaign"
    : "New Campaign";

  return (
    <div className="min-h-screen bg-gray-50/80 flex flex-col">
      {/* Sticky breadcrumb header */}
      <header className="sticky top-0 z-40 h-14 bg-white border-b flex items-center px-6 gap-2 shrink-0">
        <Link
          href="/manage"
          className="text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          Discounts
        </Link>
        <ChevronRight className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
        <span className="text-sm font-medium text-foreground truncate max-w-[320px]">
          {breadcrumbTitle}
        </span>
      </header>

      {/* Body: sidebar + main */}
      <div className="flex flex-1">
        {/* Left sidebar */}
        <aside className="sticky top-14 h-[calc(100vh-3.5rem)] w-[260px] shrink-0 bg-white border-r overflow-y-auto">
          <DiscountWizardStepper
            currentStep={currentStep}
            completedSteps={completedSteps}
            onStepClick={goToStep}
          />
        </aside>

        {/* Main content */}
        <main className="flex-1 min-w-0 px-10 py-8 pb-28 flex flex-col">
          <AnimatePresence mode="wait" custom={direction}>
            <motion.div
              key={currentStep}
              custom={direction}
              variants={slideVariants}
              initial="enter"
              animate="center"
              exit="exit"
              transition={{ duration: 0.18, ease: "easeInOut" }}
              className="flex-1 flex flex-col"
            >
              {currentStep === 1 && (
                <Step1Details
                  state={state}
                  update={update}
                  submitted={step1Submitted}
                />
              )}
              {currentStep === 2 && (
                <Step2Schedule
                  state={state}
                  update={update}
                  submitted={step2Submitted}
                />
              )}
              {currentStep === 3 && (
                <Step3Products
                  state={state}
                  update={update}
                  submitted={step3Submitted}
                />
              )}
              {currentStep === 4 && (
                <Step4Review state={state} isEdit={isEdit} />
              )}
            </motion.div>
          </AnimatePresence>
        </main>
      </div>

      {/* Sticky footer */}
      <WizardFooter
        step={currentStep}
        totalSteps={TOTAL_STEPS}
        onPrev={goPrev}
        onNext={goNext}
        isNextDisabled={currentStep === TOTAL_STEPS && submitMutation.isPending}
        isSubmitting={submitMutation.isPending}
        submitLabel={isEdit ? "Save Changes" : "Create Discount"}
        sidebarOffset={260}
      />
    </div>
  );
}
