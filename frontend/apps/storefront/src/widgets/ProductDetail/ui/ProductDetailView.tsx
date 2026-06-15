"use client";

import { notFound } from "next/navigation";
import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { Copy, Check } from "lucide-react";
import {
  fetchListingBySlug,
  fetchListings,
  ProductCard,
  ProductCardSkeleton,
  ProductDetailSkeleton,
  type Sku,
} from "@/entities/product";
import { useAuth } from "@radolfa/shared/auth";
import { ReviewsAndQuestionsSection } from "@/widgets/reviews-questions";
import { useTranslations } from "next-intl";
import ProductGallery from "./ProductGallery";
import BuyBox from "./BuyBox";

/* ── Animation variants ────────────────────────────────────────── */

const staggerContainer = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.1 } },
};

const staggerItem = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.3 } },
};

/* ── Main component ────────────────────────────────────────────── */

interface ProductDetailViewProps {
  slug: string;
}

export default function ProductDetailView({ slug }: ProductDetailViewProps) {
  const t = useTranslations("productDetail");
  const [selectedSku, setSelectedSku] = useState<Sku | null>(null);
  const [specsExpanded, setSpecsExpanded] = useState(false);
  const [codeCopied, setCodeCopied] = useState(false);

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

  const { data: relatedData, isLoading: relatedLoading } = useQuery({
    queryKey: ["listings", "related"],
    queryFn: () => fetchListings(1, 8),
  });

  /* ── Related products (exclude current) ─────────────────────── */

  const relatedProducts = useMemo(() => {
    if (!relatedData) return [];
    return relatedData.content.filter((item) => item.slug !== slug).slice(0, 4);
  }, [relatedData, slug]);

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
      className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6"
    >
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-10">
        {/* ══════════════════════════════════════════════════════════
            LEFT — Image gallery
           ══════════════════════════════════════════════════════════ */}
        <div className="lg:col-span-7">
          <ProductGallery
            images={listing.images}
            productName={productName}
            discountPercentage={listing.discountPercentage}
          />
        </div>

        {/* ══════════════════════════════════════════════════════════
            RIGHT — Product info (5 cols)
           ══════════════════════════════════════════════════════════ */}
        <div className="lg:col-span-5 space-y-5">
          {/* ── Buy box ─────────────────────────────────────────── */}
          <div className="sticky top-32 space-y-4">
            <BuyBox
              listing={listing}
              selectedSku={selectedSku}
              onSelectSku={setSelectedSku}
            />
          </div>

          {/* ── Product code ─────────────────────────────────────── */}
          {listing.productCode && (
            <div className="flex items-center gap-2 py-2 px-2 rounded bg-muted/30 text-sm">
              <span className="text-muted-foreground min-w-[120px] shrink-0">
                Код товара
              </span>
              <span className="font-medium text-foreground">
                {listing.productCode}
              </span>
              <button
                onClick={() => {
                  navigator.clipboard.writeText(listing.productCode!);
                  setCodeCopied(true);
                  setTimeout(() => setCodeCopied(false), 2000);
                }}
                className="ml-1 p-1 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                aria-label="Копировать код товара"
              >
                {codeCopied ? (
                  <Check className="w-3.5 h-3.5 text-green-500" />
                ) : (
                  <Copy className="w-3.5 h-3.5" />
                )}
              </button>
            </div>
          )}

          {/* ── Description ──────────────────────────────────────── */}
          {listing.webDescription && (
            <div className="pt-4 border-t">
              <h2 className="text-sm font-semibold text-foreground mb-2">
                {t("aboutProduct")}
              </h2>
              <p className="text-sm text-muted-foreground leading-relaxed">
                {listing.webDescription}
              </p>
            </div>
          )}

          {/* ── Attributes / Specifications ──────────────────────── */}
          {listing.attributes && listing.attributes.length > 0 && (
            <div className="pt-4 border-t">
              <h2 className="text-sm font-semibold text-foreground mb-3">
                {t("specifications")}
              </h2>
              <div className="space-y-0">
                {(specsExpanded
                  ? listing.attributes
                  : listing.attributes.slice(0, 5)
                ).map((attr, idx) => (
                  <div
                    key={attr.key}
                    className={`flex items-baseline gap-2 py-2 text-sm ${
                      idx % 2 === 0 ? "bg-muted/30" : ""
                    } rounded px-2`}
                  >
                    <span className="text-muted-foreground min-w-[120px] shrink-0">
                      {attr.key}
                    </span>
                    <span className="text-foreground font-medium">
                      {attr.values.join(", ")}
                    </span>
                  </div>
                ))}
                {listing.attributes.length > 5 && (
                  <button
                    onClick={() => setSpecsExpanded(!specsExpanded)}
                    className="text-sm text-primary hover:underline mt-2 px-2"
                  >
                    {specsExpanded
                      ? "Show less"
                      : `Show all (${listing.attributes.length})`}
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Reviews & Questions ───────────────────────────────────── */}
      <div id="reviews">
        <ReviewsAndQuestionsSection
          slug={slug}
          productBaseId={listing.productBaseId}
          listingVariantId={listing.variantId}
          isAuthenticated={isAuthenticated}
          reviewTraits={listing.reviewTraits ?? []}
        />
      </div>

      {/* ── Related products — "You May Also Like" ────────────────── */}
      <div className="mt-12 pt-8 border-t">
        <h2 className="text-xl font-semibold text-foreground mb-6">
          {t("youMayAlsoLike")}
        </h2>

        {relatedLoading ? (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-6">
            {Array.from({ length: 4 }).map((_, i) => (
              <ProductCardSkeleton key={i} />
            ))}
          </div>
        ) : relatedProducts.length > 0 ? (
          <>
            {/* Desktop grid */}
            <motion.div
              variants={staggerContainer}
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, margin: "-50px" }}
              className="hidden md:grid md:grid-cols-4 gap-6"
            >
              {relatedProducts.map((item) => (
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
              {relatedProducts.map((item) => (
                <motion.div
                  key={item.slug}
                  variants={staggerItem}
                  className="min-w-[70vw] sm:min-w-[45vw] snap-start"
                >
                  <ProductCard listing={item} />
                </motion.div>
              ))}
            </motion.div>
          </>
        ) : null}
      </div>
    </motion.div>
  );
}
