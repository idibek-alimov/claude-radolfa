"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ShoppingCart, Minus, Plus, Trash2, ImageOff, X } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/shared/ui/sheet";
import { Button } from "@/shared/ui/button";
import { useAuth } from "@/features/auth";
import {
  getCart,
  addToCart,
  updateCartItem,
  removeCartItem,
  clearCart,
} from "@/entities/cart";
import type { CartItem } from "@/entities/cart";
import { cn } from "@/shared/lib/utils";

export { addToCart, useCartMutation };

/** Re-export so other components can trigger cart mutations and invalidate. */
function useCartMutation() {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["cart"] });

  const add = useMutation({
    mutationFn: ({ skuId, quantity }: { skuId: number; quantity: number }) =>
      addToCart(skuId, quantity),
    onSuccess: invalidate,
  });

  return { add };
}

export default function CartDrawer() {
  const [open, setOpen] = useState(false);
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();

  const { data: cart, isLoading } = useQuery({
    queryKey: ["cart"],
    queryFn: getCart,
    enabled: isAuthenticated,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["cart"] });

  const updateMutation = useMutation({
    mutationFn: ({ skuId, quantity }: { skuId: number; quantity: number }) =>
      updateCartItem(skuId, quantity),
    onSuccess: invalidate,
  });

  const removeMutation = useMutation({
    mutationFn: (skuId: number) => removeCartItem(skuId),
    onSuccess: invalidate,
  });

  const clearMutation = useMutation({
    mutationFn: clearCart,
    onSuccess: invalidate,
  });

  const itemCount = cart?.itemCount ?? 0;

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <button
          aria-label="Open cart"
          className="relative p-2 rounded-md hover:bg-accent transition-colors"
        >
          <ShoppingCart className="h-5 w-5" />
          {itemCount > 0 && (
            <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] flex items-center justify-center rounded-full bg-primary text-primary-foreground text-[10px] font-bold px-1 leading-none">
              {itemCount > 99 ? "99+" : itemCount}
            </span>
          )}
        </button>
      </SheetTrigger>

      <SheetContent side="right" className="w-full sm:max-w-md flex flex-col p-0">
        <SheetHeader className="px-6 py-4 border-b">
          <SheetTitle className="flex items-center gap-2">
            <ShoppingCart className="h-5 w-5" />
            Cart
            {itemCount > 0 && (
              <span className="ml-1 text-sm font-normal text-muted-foreground">
                ({itemCount} {itemCount === 1 ? "item" : "items"})
              </span>
            )}
          </SheetTitle>
        </SheetHeader>

        {/* Body */}
        <div className="flex-1 overflow-y-auto">
          {!isAuthenticated ? (
            <div className="flex flex-col items-center justify-center h-full gap-4 px-6 text-center">
              <ShoppingCart className="h-12 w-12 text-muted-foreground/40" strokeWidth={1} />
              <p className="text-muted-foreground">Sign in to view your cart</p>
              <Button asChild onClick={() => setOpen(false)}>
                <Link href="/login">Sign In</Link>
              </Button>
            </div>
          ) : isLoading ? (
            <CartSkeleton />
          ) : !cart || cart.items.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full gap-4 px-6 text-center">
              <ShoppingCart className="h-12 w-12 text-muted-foreground/40" strokeWidth={1} />
              <p className="text-muted-foreground">Your cart is empty</p>
              <Button variant="outline" asChild onClick={() => setOpen(false)}>
                <Link href="/products">Browse Products</Link>
              </Button>
            </div>
          ) : (
            <ul className="divide-y">
              {cart.items.map((item) => (
                <CartItemRow
                  key={item.skuId}
                  item={item}
                  onUpdate={(qty) =>
                    updateMutation.mutate({ skuId: item.skuId, quantity: qty })
                  }
                  onRemove={() => removeMutation.mutate(item.skuId)}
                  isPending={
                    (updateMutation.isPending &&
                      (updateMutation.variables as { skuId: number })?.skuId === item.skuId) ||
                    (removeMutation.isPending && removeMutation.variables === item.skuId)
                  }
                />
              ))}
            </ul>
          )}
        </div>

        {/* Footer */}
        {isAuthenticated && cart && cart.items.length > 0 && (
          <div className="border-t px-6 py-4 space-y-4">
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">Subtotal</span>
              <span className="font-bold text-lg">
                ${cart.subtotal.toFixed(2)}
              </span>
            </div>

            <Button className="w-full" size="lg" asChild onClick={() => setOpen(false)}>
              <Link href="/checkout">Proceed to Checkout</Link>
            </Button>

            <button
              onClick={() => clearMutation.mutate()}
              disabled={clearMutation.isPending}
              className="w-full text-center text-xs text-muted-foreground hover:text-destructive transition-colors disabled:opacity-50"
            >
              Clear cart
            </button>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}

function CartItemRow({
  item,
  onUpdate,
  onRemove,
  isPending,
}: {
  item: CartItem;
  onUpdate: (qty: number) => void;
  onRemove: () => void;
  isPending: boolean;
}) {
  return (
    <li className={cn("flex gap-3 px-6 py-4", isPending && "opacity-60 pointer-events-none")}>
      {/* Thumbnail */}
      <div className="relative w-16 h-16 rounded-lg bg-muted overflow-hidden flex-shrink-0">
        {item.imageUrl ? (
          <Image
            src={item.imageUrl}
            alt={item.productName}
            fill
            className="object-cover"
            unoptimized
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-muted-foreground/40">
            <ImageOff className="h-5 w-5" strokeWidth={1.5} />
          </div>
        )}
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0">
        <Link
          href={`/products/${item.listingSlug}`}
          className="text-sm font-medium line-clamp-2 hover:text-primary transition-colors"
        >
          {item.productName}
        </Link>
        {item.sizeLabel && (
          <p className="text-xs text-muted-foreground mt-0.5">Size: {item.sizeLabel}</p>
        )}
        <p className="text-xs text-muted-foreground">${item.priceSnapshot.toFixed(2)} each</p>

        {/* Quantity controls */}
        <div className="flex items-center gap-1 mt-2">
          <button
            onClick={() => onUpdate(item.quantity - 1)}
            className="w-6 h-6 flex items-center justify-center rounded border hover:bg-muted transition-colors"
            aria-label="Decrease quantity"
          >
            <Minus className="h-3 w-3" />
          </button>
          <span className="w-8 text-center text-sm font-medium">{item.quantity}</span>
          <button
            onClick={() => onUpdate(item.quantity + 1)}
            className="w-6 h-6 flex items-center justify-center rounded border hover:bg-muted transition-colors"
            aria-label="Increase quantity"
          >
            <Plus className="h-3 w-3" />
          </button>
        </div>
      </div>

      {/* Right: subtotal + remove */}
      <div className="flex flex-col items-end justify-between flex-shrink-0">
        <span className="text-sm font-semibold">${item.itemSubtotal.toFixed(2)}</span>
        <button
          onClick={onRemove}
          aria-label="Remove item"
          className="text-muted-foreground hover:text-destructive transition-colors"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </li>
  );
}

function CartSkeleton() {
  return (
    <ul className="divide-y">
      {Array.from({ length: 3 }).map((_, i) => (
        <li key={i} className="flex gap-3 px-6 py-4">
          <div className="w-16 h-16 rounded-lg bg-muted animate-pulse flex-shrink-0" />
          <div className="flex-1 space-y-2">
            <div className="h-4 bg-muted rounded animate-pulse w-3/4" />
            <div className="h-3 bg-muted rounded animate-pulse w-1/2" />
            <div className="h-6 bg-muted rounded animate-pulse w-20 mt-2" />
          </div>
        </li>
      ))}
    </ul>
  );
}
