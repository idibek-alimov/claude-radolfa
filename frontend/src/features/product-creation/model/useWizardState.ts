"use client";

import { useState, useEffect, useCallback } from "react";
import {
  WizardState,
  INITIAL_WIZARD_STATE,
  WIZARD_DRAFT_KEY,
} from "./types";

export function clearDraft() {
  try {
    localStorage.removeItem(WIZARD_DRAFT_KEY);
  } catch {
    // ignore
  }
}

export function useWizardState() {
  const [state, setState] = useState<WizardState>(INITIAL_WIZARD_STATE);
  const [hydrated, setHydrated] = useState(false);

  // Always start fresh — clear any previous draft on mount
  useEffect(() => {
    clearDraft();
    setHydrated(true);
  }, []);

  const update = useCallback((patch: Partial<WizardState>) => {
    setState((prev) => ({ ...prev, ...patch }));
  }, []);

  return { state, update, hydrated };
}
