"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import Image from "next/image";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { fetchRatingSummary, fetchReviews } from "@/entities/review/api";
import type { StorefrontReview } from "@/entities/review";
import { fetchMyDeliveredOrders } from "@/entities/order";
import { SubmitReviewForm } from "@/features/review-submission/ui/SubmitReviewForm";
import { StarRating } from "@radolfa/shared/ui/StarRating";
import { formatDate } from "@radolfa/shared/lib/format";
import type { ReviewTrait } from "@/entities/review-trait";

const TOP_REVIEWS_COUNT = 2;

interface ReviewsSectionProps {
  slug: string;
  listingVariantId: number;
  isAuthenticated: boolean;
  reviewTraits: ReviewTrait[];
}

export default function ReviewsSection({
  slug,
  listingVariantId,
  isAuthenticated,
  reviewTraits,
}: ReviewsSectionProps) {
  const t = useTranslations("reviews.block");
  const tEmpty = useTranslations("reviews");
  const [writeOpen, setWriteOpen] = useState(false);

  const { data: rating, isLoading: ratingLoading } = useQuery({
    queryKey: ["rating", slug],
    queryFn: () => fetchRatingSummary(slug),
  });

  const reviewCount = rating?.reviewCount ?? 0;

  const { data: topReviews } = useQuery({
    queryKey: ["reviews", slug, 1, "newest", TOP_REVIEWS_COUNT],
    queryFn: () => fetchReviews(slug, 1, TOP_REVIEWS_COUNT, "newest"),
    enabled: reviewCount > 0,
  });

  const { data: deliveredOrders } = useQuery({
    queryKey: ["my-delivered-orders"],
    queryFn: fetchMyDeliveredOrders,
    enabled: isAuthenticated,
  });

  // Find the most-recent unreviewed delivered item for this variant.
  const eligibleReview = useMemo(() => {
    if (!deliveredOrders) return null;
    for (const order of deliveredOrders) {
      const item = order.items.find(
        (i) => i.listingVariantId === listingVariantId && !i.hasReviewed && i.skuId !== null,
      );
      if (item) return { orderId: order.id, skuId: item.skuId! };
    }
    return null;
  }, [deliveredOrders, listingVariantId]);

  const sliderAggregates = (rating?.traitAggregates ?? []).filter((a) => a.inputType === "SLIDER");

  if (ratingLoading) {
    return (
      <section className="max-w-[1440px] mx-auto px-4 sm:px-6 mt-10">
        <div className="h-48 rounded-2xl sm:rounded-3xl bg-soft animate-pulse" />
      </section>
    );
  }

  return (
    <section className="max-w-[1440px] mx-auto px-4 sm:px-6 mt-10">
      <div className="flex items-baseline justify-between mb-3 sm:mb-5">
        <h2 className="font-black text-lg sm:text-3xl">{t("heading", { count: reviewCount })}</h2>
        {eligibleReview && (
          <button
            type="button"
            onClick={() => setWriteOpen(true)}
            className="h-8 sm:h-10 px-4 sm:px-5 rounded-full bg-mag text-white font-bold text-[12px] sm:text-[13px]"
          >
            {t("write")}
          </button>
        )}
      </div>

      {reviewCount === 0 || !rating ? (
        <p className="text-[13px] text-ink/55">{tEmpty("empty")}</p>
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
            {/* ── Summary card ─────────────────────────────────────── */}
            <div className="rounded-2xl sm:rounded-3xl bg-soft p-5 sm:p-7">
              <div className="flex items-baseline gap-3">
                <span className="text-[44px] sm:text-[64px] font-black text-mag tabular-nums leading-none">
                  {rating.averageRating.toFixed(1)}
                </span>
                <div>
                  <StarRating rating={rating.averageRating} size="sm" />
                  <div className="text-[11px] sm:text-[12px] text-ink/55 mt-1">
                    {t("ratings", { count: reviewCount })}
                  </div>
                </div>
              </div>

              <div className="mt-4 sm:mt-5 space-y-1.5 sm:space-y-2 text-[11px] sm:text-[12px]">
                {[5, 4, 3, 2, 1].map((star) => {
                  const count = rating.distribution[star] ?? 0;
                  const pct = reviewCount > 0 ? (count / reviewCount) * 100 : 0;
                  return (
                    <div key={star} className="flex items-center gap-2">
                      <span className="w-6 font-semibold">{star}★</span>
                      <div className="flex-1 h-2 rounded-full bg-mag/10 overflow-hidden">
                        <div className="h-full bg-mag rounded-full" style={{ width: `${pct}%` }} />
                      </div>
                      <span className="w-8 sm:w-10 text-right text-ink/55 sm:text-ink/65 tabular-nums">
                        {count}
                      </span>
                    </div>
                  );
                })}
              </div>

              {sliderAggregates.length > 0 && (
                <div className="hidden sm:block">
                  <hr className="my-5 border-ink/8" />
                  <div className="text-[12px] font-bold mb-2">{t("byFeature")}</div>
                  <ul className="space-y-1.5 text-[12px]">
                    {sliderAggregates.map((agg) => (
                      <li key={agg.traitKey} className="flex items-center justify-between">
                        <span className="text-ink/70">{agg.labelI18n}</span>
                        <span className="font-bold text-mag">{agg.average.toFixed(1)}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            {/* ── Top reviews ──────────────────────────────────────── */}
            <div className="sm:col-span-2 grid grid-rows-2 gap-3 sm:gap-5">
              {(topReviews?.content ?? []).map((review, idx) => (
                <ReviewCardMag key={review.id} review={review} hideOnMobile={idx > 0} />
              ))}
            </div>
          </div>

          <div className="mt-4 sm:mt-5 text-center">
            <Link
              href={`/products/${slug}/reviews`}
              className="text-mag font-bold text-[12px] sm:text-[13px] hover:underline"
            >
              {t("seeAll", { count: reviewCount })}
            </Link>
          </div>
        </>
      )}

      {eligibleReview && (
        <SubmitReviewForm
          listingVariantId={listingVariantId}
          slug={slug}
          reviewTraits={reviewTraits}
          preselectedOrderId={eligibleReview.orderId}
          preselectedSkuId={eligibleReview.skuId}
          open={writeOpen}
          onOpenChange={setWriteOpen}
        />
      )}
    </section>
  );
}

function ReviewCardMag({ review, hideOnMobile }: { review: StorefrontReview; hideOnMobile: boolean }) {
  const t = useTranslations("reviews");

  const initials = review.authorName
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  const fullStars = Math.round(review.rating);

  return (
    <div
      className={`rounded-2xl sm:rounded-3xl bg-white border border-ink/8 p-4 sm:p-6 ${
        hideOnMobile ? "hidden sm:block" : ""
      }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 sm:gap-3">
          <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-mag/15 text-mag font-bold flex items-center justify-center text-[12px] sm:text-[14px]">
            {initials}
          </div>
          <div>
            <div className="text-[12px] sm:text-[13px] font-semibold">
              {review.authorName}{" "}
              <span className="text-[10px] sm:text-[11px] text-emerald font-bold ml-1">
                {t("block.verifiedBuyer")}
              </span>
            </div>
            <div className="text-[10px] sm:text-[11px] text-ink/55">{formatDate(review.createdAt)}</div>
          </div>
        </div>
        <div className="text-[12px] sm:text-[14px]" style={{ color: "#FFAB00" }} aria-label={`${review.rating} / 5`}>
          {"★".repeat(fullStars)}
          {"☆".repeat(5 - fullStars)}
        </div>
      </div>

      {review.title && <h4 className="font-bold text-[13px] sm:text-[15px] mt-2 sm:mt-3">{review.title}</h4>}
      <p className="text-[12px] sm:text-[13px] text-ink/75 mt-1 sm:mt-1.5 leading-relaxed">{review.body}</p>

      {review.photoUrls.length > 0 && (
        <div className="mt-2 sm:mt-3 flex gap-1.5 sm:gap-2">
          {review.photoUrls.slice(0, 3).map((url) => (
            <div key={url} className="w-12 h-12 sm:w-16 sm:h-16 rounded-lg overflow-hidden bg-soft">
              <Image
                src={url}
                alt={t("card.imageLightbox")}
                width={64}
                height={64}
                unoptimized
                className="w-full h-full object-cover"
              />
            </div>
          ))}
        </div>
      )}

      <div className="mt-2 sm:mt-3 text-[10px] sm:text-[11px] text-ink/55">
        👍 {t("block.helpful")} · {review.helpfulCount}
      </div>
    </div>
  );
}
