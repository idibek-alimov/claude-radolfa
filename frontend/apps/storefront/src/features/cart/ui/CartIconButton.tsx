"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { useTranslations } from "next-intl";
import { useCartQuery } from "../hooks/useCart";

/** Bag icon with item-count badge, styled to B-Magenta reference. */
export function CartIconButton() {
  const { data: cart } = useCartQuery();
  const itemCount = cart?.itemCount ?? 0;
  const t = useTranslations("cart");

  return (
    <Link
      href="/cart"
      className="px-3 py-2 rounded hover:bg-plum/40 inline-flex items-center gap-2 relative"
      aria-label={t("openCart")}
    >
      <span className="relative">
        {/* Bag SVG from B-Magenta reference */}
        <svg
          width="22"
          height="22"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          aria-hidden
        >
          <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z" />
          <path d="M3 6h18" />
          <path d="M16 10a4 4 0 0 1-8 0" />
        </svg>
        {itemCount > 0 && (
          <motion.span
            key={itemCount}
            initial={{ scale: 1.5, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ type: "spring", stiffness: 400, damping: 15 }}
            className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-mag text-white text-[10px] font-bold flex items-center justify-center"
          >
            {itemCount > 99 ? "99+" : itemCount}
          </motion.span>
        )}
      </span>
      <span className="text-[13px] font-semibold hidden sm:inline">
        {t("bag")}
      </span>
    </Link>
  );
}
