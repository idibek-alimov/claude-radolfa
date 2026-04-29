"use client";

import { useState } from "react";
import Image from "next/image";
import { useTranslations } from "next-intl";
import { ThumbsUp, ThumbsDown } from "lucide-react";
import { StarRating } from "@/shared/ui/StarRating";
import { Dialog, DialogContent, DialogTitle } from "@/shared/ui/dialog";
import type { StorefrontReview, MatchingSize } from "../model/types";

const MAX_VISIBLE_PHOTOS = 4;

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0][0]?.toUpperCase() ?? "";
  return ((parts[0][0] ?? "") + (parts[1][0] ?? "")).toUpperCase();
}

function extractTags(pros: string | null | undefined): string[] {
  if (!pros) return [];
  const items = pros
    .split(/[,\n]/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0 && s.length < 35);
  return items.length >= 2 ? items.slice(0, 4) : [];
}

interface ReviewCardProps {
  review: StorefrontReview;
  showSellerReply?: boolean;
  /** "card" (default) — bordered card used in the preview carousel.
   *  "list" — borderless list item used in the full reviews page. */
  variant?: "card" | "list";
}

export function ReviewCard({ review, showSellerReply = false, variant = "card" }: ReviewCardProps) {
  const t = useTranslations("reviews.card");
  const tSummary = useTranslations("reviews.summary");
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);

  const visiblePhotos = review.photoUrls.slice(0, MAX_VISIBLE_PHOTOS);
  const extraCount = review.photoUrls.length - MAX_VISIBLE_PHOTOS;
  const hasStructuredBody = !!(review.pros || review.cons);
  const tags = extractTags(review.pros);
  const showProsAsText = review.pros && tags.length === 0;

  const sizeFitLabel: Record<MatchingSize, string> = {
    RUNS_SMALL: tSummary("runsSmall"),
    ACCURATE: tSummary("trueToSize"),
    RUNS_LARGE: tSummary("runsLarge"),
  };

  const containerClass =
    variant === "card"
      ? "rounded-xl border bg-card p-4 space-y-3"
      : "py-5 space-y-3 border-b last:border-b-0";

  return (
    <div className={containerClass}>
      {/* Header: avatar + name/badges | stars + date */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-2">
        <div className="flex items-start gap-2.5 min-w-0">
          {/* Avatar */}
          <div className="w-9 h-9 rounded-full bg-primary/10 text-primary text-sm font-semibold flex items-center justify-center shrink-0 mt-0.5">
            {getInitials(review.authorName)}
          </div>
          {/* Name + badges */}
          <div className="min-w-0 space-y-1">
            <p className={`font-semibold leading-tight truncate ${variant === "list" ? "text-base" : "text-sm"}`}>
              {review.authorName}
            </p>
            <div className="flex flex-wrap gap-1">
              <span className="inline-flex items-center gap-1 text-xs font-medium text-green-600">
                ✓ {t("verifiedPurchase")}
              </span>
              {review.matchingSize && (
                <span className="inline-flex items-center rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                  {sizeFitLabel[review.matchingSize]}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Stars + date */}
        <div className="flex sm:flex-col items-center sm:items-end gap-2 sm:gap-0.5 shrink-0">
          <StarRating rating={review.rating} size="sm" />
          <span className="text-xs text-muted-foreground">
            {new Date(review.createdAt).toLocaleDateString()}
          </span>
        </div>
      </div>

      {/* Characteristic tags — derived from pros when comma/newline separated */}
      {tags.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {tags.map((tag, i) => (
            <span
              key={i}
              className="text-xs bg-muted px-2.5 py-0.5 rounded-full text-muted-foreground"
            >
              {tag}
            </span>
          ))}
        </div>
      )}

      {/* Review title */}
      {review.title && (
        <p className="text-sm font-semibold">{review.title}</p>
      )}

      {/* Body: pros / cons / comment */}
      <div className="space-y-1 text-sm">
        {showProsAsText && (
          <p>
            <span className="font-medium text-muted-foreground">{t("pros")}: </span>
            {review.pros}
          </p>
        )}
        {review.cons && (
          <p>
            <span className="font-medium text-muted-foreground">{t("cons")}: </span>
            {review.cons}
          </p>
        )}
        {review.body && (
          <p>
            {hasStructuredBody && (
              <span className="font-medium text-muted-foreground">{t("comment")}: </span>
            )}
            {review.body}
          </p>
        )}
      </div>

      {/* Photo thumbnails — max 4, overflow chip */}
      {visiblePhotos.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {visiblePhotos.map((url, i) => (
            <button
              key={i}
              type="button"
              onClick={() => setLightboxIndex(i)}
              className="relative h-16 w-16 overflow-hidden rounded-lg border shrink-0 cursor-zoom-in"
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
          {extraCount > 0 && (
            <div className="h-16 w-16 rounded-lg border bg-muted flex items-center justify-center text-xs text-muted-foreground font-medium shrink-0">
              +{extraCount} {t("morePhotos")}
            </div>
          )}
        </div>
      )}

      {/* Lightbox */}
      <Dialog
        open={lightboxIndex !== null}
        onOpenChange={(o) => { if (!o) setLightboxIndex(null); }}
      >
        <DialogContent
          className="max-w-none w-screen h-screen p-0 border-0 bg-black/95 rounded-none
            left-0 top-0 translate-x-0 translate-y-0
            [&>button]:text-white [&>button]:opacity-100 [&>button>svg]:h-6 [&>button>svg]:w-6"
        >
          <DialogTitle className="sr-only">{t("imageLightbox")}</DialogTitle>
          {lightboxIndex !== null && (
            <div className="relative w-full h-full flex items-center justify-center">
              <Image
                src={review.photoUrls[lightboxIndex]}
                alt={`Review photo ${lightboxIndex + 1}`}
                fill
                unoptimized
                className="object-contain"
              />
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Seller reply */}
      {showSellerReply && review.sellerReply && (
        <div className="border-l-4 border-primary bg-muted/50 rounded-r-lg px-4 py-3 space-y-1">
          <p className="text-xs font-semibold">{t("sellerReply")}</p>
          <p className="text-sm">{review.sellerReply}</p>
        </div>
      )}

      {/* Helpfulness row — list variant only */}
      {variant === "list" && (
        <div className="flex items-center justify-between pt-2 border-t">
          <div className="flex items-center gap-3 text-xs text-muted-foreground">
            <span>{t("helpful")}</span>
            <button className="flex items-center gap-1 hover:text-foreground transition-colors">
              <ThumbsUp className="h-3.5 w-3.5" />
              <span>{review.helpfulCount}</span>
            </button>
            <button className="flex items-center gap-1 hover:text-foreground transition-colors">
              <ThumbsDown className="h-3.5 w-3.5" />
              <span>{review.notHelpfulCount}</span>
            </button>
          </div>
          <button className="text-xs text-muted-foreground hover:text-foreground transition-colors">
            {t("report")}
          </button>
        </div>
      )}
    </div>
  );
}
