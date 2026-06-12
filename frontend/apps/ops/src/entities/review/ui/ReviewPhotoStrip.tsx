"use client";

import { useState, useEffect, useCallback } from "react";
import Image from "next/image";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import { useTranslations } from "next-intl";

const MAX_VISIBLE = 10;

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
  const visiblePhotos = photoUrls.slice(0, MAX_VISIBLE);
  const overflow = count - MAX_VISIBLE;

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

        {/* Photo grid */}
        <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-1.5">
          {visiblePhotos.map((url, i) => {
            const isLastSlot = i === MAX_VISIBLE - 1 && overflow > 0;
            return (
              <button
                key={i}
                onClick={() => setLightboxIndex(i)}
                className="relative aspect-square overflow-hidden rounded-md border hover:opacity-85 hover:scale-[0.97] transition-all"
              >
                <Image
                  src={url}
                  alt={`Review photo ${i + 1}`}
                  fill
                  className="object-cover"
                  unoptimized
                />
                {isLastSlot && (
                  <div className="absolute inset-0 bg-black/50 flex items-center justify-center rounded-md">
                    <span className="text-white text-sm font-semibold">+{overflow}</span>
                  </div>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Lightbox */}
      {isOpen && lightboxIndex !== null && (
        <div
          className="fixed inset-0 z-50 bg-black/90 flex items-center justify-center"
          onClick={close}
        >
          {/* Counter — top center */}
          {count > 1 && (
            <span className="absolute top-4 left-1/2 -translate-x-1/2 bg-black/50 rounded-full px-3 py-1 text-sm text-white">
              {lightboxIndex + 1} / {count}
            </span>
          )}

          {/* Close button */}
          <button
            onClick={close}
            className="absolute top-4 right-4 bg-black/30 hover:bg-black/60 rounded-full p-2 text-white transition-colors"
            aria-label="Close"
          >
            <X className="h-5 w-5" />
          </button>

          {/* Prev arrow */}
          {count > 1 && (
            <button
              onClick={(e) => { e.stopPropagation(); prev(); }}
              className="absolute left-4 bg-black/30 hover:bg-black/60 rounded-full p-2 text-white transition-colors"
              aria-label="Previous"
            >
              <ChevronLeft className="h-6 w-6" />
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
              className="absolute right-4 bg-black/30 hover:bg-black/60 rounded-full p-2 text-white transition-colors"
              aria-label="Next"
            >
              <ChevronRight className="h-6 w-6" />
            </button>
          )}
        </div>
      )}
    </>
  );
}
