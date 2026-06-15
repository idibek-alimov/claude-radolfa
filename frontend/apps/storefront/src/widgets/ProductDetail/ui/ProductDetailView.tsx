"use client";

import { notFound } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import Link from "next/link";
import { fetchListingBySlug, ProductDetailSkeleton, type Sku } from "@/entities/product";
import { useAuth } from "@radolfa/shared/auth";
import { useTranslations } from "next-intl";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@radolfa/shared/ui/breadcrumb";
import ProductGallery from "./ProductGallery";
import BuyBox from "./BuyBox";
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
  const t = useTranslations("productDetail");
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
      className="py-4 sm:py-6"
    >
      {/* ── Breadcrumb ────────────────────────────────────────────── */}
      <Breadcrumb className="max-w-[1440px] mx-auto px-4 sm:px-6 pt-4 text-[12px] text-ink/55">
        <BreadcrumbList className="gap-1.5 text-[12px] text-ink/55 sm:gap-1.5">
          <BreadcrumbItem>
            <BreadcrumbLink asChild className="hover:text-mag">
              <Link href="/">{t("home")}</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          {listing.categoryName && (
            <>
              <BreadcrumbSeparator>
                <span className="opacity-50">/</span>
              </BreadcrumbSeparator>
              <BreadcrumbItem>
                <span>{listing.categoryName}</span>
              </BreadcrumbItem>
            </>
          )}
          <BreadcrumbSeparator>
            <span className="opacity-50">/</span>
          </BreadcrumbSeparator>
          <BreadcrumbItem>
            <BreadcrumbPage className="text-ink/80 font-semibold">
              {productName}
            </BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      {/* ── Main product — gallery + buy box ─────────────────────── */}
      <section className="max-w-[1440px] mx-auto px-4 sm:px-6 mt-3 grid grid-cols-12 gap-5">
        {/* ══════════════════════════════════════════════════════════
            LEFT — Image gallery
           ══════════════════════════════════════════════════════════ */}
        <div className="col-span-12 lg:col-span-7">
          <ProductGallery
            images={listing.images}
            productName={productName}
            discountPercentage={listing.discountPercentage}
          />
        </div>

        {/* ══════════════════════════════════════════════════════════
            RIGHT — Product info (5 cols)
           ══════════════════════════════════════════════════════════ */}
        <aside className="col-span-12 lg:col-span-5 space-y-4">
          {/* ── Buy box ─────────────────────────────────────────── */}
          <div className="sticky top-32 space-y-4">
            <BuyBox
              listing={listing}
              selectedSku={selectedSku}
              onSelectSku={setSelectedSku}
            />
          </div>

          {/* ── Trust signals (delivery / returns / warranty) ────── */}
          <TrustCard />

          {/* ── Description ──────────────────────────────────────── */}
          <DescriptionBlock webDescription={listing.webDescription} />

          {/* ── Specifications ────────────────────────────────────── */}
          <SpecsTable listing={listing} selectedSku={selectedSku} />
        </aside>
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
    </motion.div>
  );
}
