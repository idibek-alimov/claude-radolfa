"use client";

import Image from "next/image";
import { useTranslations } from "next-intl";
import { StarRating } from "@/shared/ui/StarRating";
import type { StorefrontReview, MatchingSize } from "../model/types";

const sizeFitLabel: Record<MatchingSize, string> = {
  RUNS_SMALL: "Runs Small",
  ACCURATE: "True to Size",
  RUNS_LARGE: "Runs Large",
};

function getInitials(name: string): string {
  return name
    .split(" ")
    .map((part) => part[0] ?? "")
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

const MAX_VISIBLE_PHOTOS = 4;

interface ReviewCardProps {
  review: StorefrontReview;
  showSellerReply?: boolean;
}

export function ReviewCard({ review, showSellerReply = false }: ReviewCardProps) {
  const t = useTranslations("reviews.card");

  const visiblePhotos = review.photoUrls.slice(0, MAX_VISIBLE_PHOTOS);
  const extraCount = review.photoUrls.length - MAX_VISIBLE_PHOTOS;
  const hasStructuredBody = !!(review.pros || review.cons);

  return (
    <div className="rounded-xl border bg-card p-4 space-y-3">
      {/* Header: avatar + name + purchased / stars + date */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2.5 min-w-0">
          <div className="h-8 w-8 rounded-full bg-muted flex items-center justify-center text-xs font-semibold text-muted-foreground shrink-0">
            {getInitials(review.authorName)}
          </div>
          <div className="min-w-0">
            <p className="text-sm font-semibold leading-tight truncate">{review.authorName}</p>
            <p className="text-xs text-muted-foreground">✓ {t("purchased")}</p>
          </div>
        </div>
        <div className="flex flex-col items-end shrink-0 gap-0.5">
          <StarRating rating={review.rating} size="sm" />
          <span className="text-xs text-muted-foreground">
            {new Date(review.createdAt).toLocaleDateString()}
          </span>
        </div>
      </div>

      {/* Body: pros / cons / comment */}
      <div className="space-y-1 text-sm">
        {review.pros && (
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

      {/* Size fit badge */}
      {review.matchingSize && (
        <span className="inline-block rounded-full bg-muted px-2.5 py-0.5 text-xs text-muted-foreground">
          {sizeFitLabel[review.matchingSize]}
        </span>
      )}

      {/* Photo thumbnails — max 4, overflow chip */}
      {visiblePhotos.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {visiblePhotos.map((url, i) => (
            <div key={i} className="relative h-16 w-16 overflow-hidden rounded-lg border shrink-0">
              <Image
                src={url}
                alt={`Review photo ${i + 1}`}
                fill
                className="object-cover"
                unoptimized
              />
            </div>
          ))}
          {extraCount > 0 && (
            <div className="h-16 w-16 rounded-lg border bg-muted flex items-center justify-center text-xs text-muted-foreground font-medium shrink-0">
              +{extraCount} {t("morePhotos")}
            </div>
          )}
        </div>
      )}

      {/* Seller reply — only on full reviews page */}
      {showSellerReply && review.sellerReply && (
        <div className="rounded-lg bg-muted p-3 space-y-1">
          <p className="text-xs font-medium text-muted-foreground">{t("sellerReply")}</p>
          <p className="text-sm">{review.sellerReply}</p>
        </div>
      )}
    </div>
  );
}
