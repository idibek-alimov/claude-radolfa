"use client";

import Image from "next/image";
import { useCallback, useEffect, useRef, useState } from "react";
import { motion } from "framer-motion";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogTitle,
} from "@radolfa/shared/ui/dialog";
import { useSwipe } from "../lib/useSwipe";

const slideTransition = { duration: 0.35, ease: [0.32, 0.72, 0, 1] as const };

const MAX_THUMBS = 5;

interface ProductGalleryProps {
  images: string[];
  productName: string;
  discountPercentage: number | null;
}

export default function ProductGallery({
  images,
  productName,
  discountPercentage,
}: ProductGalleryProps) {
  const t = useTranslations("productDetail");
  const [selectedImageIdx, setSelectedImageIdx] = useState(0);
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const mobileTrackRef = useRef<HTMLDivElement>(null);

  const imageCount = images.length;

  /* ── Image navigation ───────────────────────────────────────── */

  const goToImage = useCallback(
    (dir: 1 | -1) => {
      if (imageCount <= 0) return;
      setSelectedImageIdx((prev) => (prev + dir + imageCount) % imageCount);
    },
    [imageCount],
  );

  const nextImage = useCallback(() => goToImage(1), [goToImage]);
  const prevImage = useCallback(() => goToImage(-1), [goToImage]);

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

  /* ── Mobile carousel scroll tracking ──────────────────────────── */

  const handleMobileScroll = useCallback(() => {
    const el = mobileTrackRef.current;
    if (!el || el.clientWidth === 0) return;
    const idx = Math.round(el.scrollLeft / el.clientWidth);
    setSelectedImageIdx((prev) => (prev === idx ? prev : idx));
  }, []);

  const goToMobileSlide = useCallback((idx: number) => {
    const el = mobileTrackRef.current;
    if (!el) return;
    el.scrollTo({ left: idx * el.clientWidth, behavior: "smooth" });
    setSelectedImageIdx(idx);
  }, []);

  const overflowCount = imageCount - MAX_THUMBS;
  const thumbImages = images.slice(0, MAX_THUMBS);

  return (
    <>
      {/* ══════════════════════════════════════════════════════════
          DESKTOP — vertical thumb strip + main
         ══════════════════════════════════════════════════════════ */}
      <div className="hidden lg:grid grid-cols-[80px_1fr] gap-3">
        {imageCount > 0 ? (
          <div className="flex flex-col gap-2">
            {thumbImages.map((url, idx) => (
              <button
                key={url}
                onClick={() => setSelectedImageIdx(idx)}
                className={`aspect-square rounded-xl overflow-hidden relative ${
                  idx === selectedImageIdx
                    ? "border-2 border-mag"
                    : "border border-ink/15 hover:border-mag"
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
            {overflowCount > 0 && (
              <button
                onClick={() => setLightboxOpen(true)}
                className="aspect-square rounded-xl border border-dashed border-ink/25 text-[11px] text-ink/60 font-bold"
              >
                +{overflowCount}
              </button>
            )}
          </div>
        ) : (
          <div />
        )}

        {/* Main image */}
        <div
          className="aspect-square bg-[#F7F3E8] rounded-3xl overflow-hidden relative cursor-zoom-in"
          onClick={() => imageCount > 0 && setLightboxOpen(true)}
        >
          {imageCount > 0 ? (
            images.map((url, idx) => (
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
              <span className="text-ink/50">{t("noImage")}</span>
            </div>
          )}

          {/* Discount badge */}
          {discountPercentage != null && (
            <span className="absolute top-5 left-5 px-3 py-1 rounded-full bg-sale text-white text-[13px] font-bold">
              −{discountPercentage}%
            </span>
          )}

          {/* Dot indicators */}
          {imageCount > 1 && (
            <div className="absolute bottom-5 left-1/2 -translate-x-1/2 flex gap-1.5">
              {images.map((_, idx) => (
                <span
                  key={idx}
                  className={`rounded-full transition-all ${
                    idx === selectedImageIdx
                      ? "w-7 h-1.5 bg-mag"
                      : "w-1.5 h-1.5 bg-ink/30"
                  }`}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* ══════════════════════════════════════════════════════════
          MOBILE — swipe carousel
         ══════════════════════════════════════════════════════════ */}
      <section className="relative lg:hidden">
        {imageCount > 0 ? (
          <div
            ref={mobileTrackRef}
            onScroll={handleMobileScroll}
            className="flex overflow-x-auto snap-x scrollbar-hide"
          >
            {images.map((url, idx) => (
              <div
                key={url}
                className="snap-start shrink-0 w-full aspect-square bg-[#F7F3E8]"
              >
                <Image
                  src={url}
                  alt={`${productName} — image ${idx + 1}`}
                  width={800}
                  height={800}
                  className="w-full h-full object-cover"
                  unoptimized
                />
              </div>
            ))}
          </div>
        ) : (
          <div className="w-full aspect-square bg-[#F7F3E8] flex items-center justify-center">
            <span className="text-ink/50">{t("noImage")}</span>
          </div>
        )}

        {/* Discount badge */}
        {discountPercentage != null && (
          <span className="absolute bottom-4 left-3 px-2.5 py-1 rounded-full bg-sale text-white text-[12px] font-bold">
            −{discountPercentage}%
          </span>
        )}

        {/* Dot indicators */}
        {imageCount > 1 && (
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-1.5">
            {images.map((_, idx) => (
              <button
                key={idx}
                onClick={() => goToMobileSlide(idx)}
                aria-label={`Go to image ${idx + 1}`}
                className={`rounded-full transition-all ${
                  idx === selectedImageIdx
                    ? "w-6 h-1.5 bg-mag"
                    : "w-1.5 h-1.5 bg-white/70"
                }`}
              />
            ))}
          </div>
        )}
      </section>

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

          <div className="relative w-full h-full" {...lightboxSwipe}>
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
                {images.map((url, idx) => (
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
                  {images.map((url, idx) => (
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
    </>
  );
}
