"use client";

import { useState, useEffect, useCallback } from "react";
import Image from "next/image";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import { useTranslations } from "next-intl";

interface ReviewPhotoStripProps {
  photoUrls: string[];
  totalCount?: number;
  onSeeAll?: () => void;
}

export function ReviewPhotoStrip({ photoUrls, totalCount, onSeeAll }: ReviewPhotoStripProps) {
  const t = useTranslations("reviews.strip");
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);

  const isOpen = lightboxIndex !== null;
  const count = photoUrls.length;

  const close = useCallback(() => setLightboxIndex(null), []);

  const prev = useCallback(() => {
    setLightboxIndex((i) => (i !== null ? (i - 1 + count) % count : null));
  }, [count]);

  const next = useCallback(() => {
    setLightboxIndex((i) => (i !== null ? (i + 1) % count : null));
  }, [count]);

  useEffect(() => {
    if (!isOpen) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") close();
      if (e.key === "ArrowLeft") prev();
      if (e.key === "ArrowRight") next();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [isOpen, close, prev, next]);

  if (count === 0) return null;

  return (
    <>
      {/* Strip */}
      <div className="space-y-2">
        {/* Header row */}
        <div className="flex items-center justify-between">
          <span className="text-sm font-semibold">{t("photosAndVideos")}</span>
          {onSeeAll && totalCount !== undefined && (
            <button
              onClick={onSeeAll}
              className="text-sm text-primary hover:underline flex items-center gap-0.5"
            >
              {t("seeAll", { n: totalCount })} ›
            </button>
          )}
        </div>

        {/* Scrollable thumbnails */}
        <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
          {photoUrls.map((url, i) => (
            <button
              key={i}
              onClick={() => setLightboxIndex(i)}
              className="relative h-20 w-20 shrink-0 overflow-hidden rounded-lg border hover:opacity-90 transition-opacity"
            >
              <Image
                src={url}
                alt={`Review photo ${i + 1}`}
                fill
                className="object-cover"
                unoptimized
              />
            </button>
          ))}
        </div>
      </div>

      {/* Lightbox */}
      {isOpen && lightboxIndex !== null && (
        <div
          className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center"
          onClick={close}
        >
          {/* Close button */}
          <button
            onClick={close}
            className="absolute top-4 right-4 text-white/80 hover:text-white transition-colors"
            aria-label="Close"
          >
            <X className="h-6 w-6" />
          </button>

          {/* Prev arrow */}
          {count > 1 && (
            <button
              onClick={(e) => { e.stopPropagation(); prev(); }}
              className="absolute left-4 text-white/80 hover:text-white transition-colors p-2"
              aria-label="Previous"
            >
              <ChevronLeft className="h-8 w-8" />
            </button>
          )}

          {/* Image */}
          <div
            className="relative max-w-[90vw] max-h-[90vh] w-full h-full flex items-center justify-center"
            onClick={(e) => e.stopPropagation()}
          >
            <Image
              src={photoUrls[lightboxIndex]}
              alt={`Review photo ${lightboxIndex + 1}`}
              width={900}
              height={900}
              className="object-contain max-w-[90vw] max-h-[90vh] rounded-lg"
              unoptimized
            />
          </div>

          {/* Next arrow */}
          {count > 1 && (
            <button
              onClick={(e) => { e.stopPropagation(); next(); }}
              className="absolute right-4 text-white/80 hover:text-white transition-colors p-2"
              aria-label="Next"
            >
              <ChevronRight className="h-8 w-8" />
            </button>
          )}

          {/* Counter */}
          {count > 1 && (
            <span className="absolute bottom-4 left-1/2 -translate-x-1/2 text-sm text-white/70">
              {lightboxIndex + 1} / {count}
            </span>
          )}
        </div>
      )}
    </>
  );
}
