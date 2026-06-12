"use client";

import Image from "next/image";
import Link from "next/link";

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
      {variants.map((v) => (
        <Link
          key={v.slug}
          href={`/products/${v.slug}/reviews`}
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
        </Link>
      ))}
    </div>
  );
}
