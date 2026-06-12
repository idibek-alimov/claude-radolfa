"use client";

import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { useTranslations } from "next-intl";
import {
  fetchListings,
  ProductCard,
  ProductCardSkeleton,
} from "@/entities/product";

const staggerContainer = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.1 } },
};

const staggerItem = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.3 } },
};

export function RelatedProducts() {
  const t = useTranslations("productDetail");
  const { data, isLoading } = useQuery({
    queryKey: ["listings", "related"],
    queryFn: () => fetchListings(1, 8),
  });

  const items = data?.content.slice(0, 4) ?? [];

  if (!isLoading && items.length === 0) return null;

  return (
    <section className="mt-12 pt-8 border-t">
      <h2 className="text-xl font-semibold text-foreground mb-6">
        {t("youMayAlsoLike")}
      </h2>

      {isLoading ? (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-6">
          {Array.from({ length: 4 }).map((_, i) => (
            <ProductCardSkeleton key={i} />
          ))}
        </div>
      ) : (
        <motion.div
          variants={staggerContainer}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, margin: "-50px" }}
          className="grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-6"
        >
          {items.map((item) => (
            <motion.div key={item.slug} variants={staggerItem}>
              <ProductCard listing={item} />
            </motion.div>
          ))}
        </motion.div>
      )}
    </section>
  );
}
