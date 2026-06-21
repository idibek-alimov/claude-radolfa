"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";

export function SlimHeader() {
  const t = useTranslations("checkout.slim");

  return (
    <header className="bg-white border-b border-ink/8 sticky top-0 z-30">
      {/* Desktop */}
      <div className="hidden md:flex max-w-[1240px] mx-auto px-6 h-16 items-center gap-5">
        <Link href="/" className="shrink-0">
          <span className="text-2xl font-extrabold tracking-tight text-ink hover:text-mag transition-colors">
            Radolfa
          </span>
        </Link>
        <Link
          href="/cart"
          className="text-[13px] font-medium text-ink/55 hover:text-mag inline-flex items-center gap-1.5"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
            <path d="M19 12H5" />
            <path d="m12 19-7-7 7-7" />
          </svg>
          {t("backToBag")}
        </Link>
        <div className="ml-auto flex items-center gap-2 text-[13px] font-semibold text-emerald">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="11" width="18" height="11" rx="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
          {t("secureCheckout")}
        </div>
      </div>

      {/* Mobile */}
      <div className="flex md:hidden px-4 h-14 items-center gap-3">
        <Link
          href="/cart"
          className="h-9 w-9 flex items-center justify-center -ml-1 text-ink"
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
            <path d="M19 12H5" />
            <path d="m12 19-7-7 7-7" />
          </svg>
        </Link>
        <h1 className="font-black text-[17px] leading-none">{t("title")}</h1>
        <div className="ml-auto flex items-center gap-1.5 text-[12px] font-semibold text-emerald">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="11" width="18" height="11" rx="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
          {t("secure")}
        </div>
      </div>
    </header>
  );
}
