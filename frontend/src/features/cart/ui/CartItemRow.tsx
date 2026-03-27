"use client";

import Image from "next/image";
import { X, Minus, Plus } from "lucide-react";
import { Button } from "@/shared/ui/button";
import type { CartItem } from "@/entities/cart";
import { formatPrice } from "@/shared/lib/format";
import { useUpdateCartItem, useRemoveCartItem } from "../hooks/useCart";

interface CartItemRowProps {
  item: CartItem;
}

export function CartItemRow({ item }: CartItemRowProps) {
  const updateItem = useUpdateCartItem();
  const removeItem = useRemoveCartItem();

  const isPending = updateItem.isPending || removeItem.isPending;

  const handleQuantityChange = (delta: number) => {
    const newQty = item.quantity + delta;
    if (newQty < 1 || newQty > item.availableStock) return;
    updateItem.mutate({ skuId: item.skuId, quantity: newQty });
  };

  return (
    <div className={`flex gap-3 py-4 ${!item.inStock ? "opacity-60" : ""}`}>
      {/* Thumbnail */}
      <div className="relative w-16 h-20 rounded-lg bg-muted overflow-hidden shrink-0">
        {item.imageUrl ? (
          <Image
            src={item.imageUrl}
            alt={item.productName}
            fill
            className="object-cover"
            unoptimized
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-muted-foreground text-[10px]">
            No img
          </div>
        )}
      </div>

      {/* Details */}
      <div className="flex-1 min-w-0 space-y-1.5">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <p className="text-sm font-medium text-foreground truncate">{item.productName}</p>
            <p className="text-xs text-muted-foreground">
              {item.colorName} · {item.sizeLabel}
            </p>
          </div>
          <Button
            variant="ghost"
            size="icon"
            className="h-6 w-6 shrink-0 text-muted-foreground hover:text-destructive"
            onClick={() => removeItem.mutate(item.skuId)}
            disabled={isPending}
          >
            <X className="h-3.5 w-3.5" />
          </Button>
        </div>

        {!item.inStock && (
          <p className="text-xs text-destructive font-medium">Out of stock</p>
        )}

        <div className="flex items-center justify-between gap-2">
          {/* Quantity stepper */}
          <div className="flex items-center rounded-lg border bg-background">
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 rounded-r-none"
              onClick={() => handleQuantityChange(-1)}
              disabled={isPending || item.quantity <= 1}
            >
              <Minus className="h-3 w-3" />
            </Button>
            <span className="text-sm font-medium w-8 text-center">{item.quantity}</span>
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 rounded-l-none"
              onClick={() => handleQuantityChange(1)}
              disabled={isPending || item.quantity >= item.availableStock}
            >
              <Plus className="h-3 w-3" />
            </Button>
          </div>

          <span className="text-sm font-semibold text-foreground">
            {formatPrice(item.lineTotal)}
          </span>
        </div>
      </div>
    </div>
  );
}
