"use client";

import { createContext, useContext, type ReactNode } from "react";
import type { ProductCardDraftApi } from "./useProductCardDraft";

const Ctx = createContext<ProductCardDraftApi | null>(null);

export function ProductCardDraftProvider({
  value,
  children,
}: {
  value: ProductCardDraftApi;
  children: ReactNode;
}) {
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useDraft(): ProductCardDraftApi {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useDraft must be used inside ProductCardDraftProvider");
  return ctx;
}
