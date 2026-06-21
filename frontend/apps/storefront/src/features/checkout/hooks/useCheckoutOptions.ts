"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchCheckoutOptions } from "../api";

export function useCheckoutOptions() {
  return useQuery({
    queryKey: ["checkout-options"],
    queryFn: fetchCheckoutOptions,
    staleTime: 5 * 60 * 1000,
  });
}
