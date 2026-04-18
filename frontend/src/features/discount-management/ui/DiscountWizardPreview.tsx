"use client";

import { ImageOff } from "lucide-react";

interface Props {
  title: string;
  colorHex: string;    // 6-char, no #
  discountValue: number; // 1–99
}

const SAMPLE_PRICE = 1000;

export function DiscountWizardPreview({ title, colorHex, discountValue }: Props) {
  const discountedPrice = Math.round(SAMPLE_PRICE * (1 - discountValue / 100));

  return (
    <div className="rounded-xl border border-border bg-card overflow-hidden max-w-[240px] shadow-sm">
      {/* Image area */}
      <div className="relative aspect-[4/5] bg-muted/40 flex items-center justify-center">
        <ImageOff className="h-10 w-10 text-muted-foreground/30" />
        {/* Campaign badge — top-left */}
        <span
          className="absolute top-2 left-2 inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold text-white leading-tight"
          style={{ backgroundColor: `#${colorHex}` }}
        >
          {title} −{discountValue}%
        </span>
      </div>

      {/* Body */}
      <div className="p-3 space-y-1.5">
        <p className="text-sm font-medium text-foreground">Example Product</p>
        <div className="flex items-baseline gap-2">
          <span className="text-lg font-bold tabular-nums text-rose-600">
            {discountedPrice.toLocaleString()} TJS
          </span>
          <span className="text-xs tabular-nums text-muted-foreground line-through">
            {SAMPLE_PRICE.toLocaleString()} TJS
          </span>
        </div>
        <p className="text-xs text-muted-foreground leading-snug">
          This is how the campaign appears on product cards.
        </p>
      </div>
    </div>
  );
}
