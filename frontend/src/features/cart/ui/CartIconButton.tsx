"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { useTranslations } from "next-intl";
import { ShoppingCart } from "lucide-react";
import { useCartQuery } from "../hooks/useCart";

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
