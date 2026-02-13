"use client";

import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import {
  ProductCard,
  ProductCardSkeleton,
  fetchHomeCollections,
} from "@/entities/product";
import type { HomeSection } from "@/entities/product";

const staggerContainer = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.06 } },
};

const staggerItem = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.35 } },
};

const SKELETON_SECTIONS = 2;
const SKELETON_CARDS = 4;

export default function HomeCollections() {
  const { data: sections, isLoading } = useQuery({
    queryKey: ["home", "collections"],
    queryFn: fetchHomeCollections,
  });

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 space-y-14">
        {Array.from({ length: SKELETON_SECTIONS }).map((_, i) => (
          <SectionSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (!sections || sections.length === 0) return null;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 space-y-14">
      {sections.map((section) => (
        <CollectionRow key={section.key} section={section} />
      ))}
    </div>
  );
}

function CollectionRow({ section }: { section: HomeSection }) {
  return (
    <section>
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <h2 className="text-2xl sm:text-3xl font-bold text-foreground">
          {section.title}
        </h2>
        <Link
          href={`/collections/${section.key}`}
          className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary/80 transition-colors"
        >
          View All
          <ArrowRight className="h-4 w-4" />
        </Link>
      </div>

      {/* Desktop grid */}
      <motion.div
        variants={staggerContainer}
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-50px" }}
        className="hidden md:grid md:grid-cols-4 gap-5"
      >
        {section.items.slice(0, 8).map((item) => (
          <motion.div key={item.slug} variants={staggerItem}>
            <ProductCard listing={item} />
          </motion.div>
        ))}
      </motion.div>

      {/* Mobile horizontal scroll */}
      <motion.div
        variants={staggerContainer}
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-50px" }}
        className="flex md:hidden gap-4 overflow-x-auto pb-4 -mx-4 px-4 snap-x snap-mandatory"
      >
        {section.items.map((item) => (
          <motion.div
            key={item.slug}
            variants={staggerItem}
            className="min-w-[70vw] sm:min-w-[45vw] snap-start"
          >
            <ProductCard listing={item} />
          </motion.div>
        ))}
      </motion.div>
    </section>
  );
}

function SectionSkeleton() {
  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div className="h-8 w-48 bg-muted rounded animate-pulse" />
        <div className="h-5 w-20 bg-muted rounded animate-pulse" />
      </div>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-5">
        {Array.from({ length: SKELETON_CARDS }).map((_, i) => (
          <ProductCardSkeleton key={i} />
        ))}
      </div>
    </div>
  );
}
