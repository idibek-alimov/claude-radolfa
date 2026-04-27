"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { useTranslations } from "next-intl";
import { ShoppingCart, Trash2 } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/shared/ui/sheet";
import { Button } from "@/shared/ui/button";
import { useCartQuery, useClearCart, CartItemRow } from "@/features/cart";
import { formatPrice } from "@/shared/lib/format";

/** Standalone trigger — renders the cart icon as a Link to /cart with an animated item-count badge. */
export function CartIconButton() {
  const { data: cart } = useCartQuery();
  const itemCount = cart?.itemCount ?? 0;
  const t = useTranslations("cart");

  return (
    <Link
      href="/cart"
      className="relative p-2 rounded-lg hover:bg-accent transition-colors"
      aria-label={t("openCart")}
    >
      <ShoppingCart className="h-5 w-5" />
      {itemCount > 0 && (
        <motion.span
          key={itemCount}
          initial={{ scale: 1.5, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: "spring", stiffness: 400, damping: 15 }}
          className="absolute -top-0.5 -right-0.5 h-4 w-4 flex items-center justify-center rounded-full bg-destructive text-destructive-foreground text-[10px] font-bold leading-none"
        >
          {itemCount > 99 ? "99+" : itemCount}
        </motion.span>
      )}
    </Link>
  );
}

/** Slide-in cart Sheet. Listens to the "cart:open" custom event to open itself. */
export function CartDrawer() {
  const [open, setOpen] = useState(false);
  const { data: cart, isLoading } = useCartQuery();
  const clearCart = useClearCart();

  useEffect(() => {
    const handler = () => setOpen(true);
    window.addEventListener("cart:open", handler);
    return () => window.removeEventListener("cart:open", handler);
  }, []);

  const items = cart?.items ?? [];
  const hasOutOfStock = items.some((i) => !i.inStock);

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetContent side="right" className="w-full sm:max-w-md flex flex-col p-0">
        <SheetHeader className="px-6 py-4 border-b shrink-0">
          <SheetTitle className="flex items-center gap-2 text-base">
            <ShoppingCart className="h-5 w-5" />
            Cart
            {items.length > 0 && (
              <span className="text-sm font-normal text-muted-foreground">
                ({items.length} {items.length === 1 ? "item" : "items"})
              </span>
            )}
          </SheetTitle>
        </SheetHeader>

        {isLoading ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-primary" />
          </div>
        ) : items.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center gap-4 px-6">
            <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center">
              <ShoppingCart className="h-8 w-8 text-muted-foreground/40" />
            </div>
            <div className="text-center">
              <p className="font-medium text-foreground">Your cart is empty</p>
              <p className="text-sm text-muted-foreground mt-1">
                Browse our products and add items to your cart
              </p>
            </div>
            <Button asChild variant="outline" onClick={() => setOpen(false)}>
              <Link href="/products">Browse Products</Link>
            </Button>
          </div>
        ) : (
          <>
            {/* Item list — scrollable */}
            <div className="flex-1 overflow-y-auto px-6 min-h-0">
              <div className="divide-y">
                {items.map((item) => (
                  <CartItemRow key={item.skuId} item={item} />
                ))}
              </div>
            </div>

            {/* Footer */}
            <div className="px-6 py-4 border-t space-y-3 shrink-0">
              {hasOutOfStock && (
                <p className="text-xs text-destructive font-medium">
                  Some items are out of stock. Remove them to continue.
                </p>
              )}

              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">Subtotal</span>
                <span className="font-semibold text-foreground">
                  {formatPrice(cart!.totalAmount)}
                </span>
              </div>

              <div className="h-px bg-border" />

              <Button
                asChild
                className="w-full"
                disabled={hasOutOfStock}
                onClick={() => setOpen(false)}
              >
                <Link href="/checkout">Proceed to Checkout</Link>
              </Button>

              <button
                onClick={() => clearCart.mutate()}
                disabled={clearCart.isPending}
                className="w-full flex items-center justify-center gap-1.5 text-xs text-muted-foreground hover:text-destructive transition-colors py-1"
              >
                <Trash2 className="h-3.5 w-3.5" />
                Clear cart
              </button>
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  );
}
