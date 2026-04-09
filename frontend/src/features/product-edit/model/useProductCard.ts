"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchProductCard } from "@/entities/product/api/admin";

export function useProductCard(productBaseId: number) {
  return useQuery({
    queryKey: ["admin-product", productBaseId],
    queryFn: () => fetchProductCard(productBaseId),
  });
}
