"use client";

import { notFound, useRouter } from "next/navigation";
import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import {
  Crown,
  Copy,
  Check,
  Minus,
  Plus,
  ShoppingCart,
} from "lucide-react";
import {
  fetchListingBySlug,
  fetchListings,
  StockBadge,
  ProductCard,
  ProductCardSkeleton,
  ProductDetailSkeleton,
  type Sku,
} from "@/entities/product";
import { useAddToCart } from "@/features/cart";
import { useAuth } from "@radolfa/shared/auth";
import { ReviewsAndQuestionsSection } from "@/widgets/reviews-questions";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { getErrorMessage } from "@radolfa/shared/lib";
import { Badge } from "@radolfa/shared/ui/badge";
import { Button } from "@radolfa/shared/ui/button";
import { formatPrice } from "@radolfa/shared/lib/format";
import ProductGallery from "./ProductGallery";

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
  const tc = useTranslations("common");
  const tCart = useTranslations("cart");
  const router = useRouter();
  const [selectedSku, setSelectedSku] = useState<Sku | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [specsExpanded, setSpecsExpanded] = useState(false);
  const [codeCopied, setCodeCopied] = useState(false);

  const addToCart = useAddToCart();
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

  /* ── Price computation ──────────────────────────────────────── */

  // When a SKU is selected, use its per-SKU pricing; otherwise fall back to variant-level.
  const activeOriginal     = selectedSku?.originalPrice     ?? listing.originalPrice;
  const activeDiscount     = selectedSku?.discountPrice     ?? listing.discountPrice;
  const activeDiscountPct  = selectedSku?.discountPercentage ?? listing.discountPercentage;
  const activeDiscountName = selectedSku?.discountName      ?? listing.discountName;
  const activeDiscountHex  = selectedSku?.discountColorHex  ?? listing.discountColorHex;
  const activeLoyalty      = selectedSku?.loyaltyPrice      ?? listing.loyaltyPrice;

  const hasDiscount     = activeDiscount != null;
  const hasLoyalty      = activeLoyalty != null;
  const hasCheaperPrice = hasDiscount || hasLoyalty;

  const totalStock = listing.skus.reduce((acc, s) => acc + s.stockQuantity, 0);
  const currentStock = selectedSku ? selectedSku.stockQuantity : totalStock;

  /* ── Render ──────────────────────────────────────────────────── */

  const productName = listing.colorDisplayName ?? listing.productCode ?? slug;

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
          {/* Category / Brand line */}
          {listing.categoryName && (
            <span className="text-sm text-primary font-medium">
              {listing.categoryName}
            </span>
          )}

          {/* Product name + badges */}
          <div>
            <h1 className="text-xl sm:text-2xl lg:text-[1.65rem] font-semibold text-foreground leading-snug">
              {productName}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              Sold by:{" "}
              <span className="font-medium">
                {listing.sellerShopName ?? "Radolfa"}
              </span>
            </p>
          </div>

          {/* ── Price block ───────────────────────────────────────── */}
          <div className="bg-muted/40 rounded-xl px-3 py-3 sm:p-4 space-y-1.5 sm:space-y-2">

            {/* Hero price */}
            <div className="flex items-center gap-1.5 sm:gap-2 flex-wrap">
              {hasLoyalty ? (
                <>
                  <Crown className="h-4 w-4 sm:h-5 sm:w-5 text-amber-500 shrink-0" />
                  <span className="text-2xl sm:text-[2rem] font-bold text-amber-600">
                    {formatPrice(activeLoyalty!)}
                  </span>
                  <span className="text-xs sm:text-sm font-medium text-amber-600/70">
                    {tc("yourPrice")}
                  </span>
                  {hasDiscount && (
                    <span
                      className="text-xs font-semibold px-2 py-0.5 rounded-full text-white"
                      style={{ backgroundColor: `#${activeDiscountHex ?? "ef4444"}` }}
                    >
                      {activeDiscountName} · -{activeDiscountPct}%
                    </span>
                  )}
                </>
              ) : hasDiscount ? (
                <>
                  <span className="text-2xl sm:text-[2rem] font-bold text-red-600">
                    {formatPrice(activeDiscount!)}
                  </span>
                  <span
                    className="text-xs font-semibold px-2 py-0.5 rounded-full text-white"
                    style={{ backgroundColor: `#${activeDiscountHex ?? "ef4444"}` }}
                  >
                    {activeDiscountName} · -{activeDiscountPct}%
                  </span>
                </>
              ) : (
                <span className="text-2xl sm:text-[2rem] font-bold text-violet-600">
                  {formatPrice(activeOriginal)}
                </span>
              )}
            </div>

            {/* Strikethrough row */}
            {hasCheaperPrice && (
              <div className="flex items-baseline gap-1.5 sm:gap-2">
                <span className="text-xs sm:text-sm text-muted-foreground line-through">
                  {formatPrice(activeOriginal)}
                </span>
                {hasDiscount && !hasLoyalty && (
                  <span
                    className="text-xs font-semibold px-1.5 py-0.5 rounded text-white"
                    style={{ backgroundColor: `#${activeDiscountHex ?? "ef4444"}` }}
                  >
                    -{activeDiscountPct}%
                  </span>
                )}
              </div>
            )}

            {/* Loyalty tier detail */}
            {hasLoyalty && listing.loyaltyPercentage != null && (
              <p className="text-xs text-amber-600 font-medium">
                {tc("loyaltyTierDetail", { pct: listing.loyaltyPercentage })}
              </p>
            )}

            {/* Partial discount hint — shown at variant level before a size is selected */}
            {listing.isPartialDiscount && !selectedSku && (
              <p className="text-xs text-muted-foreground">
                {t("partialDiscountHint")}
              </p>
            )}

          </div>

          {/* ── Colour display ────────────────────────────────────── */}
          {listing.colorKey && (
            <div className="flex items-center gap-2">
              <p className="text-sm text-muted-foreground">
                {t("color")}:
              </p>
              {listing.colorHex && (
                <span
                  className="w-5 h-5 rounded-full border border-muted-foreground/20 inline-block"
                  style={{ backgroundColor: listing.colorHex }}
                />
              )}
              <span className="text-sm font-medium text-foreground">
                {listing.colorDisplayName ?? listing.colorKey}
              </span>
            </div>
          )}

          {/* ── Size selector ────────────────────────────────────── */}
          {listing.skus && listing.skus.length > 0 && (
            <div>
              <p className="text-sm text-muted-foreground mb-2">
                {t("availableSizes")}
              </p>
              <div className="flex gap-2 flex-wrap">
                {listing.skus.map((skuItem) => {
                  const isOutOfStock = skuItem.stockQuantity === 0;
                  const isSelected = selectedSku?.skuId === skuItem.skuId;

                  return (
                    <button
                      key={skuItem.skuId}
                      onClick={() =>
                        !isOutOfStock && (
                        setSelectedSku(isSelected ? null : skuItem),
                        setQuantity(1)
                      )
                      }
                      disabled={isOutOfStock}
                      className={`
                        min-w-[48px] h-10 px-3 rounded-lg border text-sm font-medium
                        transition-all relative
                        ${
                          isSelected
                            ? "border-primary bg-primary text-primary-foreground shadow-sm"
                            : isOutOfStock
                              ? "border-muted bg-muted text-muted-foreground/40 cursor-not-allowed"
                              : "border-input bg-background hover:border-primary/60 hover:shadow-sm"
                        }
                      `}
                    >
                      {skuItem.sizeLabel}
                      {skuItem.discountPrice != null && (
                        <span
                          className="absolute -top-1 -right-1 w-2 h-2 rounded-full border border-background"
                          style={{ backgroundColor: `#${skuItem.discountColorHex ?? "ef4444"}` }}
                        />
                      )}
                      {isOutOfStock && (
                        <span className="absolute inset-0 flex items-center justify-center">
                          <span className="block w-[calc(100%-12px)] h-px bg-muted-foreground/30 rotate-[-20deg]" />
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* ── Stock indicator ──────────────────────────────────── */}
          <div className="flex items-center gap-3">
            <StockBadge stock={currentStock} />
            {currentStock > 0 && currentStock <= 5 && (
              <span className="text-xs text-orange-600 font-medium">
                {t("itemsLeft", { count: currentStock })}
              </span>
            )}
          </div>

          {/* ── Quantity + Add to Cart ────────────────────────────── */}
          {selectedSku && selectedSku.stockQuantity > 0 && (
            <div className="space-y-3">
              {/* Quantity stepper */}
              <div className="flex items-center gap-3">
                <span className="text-sm text-muted-foreground">{t("quantity")}</span>
                <div className="flex items-center rounded-lg border bg-background">
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-9 w-9 rounded-r-none"
                    onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                    disabled={quantity <= 1}
                  >
                    <Minus className="h-4 w-4" />
                  </Button>
                  <span className="text-sm font-medium w-10 text-center">{quantity}</span>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-9 w-9 rounded-l-none"
                    onClick={() =>
                      setQuantity((q) => Math.min(selectedSku.stockQuantity, q + 1))
                    }
                    disabled={quantity >= selectedSku.stockQuantity}
                  >
                    <Plus className="h-4 w-4" />
                  </Button>
                </div>
              </div>

              {/* Add to Cart button */}
              <Button
                className="w-full h-11 gap-2"
                disabled={addToCart.isPending}
                onClick={() => {
                  addToCart.mutate(
                    { skuId: selectedSku.skuId, quantity },
                    {
                      onSuccess: () => {
                        // toast.success(t("addedToCart"), {
                        //   action: {
                        //     label: tCart("viewCart"),
                        //     onClick: () => router.push("/cart"),
                        //   },
                        // });
                      },
                      onError: (err) => {
                        toast.error(getErrorMessage(err));
                      },
                    },
                  );
                }}
              >
                <ShoppingCart className="h-4 w-4" />
                {addToCart.isPending ? t("adding") : t("addToCart")}
              </Button>
            </div>
          )}

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
      <ReviewsAndQuestionsSection
        slug={slug}
        productBaseId={listing.productBaseId}
        listingVariantId={listing.variantId}
        isAuthenticated={isAuthenticated}
        reviewTraits={listing.reviewTraits ?? []}
      />

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
