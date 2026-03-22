"use client";

import { useState, useEffect, useCallback } from "react";
import {
  WizardState,
  INITIAL_WIZARD_STATE,
  WIZARD_DRAFT_KEY,
} from "./types";

function loadDraft(): WizardState {
  try {
    const raw = localStorage.getItem(WIZARD_DRAFT_KEY);
    if (!raw) return INITIAL_WIZARD_STATE;
    return { ...INITIAL_WIZARD_STATE, ...JSON.parse(raw) };
  } catch {
    return INITIAL_WIZARD_STATE;
  }
}

function saveDraft(state: WizardState) {
  try {
    localStorage.setItem(WIZARD_DRAFT_KEY, JSON.stringify(state));
  } catch {
    // localStorage unavailable — silently ignore
  }
}

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

  // Restore from localStorage on mount (client only)
  useEffect(() => {
    setState(loadDraft());
    setHydrated(true);
  }, []);

  // Persist to localStorage on every change (after hydration)
  useEffect(() => {
    if (hydrated) saveDraft(state);
  }, [state, hydrated]);

  const update = useCallback((patch: Partial<WizardState>) => {
    setState((prev) => ({ ...prev, ...patch }));
  }, []);

  return { state, update, hydrated };
}
