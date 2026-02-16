"use client";

import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { useState, useCallback, useRef, useMemo, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { ChevronLeft, ChevronRight, ZoomIn } from "lucide-react";
import { fetchListingBySlug, fetchListings } from "@/entities/product/api";
import type { Sku } from "@/entities/product";
import { Badge } from "@/shared/ui/badge";
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
  const [selectedImageIdx, setSelectedImageIdx] = useState(0);
  const [selectedSku, setSelectedSku] = useState<Sku | null>(null);
  const [lightboxOpen, setLightboxOpen] = useState(false);

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

  /* ── Stable colour swatches (fixed order) ────────────────────── */

  const allSwatches = useMemo(() => {
    if (!listing) return [];
    const current = {
      slug: listing.slug,
      colorKey: listing.colorKey,
      thumbnail: listing.images[0] ?? "",
      isCurrent: true,
    };
    const siblings = (listing.siblingVariants ?? []).map((s) => ({
      slug: s.slug,
      colorKey: s.colorKey,
      thumbnail: s.thumbnail,
      isCurrent: false,
    }));
    return [current, ...siblings].sort((a, b) =>
      a.colorKey.localeCompare(b.colorKey),
    );
  }, [listing]);

  /* ── Related products (exclude current) ─────────────────────── */

  const relatedProducts = useMemo(() => {
    if (!relatedData) return [];
    return relatedData.items.filter((item) => item.slug !== slug).slice(0, 4);
  }, [relatedData, slug]);

  /* ── Loading / Error ─────────────────────────────────────────── */

  if (isLoading) return <ProductDetailSkeleton />;

  if (isError || !listing) {
    notFound();
  }

  const mainImage =
    listing.images[selectedImageIdx] ?? listing.images[0] ?? null;

  /* ── Render ──────────────────────────────────────────────────── */

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
      className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
    >
      {/* ── Breadcrumb ──────────────────────────────────────────── */}
      <Breadcrumb className="mb-8">
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href="/">Home</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href="/products">Products</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage>{listing.name ?? slug}</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-10">
        {/* ── Left — Image gallery (60%) ────────────────────────── */}
        <div className="lg:col-span-3">
          {/* Main image — clickable to open lightbox, swipeable */}
          <div
            className="relative w-full aspect-square rounded-xl border bg-muted overflow-hidden cursor-zoom-in group"
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
                    alt={`${listing.name ?? "Product"} — image ${idx + 1}`}
                    fill
                    className="object-cover"
                    unoptimized
                  />
                </motion.div>
              ))
            ) : (
              <div className="w-full h-full flex items-center justify-center">
                <span className="text-muted-foreground">
                  No image available
                </span>
              </div>
            )}

            {/* Zoom hint overlay */}
            {mainImage && (
              <div className="absolute inset-0 flex items-center justify-center bg-black/0 group-hover:bg-black/5 transition-colors pointer-events-none">
                <ZoomIn className="w-8 h-8 text-white drop-shadow-lg opacity-0 group-hover:opacity-70 transition-opacity" />
              </div>
            )}

            {/* Image counter pill */}
            {imageCount > 1 && (
              <div className="absolute bottom-3 right-3 bg-black/60 text-white text-xs px-2.5 py-1 rounded-full pointer-events-none">
                {selectedImageIdx + 1} / {imageCount}
              </div>
            )}
          </div>

          {/* Dot indicators — small screens only */}
          {imageCount > 1 && (
            <div className="flex sm:hidden justify-center gap-2 mt-3">
              {listing.images.map((_, idx) => (
                <button
                  key={idx}
                  onClick={() => setSelectedImageIdx(idx)}
                  className={`w-2.5 h-2.5 rounded-full transition-colors ${
                    idx === selectedImageIdx
                      ? "bg-primary"
                      : "bg-muted-foreground/30"
                  }`}
                  aria-label={`Go to image ${idx + 1}`}
                />
              ))}
            </div>
          )}

          {/* Thumbnail strip — sm+ screens */}
          {imageCount > 1 && (
            <div className="hidden sm:flex gap-3 mt-4 overflow-x-auto pb-2">
              {listing.images.map((url, idx) => (
                <motion.button
                  key={url}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setSelectedImageIdx(idx)}
                  className={`relative w-20 h-20 rounded-lg border-2 overflow-hidden shrink-0 transition-colors ${
                    idx === selectedImageIdx
                      ? "border-primary ring-2 ring-primary/20"
                      : "border-transparent hover:border-muted-foreground/30"
                  }`}
                >
                  <Image
                    src={url}
                    alt={`${listing.name ?? "Product"} — thumbnail ${idx + 1}`}
                    fill
                    className="object-cover"
                    unoptimized
                  />
                </motion.button>
              ))}
            </div>
          )}
        </div>

        {/* ── Right — Product info (40%) ─────────────────────────── */}
        <div className="lg:col-span-2 space-y-6">
          {/* Product name */}
          <h1 className="text-3xl lg:text-4xl font-bold text-foreground leading-tight">
            {listing.name ?? "—"}
          </h1>

          {/* Price — always shows listing-level price */}
          <div className="space-y-1.5">
            {listing.priceStart === listing.priceEnd ? (
              <p className="text-4xl font-bold text-primary">
                {formatPrice(listing.priceStart)}
              </p>
            ) : (
              <>
                <p className="text-4xl font-bold text-primary">
                  From {formatPrice(listing.priceStart)}
                </p>
                <p className="text-sm text-muted-foreground">
                  {formatPrice(listing.priceStart)} –{" "}
                  {formatPrice(listing.priceEnd)} depending on size
                </p>
              </>
            )}
          </div>

          {/* Stock indicator */}
          <div className="flex items-center gap-3">
            <StockBadge
              stock={
                selectedSku ? selectedSku.stockQuantity : listing.totalStock
              }
            />
            {listing.colorKey && (
              <Badge variant="secondary">{listing.colorKey}</Badge>
            )}
          </div>

          {/* ── Colour swatches (stable order — no reordering) ──── */}
          {allSwatches.length > 1 && (
            <div className="pt-5 border-t">
              <h2 className="text-sm font-medium text-muted-foreground mb-3">
                Available Colours
              </h2>
              <div className="flex gap-2 flex-wrap">
                {allSwatches.map((swatch) => {
                  const thumb = swatch.thumbnail ? (
                    <Image
                      src={swatch.thumbnail}
                      alt={swatch.colorKey}
                      fill
                      className="object-cover"
                      unoptimized
                    />
                  ) : (
                    <div className="w-full h-full bg-muted flex items-center justify-center text-xs">
                      {swatch.colorKey}
                    </div>
                  );

                  if (swatch.isCurrent) {
                    return (
                      <motion.div
                        key={swatch.slug}
                        whileHover={{ scale: 1.08 }}
                        className="relative w-12 h-12 rounded-lg overflow-hidden ring-2 ring-primary ring-offset-2"
                      >
                        {thumb}
                      </motion.div>
                    );
                  }

                  return (
                    <Link
                      key={swatch.slug}
                      href={`/products/${swatch.slug}`}
                    >
                      <motion.div
                        whileHover={{ scale: 1.08 }}
                        whileTap={{ scale: 0.95 }}
                        className="relative w-12 h-12 rounded-lg border-2 border-transparent hover:border-muted-foreground/50 overflow-hidden transition-colors"
                      >
                        {thumb}
                      </motion.div>
                    </Link>
                  );
                })}
              </div>
            </div>
          )}

          {/* ── Size selector (display only — no cart) ──────────── */}
          {listing.skus && listing.skus.length > 0 && (
            <div className="pt-5 border-t">
              <h2 className="text-sm font-medium text-muted-foreground mb-3">
                Available Sizes
              </h2>
              <div className="flex gap-2 flex-wrap">
                {listing.skus.map((sku) => {
                  const isOutOfStock = sku.stockQuantity === 0;
                  const isSelected = selectedSku?.id === sku.id;

                  return (
                    <motion.button
                      key={sku.id}
                      whileHover={!isOutOfStock ? { scale: 1.05 } : undefined}
                      whileTap={!isOutOfStock ? { scale: 0.95 } : undefined}
                      onClick={() =>
                        !isOutOfStock &&
                        setSelectedSku(isSelected ? null : sku)
                      }
                      disabled={isOutOfStock}
                      className={`px-4 py-2 rounded-full border-2 text-sm font-medium transition-all ${
                        isSelected
                          ? "border-primary bg-primary text-primary-foreground shadow-md"
                          : isOutOfStock
                            ? "border-muted bg-muted text-muted-foreground cursor-not-allowed line-through opacity-50"
                            : "border-input hover:border-primary/50 bg-background hover:shadow-sm"
                      }`}
                    >
                      {sku.sizeLabel}
                      {!isOutOfStock && (
                        <span className="ml-1.5 text-xs opacity-60">
                          ({sku.stockQuantity})
                        </span>
                      )}
                    </motion.button>
                  );
                })}
              </div>
            </div>
          )}

          {/* ── Description ──────────────────────────────────────── */}
          {listing.webDescription && (
            <div className="pt-5 border-t">
              <h2 className="text-sm font-medium text-muted-foreground mb-2">
                Description
              </h2>
              <p className="text-foreground leading-relaxed">
                {listing.webDescription}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* ── Related products — "You May Also Like" ────────────────── */}
      <div className="mt-16 pt-16 border-t">
        <h2 className="text-2xl font-bold text-foreground mb-8">
          You May Also Like
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
            {listing.name} — Image {selectedImageIdx + 1} of {imageCount}
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
                      alt={`${listing.name} — image ${idx + 1}`}
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
