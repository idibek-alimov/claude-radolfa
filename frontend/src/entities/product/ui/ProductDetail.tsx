"use client";

import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { useState, useCallback, useRef, useMemo, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import {
  ChevronLeft,
  ChevronRight,
  ZoomIn,
  Flame,
  Star,
  Crown,
  Copy,
  Check,
  Minus,
  Plus,
  ShoppingCart,
} from "lucide-react";
import { fetchListingBySlug, fetchListings } from "@/entities/product/api";
import type { Sku } from "@/entities/product";
import { useAddToCart } from "@/features/cart";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import {
  Dialog,
  DialogContent,
  DialogTitle,
} from "@/shared/ui/dialog";
import {
  Breadcrumb,
  BreadcrumbList,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/shared/ui/breadcrumb";
import StockBadge from "./StockBadge";
import ProductCard from "./ProductCard";
import ProductCardSkeleton from "./ProductCardSkeleton";
import ProductDetailSkeleton from "./ProductDetailSkeleton";
import { formatPrice } from "@/shared/lib/format";

/* ── Animation variants ────────────────────────────────────────── */

const staggerContainer = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.1 } },
};

const staggerItem = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.3 } },
};

const slideTransition = { duration: 0.35, ease: [0.32, 0.72, 0, 1] as const };

/* ── Swipe hook ────────────────────────────────────────────────── */

function useSwipe(onSwipeLeft: () => void, onSwipeRight: () => void) {
  const startX = useRef(0);
  const endX = useRef(0);

  const onTouchStart = useCallback((e: React.TouchEvent) => {
    startX.current = e.targetTouches[0].clientX;
    endX.current = e.targetTouches[0].clientX;
  }, []);

  const onTouchMove = useCallback((e: React.TouchEvent) => {
    endX.current = e.targetTouches[0].clientX;
  }, []);

  const onTouchEnd = useCallback(() => {
    const diff = startX.current - endX.current;
    if (Math.abs(diff) > 50) {
      diff > 0 ? onSwipeLeft() : onSwipeRight();
    }
  }, [onSwipeLeft, onSwipeRight]);

  return { onTouchStart, onTouchMove, onTouchEnd };
}

/* ── Main component ────────────────────────────────────────────── */

interface ProductDetailProps {
  slug: string;
}

export default function ProductDetail({ slug }: ProductDetailProps) {
  const t = useTranslations("productDetail");
  const tc = useTranslations("common");
  const [selectedImageIdx, setSelectedImageIdx] = useState(0);
  const [selectedSku, setSelectedSku] = useState<Sku | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const [specsExpanded, setSpecsExpanded] = useState(false);
  const [codeCopied, setCodeCopied] = useState(false);

  const addToCart = useAddToCart();

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

  /* ── Image navigation ───────────────────────────────────────── */

  const imageCount = listing?.images.length ?? 0;

  const goToImage = useCallback(
    (dir: 1 | -1) => {
      if (imageCount <= 0) return;
      setSelectedImageIdx((prev) => (prev + dir + imageCount) % imageCount);
    },
    [imageCount],
  );

  const nextImage = useCallback(() => goToImage(1), [goToImage]);
  const prevImage = useCallback(() => goToImage(-1), [goToImage]);

  const gallerySwipe = useSwipe(nextImage, prevImage);
  const lightboxSwipe = useSwipe(nextImage, prevImage);

  /* ── Keyboard navigation for lightbox ────────────────────────── */

  useEffect(() => {
    if (!lightboxOpen) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === "ArrowRight") nextImage();
      if (e.key === "ArrowLeft") prevImage();
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [lightboxOpen, nextImage, prevImage]);

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

  const mainImage =
    listing.images[selectedImageIdx] ?? listing.images[0] ?? null;

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
      {/* ── Breadcrumb ──────────────────────────────────────────── */}
      <Breadcrumb className="mb-4 sm:mb-6">
        <BreadcrumbList className="text-xs sm:text-sm">
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href="/">{t("home")}</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href="/products">{t("products")}</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          {listing.categoryName && (
            <>
              <BreadcrumbSeparator />
              <BreadcrumbItem>
                <BreadcrumbLink asChild>
                  <Link href={`/products?category=${listing.categoryName}`}>
                    {listing.categoryName}
                  </Link>
                </BreadcrumbLink>
              </BreadcrumbItem>
            </>
          )}
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage className="line-clamp-1">
              {productName}
            </BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-10">
        {/* ══════════════════════════════════════════════════════════
            LEFT — Image gallery
           ══════════════════════════════════════════════════════════ */}
        <div className="lg:col-span-7">
          <div className="flex gap-3">
            {/* Vertical thumbnail strip — desktop only */}
            {imageCount > 1 && (
              <div className="hidden lg:flex flex-col gap-2 w-[72px] shrink-0 max-h-[600px] overflow-y-auto scrollbar-thin">
                {listing.images.map((url, idx) => (
                  <button
                    key={url}
                    onMouseEnter={() => setSelectedImageIdx(idx)}
                    onClick={() => setSelectedImageIdx(idx)}
                    className={`relative w-[72px] h-[88px] rounded-lg overflow-hidden border-2 shrink-0 transition-all ${
                      idx === selectedImageIdx
                        ? "border-primary ring-1 ring-primary/20"
                        : "border-transparent hover:border-muted-foreground/30"
                    }`}
                  >
                    <Image
                      src={url}
                      alt={`${productName} — thumbnail ${idx + 1}`}
                      fill
                      className="object-cover"
                      unoptimized
                    />
                  </button>
                ))}
              </div>
            )}

            {/* Main image */}
            <div className="flex-1 min-w-0">
              <div
                className="relative w-full aspect-[3/4] sm:aspect-[4/5] rounded-xl bg-muted overflow-hidden cursor-zoom-in group"
                onClick={() => mainImage && setLightboxOpen(true)}
                {...gallerySwipe}
              >
                {listing.images.length > 0 ? (
                  listing.images.map((url, idx) => (
                    <motion.div
                      key={url}
                      className="absolute inset-0"
                      animate={{ x: `${(idx - selectedImageIdx) * 100}%` }}
                      transition={slideTransition}
                    >
                      <Image
                        src={url}
                        alt={`${productName} — image ${idx + 1}`}
                        fill
                        className="object-cover"
                        unoptimized
                      />
                    </motion.div>
                  ))
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <span className="text-muted-foreground">
                      {t("noImage")}
                    </span>
                  </div>
                )}

                {/* Tier price badge on image */}
                {hasLoyalty && (
                  <div className="absolute top-2 sm:top-3 right-2 sm:right-3 z-10">
                    <span className="inline-flex items-center gap-0.5 rounded-full bg-amber-500 px-1.5 sm:px-2 py-0.5 text-[10px] sm:text-xs font-bold text-white shadow-md">
                      <Crown className="h-2.5 w-2.5 sm:h-3 sm:w-3" />
                      {tc("yourPrice")}
                    </span>
                  </div>
                )}

                {/* Zoom hint */}
                {mainImage && (
                  <div className="absolute inset-0 flex items-center justify-center bg-black/0 group-hover:bg-black/5 transition-colors pointer-events-none">
                    <ZoomIn className="w-8 h-8 text-white drop-shadow-lg opacity-0 group-hover:opacity-60 transition-opacity" />
                  </div>
                )}

                {/* Image counter pill */}
                {imageCount > 1 && (
                  <div className="absolute bottom-3 right-3 bg-black/60 text-white text-xs px-2.5 py-1 rounded-full pointer-events-none">
                    {selectedImageIdx + 1} / {imageCount}
                  </div>
                )}
              </div>

              {/* Dot indicators — mobile only */}
              {imageCount > 1 && (
                <div className="flex lg:hidden justify-center gap-2 mt-3">
                  {listing.images.map((_, idx) => (
                    <button
                      key={idx}
                      onClick={() => setSelectedImageIdx(idx)}
                      className={`w-2 h-2 rounded-full transition-colors ${
                        idx === selectedImageIdx
                          ? "bg-primary"
                          : "bg-muted-foreground/30"
                      }`}
                      aria-label={`Go to image ${idx + 1}`}
                    />
                  ))}
                </div>
              )}

              {/* Horizontal thumbnail strip — tablet (sm-lg) */}
              {imageCount > 1 && (
                <div className="hidden sm:flex lg:hidden gap-2 mt-3 overflow-x-auto pb-1">
                  {listing.images.map((url, idx) => (
                    <button
                      key={url}
                      onClick={() => setSelectedImageIdx(idx)}
                      className={`relative w-16 h-20 rounded-lg border-2 overflow-hidden shrink-0 transition-colors ${
                        idx === selectedImageIdx
                          ? "border-primary ring-1 ring-primary/20"
                          : "border-transparent hover:border-muted-foreground/30"
                      }`}
                    >
                      <Image
                        src={url}
                        alt={`${productName} — thumbnail ${idx + 1}`}
                        fill
                        className="object-cover"
                        unoptimized
                      />
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* ══════════════════════════════════════════════════════════
            RIGHT — Product info (5 cols)
           ══════════════════════════════════════════════════════════ */}
        <div className="lg:col-span-5 space-y-5">
          {/* Category / Brand line */}
          {listing.categoryName && (
            <Link
              href={`/products?category=${listing.categoryName}`}
              className="text-sm text-primary hover:underline font-medium"
            >
              {listing.categoryName}
            </Link>
          )}

          {/* Product name + badges */}
          <div>
            <h1 className="text-xl sm:text-2xl lg:text-[1.65rem] font-semibold text-foreground leading-snug">
              {productName}
            </h1>

            {/* Status badges */}
            {(listing.topSelling || listing.featured) && (
              <div className="flex items-center gap-2 mt-2">
                {listing.topSelling && (
                  <Badge
                    variant="secondary"
                    className="bg-orange-50 text-orange-700 border-orange-200 text-xs gap-1"
                  >
                    <Flame className="w-3 h-3" />
                    {t("bestseller")}
                  </Badge>
                )}
                {listing.featured && (
                  <Badge
                    variant="secondary"
                    className="bg-blue-50 text-blue-700 border-blue-200 text-xs gap-1"
                  >
                    <Star className="w-3 h-3" />
                    {t("featured")}
                  </Badge>
                )}
              </div>
            )}
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
                      style={{ backgroundColor: activeDiscountHex ?? "#ef4444" }}
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
                    style={{ backgroundColor: activeDiscountHex ?? "#ef4444" }}
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
                    style={{ backgroundColor: activeDiscountHex ?? "#ef4444" }}
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
                          style={{ backgroundColor: skuItem.discountColorHex ?? "#ef4444" }}
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
                        toast.success(t("addedToCart"));
                        window.dispatchEvent(new CustomEvent("cart:open"));
                      },
                      onError: () => {
                        toast.error(t("outOfStockError"));
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
                      {attr.value}
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

      {/* ── Image lightbox (fullscreen modal) ─────────────────────── */}
      <Dialog open={lightboxOpen} onOpenChange={setLightboxOpen}>
        <DialogContent
          className="max-w-none w-screen h-screen p-0 border-0 bg-black/95 rounded-none
            left-0 top-0 translate-x-0 translate-y-0
            data-[state=open]:slide-in-from-left-0 data-[state=open]:slide-in-from-top-0
            data-[state=closed]:slide-out-to-left-0 data-[state=closed]:slide-out-to-top-0
            [&>button]:text-white [&>button]:opacity-100 [&>button>svg]:h-6 [&>button>svg]:w-6"
        >
          <DialogTitle className="sr-only">
            {productName} — Image {selectedImageIdx + 1} of {imageCount}
          </DialogTitle>

          <div
            className="relative w-full h-full"
            {...lightboxSwipe}
          >
            {/* Navigation arrows */}
            {imageCount > 1 && (
              <button
                onClick={prevImage}
                className="absolute left-2 sm:left-4 top-1/2 -translate-y-1/2 z-10
                  w-10 h-10 rounded-full bg-white/10 hover:bg-white/20
                  flex items-center justify-center text-white transition-colors"
                aria-label="Previous image"
              >
                <ChevronLeft className="w-6 h-6" />
              </button>
            )}

            {imageCount > 1 && (
              <button
                onClick={nextImage}
                className="absolute right-2 sm:right-4 top-1/2 -translate-y-1/2 z-10
                  w-10 h-10 rounded-full bg-white/10 hover:bg-white/20
                  flex items-center justify-center text-white transition-colors"
                aria-label="Next image"
              >
                <ChevronRight className="w-6 h-6" />
              </button>
            )}

            {/* Main image area */}
            <div className="absolute inset-0 flex items-center justify-center px-14 pt-12 pb-28 overflow-hidden">
              <div className="relative w-full h-full">
                {listing.images.map((url, idx) => (
                  <motion.div
                    key={url}
                    className="absolute inset-0"
                    animate={{ x: `${(idx - selectedImageIdx) * 100}%` }}
                    transition={slideTransition}
                  >
                    <Image
                      src={url}
                      alt={`${productName} — image ${idx + 1}`}
                      fill
                      className="object-contain"
                      unoptimized
                    />
                  </motion.div>
                ))}
              </div>
            </div>

            {/* Bottom controls */}
            <div className="absolute bottom-4 left-0 right-0 flex flex-col items-center gap-3">
              {/* Thumbnails — sm+ screens */}
              {imageCount > 1 && (
                <div className="hidden sm:flex gap-2 overflow-x-auto max-w-[80vw] pb-1">
                  {listing.images.map((url, idx) => (
                    <button
                      key={url}
                      onClick={() => setSelectedImageIdx(idx)}
                      className={`relative w-12 h-12 rounded-md overflow-hidden border-2 shrink-0 transition-all ${
                        idx === selectedImageIdx
                          ? "border-white opacity-100"
                          : "border-transparent opacity-50 hover:opacity-80"
                      }`}
                    >
                      <Image
                        src={url}
                        alt={`Thumbnail ${idx + 1}`}
                        fill
                        className="object-cover"
                        unoptimized
                      />
                    </button>
                  ))}
                </div>
              )}

              {/* Counter */}
              {imageCount > 1 && (
                <div className="bg-black/60 text-white text-sm px-4 py-1.5 rounded-full">
                  {selectedImageIdx + 1} / {imageCount}
                </div>
              )}
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </motion.div>
  );
}
