import Image from "next/image";
import { StarRating } from "@/shared/ui/StarRating";
import type { StorefrontReview, MatchingSize } from "../model/types";

const sizeFitLabel: Record<MatchingSize, string> = {
  RUNS_SMALL: "Runs Small",
  ACCURATE: "True to Size",
  RUNS_LARGE: "Runs Large",
};

interface ReviewCardProps {
  review: StorefrontReview;
}

export function ReviewCard({ review }: ReviewCardProps) {
  return (
    <div className="space-y-2 py-4 border-b last:border-b-0">
      {/* Header: stars + author + date */}
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <StarRating rating={review.rating} size="sm" />
          <span className="text-sm font-medium">{review.authorName}</span>
        </div>
        <span className="text-xs text-muted-foreground shrink-0">
          {new Date(review.createdAt).toLocaleDateString()}
        </span>
      </div>

      {/* Title */}
      {review.title && (
        <p className="text-sm font-semibold">{review.title}</p>
      )}

      {/* Body */}
      <p className="text-sm text-foreground">{review.body}</p>

      {/* Pros / Cons */}
      {(review.pros || review.cons) && (
        <div className="flex flex-wrap gap-2">
          {review.pros && (
            <span className="inline-flex items-center gap-1 rounded-full bg-green-50 px-2.5 py-0.5 text-xs text-green-700 border border-green-200">
              + {review.pros}
            </span>
          )}
          {review.cons && (
            <span className="inline-flex items-center gap-1 rounded-full bg-red-50 px-2.5 py-0.5 text-xs text-red-700 border border-red-200">
              − {review.cons}
            </span>
          )}
        </div>
      )}

      {/* Size fit badge */}
      {review.matchingSize && (
        <span className="inline-block rounded-full bg-muted px-2.5 py-0.5 text-xs text-muted-foreground">
          {sizeFitLabel[review.matchingSize]}
        </span>
      )}

      {/* Photo thumbnails */}
      {review.photoUrls.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {review.photoUrls.map((url, i) => (
            <div key={i} className="relative h-16 w-16 overflow-hidden rounded-md border">
              <Image
                src={url}
                alt={`Review photo ${i + 1}`}
                fill
                className="object-cover"
                unoptimized
              />
            </div>
          ))}
        </div>
      )}

      {/* Seller reply */}
      {review.sellerReply && (
        <div className="ml-4 border-l-2 border-muted pl-3">
          <p className="text-xs font-medium text-muted-foreground mb-0.5">Seller reply:</p>
          <p className="text-sm">{review.sellerReply}</p>
        </div>
      )}
    </div>
  );
}
