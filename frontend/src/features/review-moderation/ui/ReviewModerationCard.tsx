"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Star, CheckCircle, XCircle, Image as ImageIcon } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { approveReview, rejectReview } from "@/entities/review";
import type { ReviewAdminView } from "@/entities/review";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";
import { useAuth } from "@/features/auth";
import { UserRole } from "@/entities/user";
import { ReplyDialog } from "./ReplyDialog";

interface ReviewModerationCardProps {
  review: ReviewAdminView;
}

const SIZE_FIT_LABELS: Record<string, string> = {
  RUNS_SMALL: "Runs Small",
  ACCURATE: "True to Size",
  RUNS_LARGE: "Runs Large",
};

export function ReviewModerationCard({ review }: ReviewModerationCardProps) {
  const { user } = useAuth();
  const isAdmin = user?.role === UserRole.ADMIN;
  const qc = useQueryClient();

  const onSuccess = () => {
    qc.invalidateQueries({ queryKey: ["pending-reviews"] });
  };

  const approveMutation = useMutation({
    mutationFn: () => approveReview(review.id),
    onSuccess: () => { onSuccess(); toast.success("Review approved"); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const rejectMutation = useMutation({
    mutationFn: () => rejectReview(review.id),
    onSuccess: () => { onSuccess(); toast.success("Review rejected"); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const isPending = approveMutation.isPending || rejectMutation.isPending;

  return (
    <div className="border rounded-lg p-4 space-y-3 bg-card">
      {/* Header */}
      <div className="flex items-start justify-between gap-2">
        <div className="space-y-0.5">
          <div className="flex items-center gap-2">
            <span className="font-medium text-sm">{review.authorName}</span>
            <span className="text-xs text-muted-foreground">
              {new Date(review.createdAt).toLocaleDateString()}
            </span>
          </div>
          <a
            href={`/products/${review.variantSlug}`}
            target="_blank"
            rel="noopener noreferrer"
            className="text-xs text-primary underline-offset-2 hover:underline"
          >
            {review.variantSlug}
          </a>
        </div>
        <div className="flex items-center gap-1">
          {Array.from({ length: 5 }).map((_, i) => (
            <Star
              key={i}
              className={`h-4 w-4 ${
                i < review.rating
                  ? "fill-amber-400 text-amber-400"
                  : "text-muted-foreground"
              }`}
            />
          ))}
        </div>
      </div>

      {/* Content */}
      <div className="space-y-1">
        {review.title && (
          <p className="font-medium text-sm">{review.title}</p>
        )}
        <p className="text-sm text-muted-foreground">{review.body}</p>
      </div>

      {/* Pros / Cons / Size fit */}
      {(review.pros || review.cons || review.matchingSize) && (
        <div className="flex flex-wrap gap-2 text-xs">
          {review.pros && (
            <span className="text-green-600 bg-green-50 px-2 py-0.5 rounded">
              + {review.pros}
            </span>
          )}
          {review.cons && (
            <span className="text-red-600 bg-red-50 px-2 py-0.5 rounded">
              − {review.cons}
            </span>
          )}
          {review.matchingSize && (
            <Badge variant="outline">
              {SIZE_FIT_LABELS[review.matchingSize] ?? review.matchingSize}
            </Badge>
          )}
        </div>
      )}

      {/* Photos */}
      {review.photoUrls.length > 0 && (
        <div className="flex items-center gap-1 text-xs text-muted-foreground">
          <ImageIcon className="h-3 w-3" />
          {review.photoUrls.length} photo{review.photoUrls.length > 1 ? "s" : ""}
        </div>
      )}

      {/* Existing seller reply */}
      {review.sellerReply && (
        <div className="bg-muted rounded p-2 text-xs space-y-0.5">
          <span className="font-medium">Seller reply:</span>
          <p className="text-muted-foreground">{review.sellerReply}</p>
        </div>
      )}

      {/* Actions */}
      <div className="flex items-center gap-2 pt-1">
        {isAdmin && (
          <>
            <Button
              size="sm"
              variant="default"
              className="bg-green-600 hover:bg-green-700 text-white"
              disabled={isPending}
              onClick={() => approveMutation.mutate()}
            >
              <CheckCircle className="h-3.5 w-3.5 mr-1" />
              Approve
            </Button>
            <Button
              size="sm"
              variant="destructive"
              disabled={isPending}
              onClick={() => rejectMutation.mutate()}
            >
              <XCircle className="h-3.5 w-3.5 mr-1" />
              Reject
            </Button>
          </>
        )}
        <ReplyDialog reviewId={review.id} existingReply={review.sellerReply} />
      </div>
    </div>
  );
}
