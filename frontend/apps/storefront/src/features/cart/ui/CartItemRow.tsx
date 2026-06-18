"use client";

import Image from "next/image";
import { Minus, Plus, X, Crown, AlertCircle } from "lucide-react";
import { useTranslations } from "next-intl";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@radolfa/shared/ui/tooltip";
import type { CartItem } from "@/entities/cart";
import { formatPrice } from "@radolfa/shared/lib/format";
import { useUpdateCartItem, useRemoveCartItem } from "../hooks/useCart";

const LOW_STOCK_THRESHOLD = 5;

interface CartItemRowProps {
  item: CartItem;
}

export function CartItemRow({ item }: CartItemRowProps) {
  const t = useTranslations("cart");
  const updateItem = useUpdateCartItem();
  const removeItem = useRemoveCartItem();

  const isPending = updateItem.isPending || removeItem.isPending;
  const isLowStock = item.inStock && item.availableStock <= LOW_STOCK_THRESHOLD;
  const hasStrikethrough = item.unitPrice < item.originalUnitPrice;

  const handleQuantityChange = (delta: number) => {
    const newQty = item.quantity + delta;
    if (newQty < 1 || newQty > item.availableStock) return;
    updateItem.mutate({ skuId: item.skuId, quantity: newQty });
  };

  return (
    <article
      className={`bg-white rounded-2xl border border-ink/8 p-3 sm:p-4 flex gap-3 sm:gap-4 ${!item.inStock ? "opacity-60" : ""}`}
    >
      <div className="w-24 h-28 sm:w-28 sm:h-32 rounded-xl overflow-hidden bg-plum/30 shrink-0 relative">
        {item.imageUrl ? (
          <Image
            src={item.imageUrl}
            alt={item.productName}
            fill
            className="object-cover"
            unoptimized
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-ink/40 text-[10px]">
            {t("noImage")}
          </div>
        )}
        {item.mechanism === "CAMPAIGN" && (
          <span className="absolute top-1.5 sm:top-2 left-1.5 sm:left-2 px-1.5 py-0.5 rounded bg-sale text-white text-[10px] font-bold">
            −{item.discountPercent}%
          </span>
        )}
        {item.mechanism === "LOYALTY" && (
          <span className="absolute top-1.5 sm:top-2 left-1.5 sm:left-2 px-1.5 py-0.5 rounded bg-gold text-ink text-[10px] font-bold inline-flex items-center gap-1">
            <Crown className="h-2.5 w-2.5" fill="currentColor" />−{item.discountPercent}%
          </span>
        )}
      </div>

      <div className="flex-1 min-w-0 flex flex-col">
        <div className="flex items-start justify-between gap-2 sm:gap-3">
          <div className="min-w-0">
            {item.category && (
              <div className="text-[10px] sm:text-[11px] text-ink/55 uppercase tracking-wider font-medium">
                {item.category}
              </div>
            )}
            <div className="text-[14px] sm:text-[15px] font-semibold leading-tight mt-0.5 sm:mt-1 truncate">
              {item.productName}
            </div>
            <div className="text-[11px] sm:text-[12px] text-ink/55 mt-1 sm:mt-1.5">
              {item.colorName} · {item.sizeLabel}
            </div>

            {item.mechanism === "LOYALTY" ? (
              <div className="text-[11px] sm:text-[12px] mt-1 sm:mt-1.5 inline-flex items-center gap-1 sm:gap-1.5 font-semibold text-[#9A6E0F]">
                <Crown className="h-2.5 w-2.5 sm:h-3 sm:w-3" fill="#FFCC4F" stroke="none" />
                {t("crownPriceApplied")}
              </div>
            ) : isLowStock ? (
              <div className="text-[11px] sm:text-[12px] mt-1 sm:mt-1.5 inline-flex items-center gap-1 font-semibold text-[#C06A00]">
                <AlertCircle className="h-2.5 w-2.5 sm:h-3 sm:w-3" />
                {t("onlyLeft", { count: item.availableStock })}
              </div>
            ) : item.inStock ? (
              <div className="text-[11px] sm:text-[12px] text-emerald font-semibold mt-1 sm:mt-1.5 inline-flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald" /> {t("inStock")}
              </div>
            ) : (
              <div className="text-[11px] sm:text-[12px] text-sale font-semibold mt-1 sm:mt-1.5">
                {t("outOfStock")}
              </div>
            )}
          </div>

          <div className="text-right shrink-0">
            <div className="font-black text-mag text-[16px] sm:text-[20px] tabular-nums leading-none">
              {formatPrice(item.unitPrice)}
            </div>
            {hasStrikethrough && (
              <div className="text-[11px] sm:text-[12px] text-ink/45 line-through tabular-nums mt-1">
                {formatPrice(item.originalUnitPrice)}
              </div>
            )}
          </div>
        </div>

        <div className="mt-auto pt-2 sm:pt-3 flex items-center justify-between">
          <div className="inline-flex items-center rounded-full border border-ink/12 h-8 sm:h-9">
            <button
              className="w-8 sm:w-9 h-8 sm:h-9 flex items-center justify-center text-ink/60 hover:text-mag text-lg leading-none disabled:opacity-40"
              onClick={() => handleQuantityChange(-1)}
              disabled={isPending || item.quantity <= 1}
            >
              <Minus className="h-3 w-3" />
            </button>
            <span className="w-6 sm:w-8 text-center text-[13px] sm:text-[14px] font-bold tabular-nums">
              {item.quantity}
            </span>
            <button
              className="w-8 sm:w-9 h-8 sm:h-9 flex items-center justify-center text-ink/60 hover:text-mag text-lg leading-none disabled:opacity-40"
              onClick={() => handleQuantityChange(1)}
              disabled={isPending || item.quantity >= item.availableStock}
            >
              <Plus className="h-3 w-3" />
            </button>
          </div>

          <TooltipProvider delayDuration={300}>
            <Tooltip>
              <TooltipTrigger asChild>
                <button
                  className="p-1.5 rounded hover:bg-plum/40 text-ink/35 hover:text-sale transition-colors disabled:opacity-40"
                  onClick={() => removeItem.mutate(item.skuId)}
                  disabled={isPending}
                >
                  <X className="h-4 w-4" />
                </button>
              </TooltipTrigger>
              <TooltipContent>{t("remove")}</TooltipContent>
            </Tooltip>
          </TooltipProvider>
        </div>
      </div>
    </article>
  );
}
