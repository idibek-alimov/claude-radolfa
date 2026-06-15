"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { motion } from "framer-motion";
import { useTranslations } from "next-intl";
import { fetchListings, ProductCard, ProductCardSkeleton } from "@/entities/product";

const RELATED_COUNT = 5;

/* ── Animation variants ────────────────────────────────────────── */

const staggerContainer = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.1 } },
};

const staggerItem = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.3 } },
};

/* ── Component ─────────────────────────────────────────────────── */

interface RelatedProductsProps {
  /** Slug of the product currently being viewed — excluded from the rail. */
  currentSlug: string;
}

/**
 * "You might also like" rail — full-width 1440px section below the
 * Reviews/Q&A blocks. No dedicated "related" endpoint exists yet, so this
 * over-fetches the listing grid and excludes the current product client-side.
 */
export default function RelatedProducts({ currentSlug }: RelatedProductsProps) {
  const t = useTranslations("productDetail");

  const { data, isLoading } = useQuery({
    queryKey: ["listings", "related"],
    queryFn: () => fetchListings(1, 8),
  });

  const relatedProducts = useMemo(() => {
    if (!data) return [];
    return data.content.filter((item) => item.slug !== currentSlug).slice(0, RELATED_COUNT);
  }, [data, currentSlug]);

  if (!isLoading && relatedProducts.length === 0) return null;

  return (
    <section className="max-w-[1440px] mx-auto px-4 sm:px-6 mt-10">
      <div className="flex items-baseline justify-between mb-4">
        <h2 className="font-black text-lg sm:text-3xl">{t("youMayAlsoLike")}</h2>
        <Link href="/search" className="text-mag text-[14px] font-bold hover:underline">
          {t("seeAll")}
        </Link>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          {Array.from({ length: RELATED_COUNT }).map((_, i) => (
            <ProductCardSkeleton key={i} />
          ))}
        </div>
      ) : (
        <motion.div
          variants={staggerContainer}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, margin: "-50px" }}
          className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4"
        >
          {relatedProducts.map((item) => (
            <motion.div key={item.slug} variants={staggerItem}>
              <ProductCard listing={item} />
            </motion.div>
          ))}
        </motion.div>
      )}
    </section>
  );
}
