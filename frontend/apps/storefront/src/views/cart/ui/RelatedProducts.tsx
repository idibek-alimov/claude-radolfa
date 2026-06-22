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
  const t = useTranslations("cart");
  const { data, isLoading } = useQuery({
    queryKey: ["listings", "related"],
    queryFn: () => fetchListings(1, 8),
  });

  const items = data?.content.slice(0, 5) ?? [];

  if (!isLoading && items.length === 0) return null;

  return (
    <section className="mt-10 pt-8 border-t border-ink/8">
      <h2 className="font-black text-xl sm:text-2xl mb-4">{t("youMightAlsoLike")}</h2>

      {isLoading ? (
        <div className="flex gap-3 overflow-x-auto scrollbar-hide pb-2 -mx-4 px-4 lg:mx-0 lg:px-0 lg:grid lg:grid-cols-5 lg:gap-4 lg:overflow-visible">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="shrink-0 w-40 lg:w-auto">
              <ProductCardSkeleton />
            </div>
          ))}
        </div>
      ) : (
        <motion.div
          variants={staggerContainer}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, margin: "-50px" }}
          className="flex gap-3 overflow-x-auto scrollbar-hide pb-2 -mx-4 px-4 lg:mx-0 lg:px-0 lg:grid lg:grid-cols-5 lg:gap-4 lg:overflow-visible"
        >
          {items.map((item) => (
            <motion.div key={item.slug} variants={staggerItem} className="shrink-0 w-40 lg:w-auto">
              <ProductCard listing={item} />
            </motion.div>
          ))}
        </motion.div>
      )}
    </section>
  );
}
