"use client";

import Image from "next/image";
import { storefrontUrl } from "@radolfa/shared/lib";

export interface VariantPill {
  slug: string;
  label: string;
  thumbnail: string | null;
  isActive: boolean;
}

interface ReviewVariantFilterStripProps {
  variants: VariantPill[];
}

export function ReviewVariantFilterStrip({ variants }: ReviewVariantFilterStripProps) {
  if (variants.length <= 1) return null;

  return (
    <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
      {/* Cross-app links: open storefront product reviews in a new tab */}
      {variants.map((v) => (
        <a
          key={v.slug}
          href={storefrontUrl(`/products/${v.slug}/reviews`)}
          target="_blank"
          rel="noopener noreferrer"
          className={`flex items-center gap-2 rounded-full border px-3 py-1.5 shrink-0 transition-colors ${
            v.isActive
              ? "border-primary bg-primary/5 text-primary"
              : "border-border text-muted-foreground hover:border-foreground hover:text-foreground"
          }`}
        >
          {v.thumbnail && (
            <div className="relative w-8 h-8 rounded-full overflow-hidden shrink-0 border">
              <Image
                src={v.thumbnail}
                alt={v.label}
                fill
                className="object-cover"
                unoptimized
              />
            </div>
          )}
          <span className="text-sm font-medium whitespace-nowrap">{v.label}</span>
        </a>
      ))}
    </div>
  );
}
