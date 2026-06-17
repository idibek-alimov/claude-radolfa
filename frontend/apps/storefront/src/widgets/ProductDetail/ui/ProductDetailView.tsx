"use client";

import { notFound } from "next/navigation";
import { useEffect, useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
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
  // activeCode is the productCode (article number) of the colour currently displayed.
  // It starts from the route param and changes in place when the user clicks a color
  // swatch — without a router navigation. The URL is kept in sync via
  // window.history.pushState so the address bar and shareable links stay correct.
  const [activeCode, setActiveCode] = useState(slug);
  const [selectedSku, setSelectedSku] = useState<Sku | null>(null);

  /* ── Reset size when color changes ──────────────────────────── */
  useEffect(() => {
    setSelectedSku(null);
  }, [activeCode]);

  const { isAuthenticated } = useAuth();

  /* ── Queries ─────────────────────────────────────────────────── */

  const {
    data: listing,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["listing", activeCode],
    queryFn: () => fetchListingBySlug(activeCode),
    enabled: activeCode.length > 0,
    // While fetching the new color keep the previous color's content on screen —
    // no skeleton flash, no opacity replay. Works because the component stays
    // mounted (we never navigate the router on color switch).
    placeholderData: keepPreviousData,
  });

  /* ── Color switch — in-place data swap, shallow URL update ───── */
  const handleSelectColor = (nextCode: string) => {
    if (nextCode === activeCode) return;
    setActiveCode(nextCode);
    // Update the address bar without a server navigation or remount.
    // window.history.pushState is the App Router–supported way to do this on
    // Next 15. Browser back/forward still works via a normal popstate.
    window.history.pushState(null, "", `/products/${nextCode}`);
  };

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
        categorySlug={listing.categorySlug}
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
          {/* key={activeCode} remounts only the gallery when the color changes so
              selectedImageIdx resets to 0 (the new color's first image). The
              gallery's per-image motion.div has no mount fade, so this is safe. */}
          <ProductGallery
            key={activeCode}
            images={listing.images}
            productName={productName}
            discountPercentage={listing.discountPercentage}
          />
        </div>

        {/* ── Breadcrumb (mobile only — between gallery and buy box) ── */}
        <div className="col-span-12 lg:hidden order-2">
          <ProductBreadcrumb
            categoryName={listing.categoryName}
            categorySlug={listing.categorySlug}
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
              activeSlug={activeCode}
              selectedSku={selectedSku}
              onSelectSku={setSelectedSku}
              onSelectColor={handleSelectColor}
            />
            {/* ── Trust signals (delivery / returns / warranty) ────── */}
            <TrustCard />
          </div>
        </aside>

        {/* ══════════════════════════════════════════════════════════
            LEFT / ROW 2 — Specifications  (mobile: 4th)
           ══════════════════════════════════════════════════════════ */}
        <div className="col-span-12 lg:col-span-7 order-4 lg:order-none lg:col-start-1 lg:row-start-2">
          <SpecsTable listing={listing} />
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
          slug={activeCode}
          listingVariantId={listing.variantId}
          isAuthenticated={isAuthenticated}
          reviewTraits={listing.reviewTraits ?? []}
        />
      </div>

      {/* ── Questions ─────────────────────────────────────────────── */}
      <QuestionsSection
        slug={activeCode}
        productBaseId={listing.productBaseId}
        listingVariantId={listing.variantId}
        isAuthenticated={isAuthenticated}
      />

      {/* ── Related products — "You May Also Like" ────────────────── */}
      <RelatedProducts currentCode={activeCode} />

      {/* ── Sticky Add-to-Bag bar (mobile) ───────────────────────────── */}
      <MobileBuyBar listing={listing} selectedSku={selectedSku} />
    </motion.div>
  );
}
