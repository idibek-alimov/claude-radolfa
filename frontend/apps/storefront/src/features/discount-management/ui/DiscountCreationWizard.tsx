"use client";

import { useState, useCallback, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AnimatePresence, motion } from "framer-motion";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronRight, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { fetchDiscountById, createDiscount, updateDiscount } from "../api";
import type { AmountType } from "../model/types";
import { getErrorMessage } from "@/shared/lib/utils";
import { DiscountWizardStepper } from "./DiscountWizardStepper";
import { WizardFooter } from "@/features/product-creation/ui/WizardFooter";
import { Step1Details } from "./wizard-steps/Step1Details";
import { Step2Schedule } from "./wizard-steps/Step2Schedule";
import { Step2bLimitsCoupon } from "./wizard-steps/Step2bLimitsCoupon";
import { Step3Targets } from "./wizard-steps/Step3Targets";
import { Step4Review } from "./wizard-steps/Step4Review";
import { isCouponsEnabled } from "@/shared/lib";

export type SegmentSelection =
  | { type: "LOYALTY_TIER"; tierId: number }
  | { type: "NEW_CUSTOMER" };

export interface DiscountWizardState {
  title: string;
  typeId: number;
  amountType: AmountType;
  discountValue: number;
  colorHex: string;
  validFrom: string;
  validUpto: string;
  // target mode — exclusive
  targetMode: "SKU" | "CATEGORY" | "SEGMENT";
  selectedCodes: string[];
  selectedCategoryIds: number[];
  includeDescendants: boolean;
  selectedSegment: SegmentSelection | null;
  // limits
  minBasketAmount: string;
  usageCapTotal: string;
  usageCapPerCustomer: string;
  couponCode: string;
}

const DEFAULT_STATE: DiscountWizardState = {
  title: "",
  typeId: 0,
  amountType: "PERCENT",
  discountValue: 10,
  colorHex: "E74C3C",
  validFrom: "",
  validUpto: "",
  targetMode: "SKU",
  selectedCodes: [],
  selectedCategoryIds: [],
  includeDescendants: true,
  selectedSegment: null,
  minBasketAmount: "",
  usageCapTotal: "",
  usageCapPerCustomer: "",
  couponCode: "",
};

const TOTAL_STEPS = isCouponsEnabled ? 5 : 4;

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
  const valueOk =
    s.amountType === "PERCENT"
      ? s.discountValue >= 1 && s.discountValue <= 99
      : s.discountValue >= 1 && s.discountValue <= 999999;
  return s.title.trim().length > 0 && s.typeId > 0 && valueOk && s.colorHex.length === 6;
}

function isStep2Valid(s: DiscountWizardState): boolean {
  if (!s.validFrom || !s.validUpto) return false;
  return new Date(s.validUpto) > new Date(s.validFrom);
}

function isStep3Valid(s: DiscountWizardState): boolean {
  if (s.targetMode === "SKU") return s.selectedCodes.length > 0;
  if (s.targetMode === "CATEGORY") return s.selectedCategoryIds.length > 0;
  // SEGMENT
  if (!s.selectedSegment) return false;
  if (s.selectedSegment.type === "LOYALTY_TIER") return s.selectedSegment.tierId > 0;
  return true;
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
  const [step2bSubmitted, setStep2bSubmitted] = useState(false);
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

    const targets = sourceDiscount.targets;
    const hasCategory = targets.some((t) => t.targetType === "CATEGORY");
    const hasSegment = targets.some((t) => t.targetType === "SEGMENT");
    const targetMode: DiscountWizardState["targetMode"] = hasCategory
      ? "CATEGORY"
      : hasSegment
      ? "SEGMENT"
      : "SKU";

    const firstCat = targets.find((t) => t.targetType === "CATEGORY");
    const firstSeg = targets.find((t) => t.targetType === "SEGMENT");
    const selectedSegment: SegmentSelection | null = firstSeg
      ? firstSeg.referenceId === "NEW"
        ? { type: "NEW_CUSTOMER" }
        : { type: "LOYALTY_TIER", tierId: Number(firstSeg.referenceId) }
      : null;

    setState({
      title: isDuplicate ? `Copy of ${sourceDiscount.title}` : sourceDiscount.title,
      typeId: sourceDiscount.type.id,
      amountType: sourceDiscount.amountType,
      discountValue: sourceDiscount.amountValue,
      colorHex: sourceDiscount.colorHex.replace(/^#/, ""),
      validFrom: isDuplicate ? "" : toLocalInput(sourceDiscount.validFrom),
      validUpto: isDuplicate ? "" : toLocalInput(sourceDiscount.validUpto),
      targetMode,
      selectedCodes: targets.filter((t) => t.targetType === "SKU").map((t) => t.referenceId),
      selectedCategoryIds: targets
        .filter((t) => t.targetType === "CATEGORY")
        .map((t) => Number(t.referenceId)),
      includeDescendants: firstCat?.includeDescendants ?? true,
      selectedSegment,
      minBasketAmount: sourceDiscount.minBasketAmount != null ? String(sourceDiscount.minBasketAmount) : "",
      usageCapTotal: sourceDiscount.usageCapTotal != null ? String(sourceDiscount.usageCapTotal) : "",
      usageCapPerCustomer: sourceDiscount.usageCapPerCustomer != null ? String(sourceDiscount.usageCapPerCustomer) : "",
      couponCode: isDuplicate ? "" : (sourceDiscount.couponCode ?? ""),
    });
    setInitialized(true);
  }, [sourceDiscount, initialized, isDuplicate]);

  // ── Submit mutation ────────────────────────────────────────────

  const parseOptionalNumber = (s: string) => (s.trim() ? Number(s) : undefined);
  const parseOptionalInt = (s: string) => (s.trim() ? parseInt(s, 10) : undefined);

  const submitMutation = useMutation({
    mutationFn: () => {
      const targets =
        state.targetMode === "SKU"
          ? state.selectedCodes.map((code) => ({ targetType: "SKU" as const, referenceId: code }))
          : state.targetMode === "CATEGORY"
          ? state.selectedCategoryIds.map((id) => ({
              targetType: "CATEGORY" as const,
              referenceId: String(id),
              includeDescendants: state.includeDescendants,
            }))
          : state.selectedSegment
          ? [
              {
                targetType: "SEGMENT" as const,
                referenceId:
                  state.selectedSegment.type === "LOYALTY_TIER"
                    ? String(state.selectedSegment.tierId)
                    : "NEW",
              },
            ]
          : [];

      const payload = {
        typeId: state.typeId,
        targets,
        amountType: state.amountType,
        amountValue: state.discountValue,
        validFrom: toIso(state.validFrom),
        validUpto: toIso(state.validUpto),
        title: state.title.trim(),
        colorHex: state.colorHex,
        minBasketAmount: parseOptionalNumber(state.minBasketAmount),
        usageCapTotal: parseOptionalInt(state.usageCapTotal),
        usageCapPerCustomer: parseOptionalInt(state.usageCapPerCustomer),
        couponCode: state.couponCode?.trim().toUpperCase() || undefined,
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

  const productsStep = isCouponsEnabled ? 4 : 3;

  function goNext() {
    if (currentStep === 1) {
      setStep1Submitted(true);
      if (!isStep1Valid(state)) return;
    }
    if (currentStep === 2) {
      setStep2Submitted(true);
      if (!isStep2Valid(state)) return;
    }
    if (isCouponsEnabled && currentStep === 3) {
      setStep2bSubmitted(true);
      // Coupon field is optional; only block if non-empty but invalid format
      const code = state.couponCode?.trim();
      if (code && !/^[A-Z0-9]{3,32}$/.test(code)) return;
    }
    if (currentStep === productsStep) {
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
              {isCouponsEnabled && currentStep === 3 && (
                <Step2bLimitsCoupon
                  state={state}
                  update={update}
                  sourceId={sourceId}
                  submitted={step2bSubmitted}
                />
              )}
              {currentStep === productsStep && (
                <Step3Targets
                  state={state}
                  update={update}
                  submitted={step3Submitted}
                  editId={editId}
                />
              )}
              {currentStep === TOTAL_STEPS && (
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
