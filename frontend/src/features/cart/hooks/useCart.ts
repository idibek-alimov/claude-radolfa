"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth";
import { getCart, addToCart, updateCartItem, removeCartItem, clearCart, applyCoupon, removeCoupon } from "@/entities/cart";
import type { Cart } from "@/entities/cart";

export function useCartQuery() {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: ["cart"],
    queryFn: getCart,
    enabled: isAuthenticated,
    staleTime: 30 * 1000,
  });
}

export function useAddToCart() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ skuId, quantity }: { skuId: number; quantity: number }) =>
      addToCart(skuId, quantity),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cart"] });
    },
  });
}

export function useUpdateCartItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ skuId, quantity }: { skuId: number; quantity: number }) =>
      updateCartItem(skuId, quantity),
    onMutate: async ({ skuId, quantity }) => {
      await queryClient.cancelQueries({ queryKey: ["cart"] });
      const snapshot = queryClient.getQueryData<Cart>(["cart"]);
      queryClient.setQueryData<Cart>(["cart"], (old) => {
        if (!old) return old;
        const items = old.items.map((i) =>
          i.skuId === skuId
            ? { ...i, quantity, lineTotal: i.unitPrice * quantity }
            : i,
        );
        const totalAmount = items.reduce((s, i) => s + i.lineTotal, 0);
        const itemCount = items.reduce((s, i) => s + i.quantity, 0);
        return { ...old, items, totalAmount, itemCount };
      });
      return { snapshot };
    },
    onError: (_err, _vars, ctx) => {
      if (ctx?.snapshot) queryClient.setQueryData(["cart"], ctx.snapshot);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["cart"] });
    },
  });
}

export function useRemoveCartItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (skuId: number) => removeCartItem(skuId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cart"] });
    },
  });
}

export function useClearCart() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: clearCart,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cart"] });
    },
  });
}

export function useApplyCoupon() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (code: string) => applyCoupon(code),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cart"] });
    },
  });
}

export function useRemoveCoupon() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: removeCoupon,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cart"] });
    },
  });
}
