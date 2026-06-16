"use client";

import { notFound } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { fetchListingBySlug, ProductDetailSkeleton, type Sku } from "@/entities/product";
import { useAuth } from "@radolfa/shared/auth";
import ProductGallery from "./ProductGallery";
import ProductBreadcrumb from "./ProductBreadcrumb";
import BuyBox from "./BuyBox";
import MobileBuyBar from "./MobileBuyBar";
import TrustCard from "./TrustCard";
import DescriptionBlock from "./DescriptionBlock";
import SpecsTable from "./SpecsTable";
import ReviewsSection from "./ReviewsSection";
import QuestionsSection from "./QuestionsSection";
import RelatedProducts from "./RelatedProducts";

/* ── Main component ────────────────────────────────────────────── */

interface ProductDetailViewProps {
  slug: string;
}

export default function ProductDetailView({ slug }: ProductDetailViewProps) {
  const [selectedSku, setSelectedSku] = useState<Sku | null>(null);

  const { isAuthenticated } = useAuth();

  /* ── Queries ─────────────────────────────────────────────────── */

  const {
    data: listing,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["listing", slug],
    queryFn: () => fetchListingBySlug(slug),
    enabled: slug.length > 0,
  });

  /* ── Loading / Error ─────────────────────────────────────────── */

  if (isLoading) return <ProductDetailSkeleton />;

  if (isError || !listing) {
    notFound();
  }

  /* ── Render ──────────────────────────────────────────────────── */

  const productName = listing.colorDisplayName;

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
      className="pt-4 sm:pt-6 pb-20 md:pb-6"
    >
      {/* ── Breadcrumb (desktop — above the gallery/buy-box grid) ──── */}
      <ProductBreadcrumb
        categoryName={listing.categoryName}
        productName={productName}
        className="hidden lg:block max-w-[1440px] mx-auto px-4 sm:px-6 pt-4 text-[12px] text-ink/55"
      />

      {/* ── Main product — gallery + buy box ─────────────────────── */}
      {/*
          One 12-col grid. Desktop uses explicit row/col placement;
          mobile uses order-* to keep: gallery → breadcrumb → buybox → specs → description.
          The aside spans 3 rows on desktop so its sticky child travels the full left height.
      */}
      <section
        className="max-w-[1440px] mx-auto px-4 sm:px-6 mt-3 grid grid-cols-12 gap-5
                   lg:grid-rows-[auto_auto_auto]"
      >
        {/* ══════════════════════════════════════════════════════════
            LEFT / ROW 1 — Image gallery  (mobile: 1st)
           ══════════════════════════════════════════════════════════ */}
        <div className="col-span-12 lg:col-span-7 order-1 lg:order-none lg:col-start-1 lg:row-start-1">
          <ProductGallery
            images={listing.images}
            productName={productName}
            discountPercentage={listing.discountPercentage}
          />
        </div>

        {/* ── Breadcrumb (mobile only — between gallery and buy box) ── */}
        <div className="col-span-12 lg:hidden order-2">
          <ProductBreadcrumb
            categoryName={listing.categoryName}
            productName={productName}
            className="text-[12px] text-ink/55"
          />
        </div>

        {/* ══════════════════════════════════════════════════════════
            RIGHT — Buy box + Trust, spans rows 1-3, sticky  (mobile: 3rd)
           ══════════════════════════════════════════════════════════ */}
        <aside
          className="col-span-12 lg:col-span-5 order-3 lg:order-none
                     lg:col-start-8 lg:row-start-1 lg:row-span-3"
        >
          <div className="lg:sticky lg:top-32 space-y-4">
            <BuyBox
              listing={listing}
              selectedSku={selectedSku}
              onSelectSku={setSelectedSku}
            />
            {/* ── Trust signals (delivery / returns / warranty) ────── */}
            <TrustCard />
          </div>
        </aside>

        {/* ══════════════════════════════════════════════════════════
            LEFT / ROW 2 — Specifications  (mobile: 4th)
           ══════════════════════════════════════════════════════════ */}
        <div className="col-span-12 lg:col-span-7 order-4 lg:order-none lg:col-start-1 lg:row-start-2">
          <SpecsTable listing={listing} selectedSku={selectedSku} />
        </div>

        {/* ══════════════════════════════════════════════════════════
            LEFT / ROW 3 — Description  (mobile: 5th)
           ══════════════════════════════════════════════════════════ */}
        <div className="col-span-12 lg:col-span-7 order-5 lg:order-none lg:col-start-1 lg:row-start-3">
          <DescriptionBlock webDescription={listing.webDescription} />
        </div>
      </section>

      {/* ── Reviews ───────────────────────────────────────────────── */}
      <div id="reviews">
        <ReviewsSection
          slug={slug}
          listingVariantId={listing.variantId}
          isAuthenticated={isAuthenticated}
          reviewTraits={listing.reviewTraits ?? []}
        />
      </div>

      {/* ── Questions ─────────────────────────────────────────────── */}
      <QuestionsSection
        slug={slug}
        productBaseId={listing.productBaseId}
        listingVariantId={listing.variantId}
        isAuthenticated={isAuthenticated}
      />

      {/* ── Related products — "You May Also Like" ────────────────── */}
      <RelatedProducts currentSlug={slug} />

      {/* ── Sticky Add-to-Bag bar (mobile) ───────────────────────────── */}
      <MobileBuyBar listing={listing} selectedSku={selectedSku} />
    </motion.div>
  );
}
