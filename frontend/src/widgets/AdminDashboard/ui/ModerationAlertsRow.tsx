"use client";

import { useQuery } from "@tanstack/react-query";
import { Star, MessageSquare, ArrowRight, CheckCircle2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { fetchPendingReviews } from "@/entities/review";
import { fetchPendingQuestions } from "@/entities/question";
import { useAuth } from "@/features/auth";
import { cn } from "@/shared/lib";
import { Button } from "@/shared/ui/button";
import type { ReviewAdminView } from "@/entities/review";
import type { QuestionView } from "@/entities/question";

type UrgencyLevel = "clear" | "moderate" | "high";

function getUrgency(count: number): UrgencyLevel {
  if (count === 0) return "clear";
  if (count <= 5) return "moderate";
  return "high";
}

const urgencyRing: Record<UrgencyLevel, string> = {
  clear: "ring-2 ring-emerald-200",
  moderate: "ring-2 ring-amber-300",
  high: "ring-2 ring-rose-400",
};

const urgencyCountColor: Record<UrgencyLevel, string> = {
  clear: "text-emerald-600",
  moderate: "text-amber-600",
  high: "text-rose-600",
};

const urgencyIconBg: Record<UrgencyLevel, string> = {
  clear: "bg-emerald-50 text-emerald-500",
  moderate: "bg-amber-50 text-amber-500",
  high: "bg-rose-50 text-rose-500",
};

// ── Preview row for a pending review ─────────────────────────────────────────

function ReviewPreviewRow({ review }: { review: ReviewAdminView }) {
  return (
    <div className="flex items-center gap-3 py-2.5 border-b border-zinc-100 last:border-0">
      <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-zinc-100 text-[11px] font-bold text-zinc-500">
        {review.authorName.charAt(0).toUpperCase()}
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-xs font-semibold text-zinc-800 truncate">{review.authorName}</p>
        <p className="text-[11px] text-zinc-400 truncate leading-tight">
          {review.title ?? review.body.slice(0, 48)}
        </p>
      </div>
      <div className="flex items-center gap-0.5 shrink-0">
        {Array.from({ length: review.rating }).map((_, i) => (
          <Star key={i} className="h-3 w-3 fill-amber-400 text-amber-400" />
        ))}
      </div>
    </div>
  );
}

// ── Preview row for a pending question ───────────────────────────────────────

function QuestionPreviewRow({ question }: { question: QuestionView }) {
  return (
    <div className="flex items-center gap-3 py-2.5 border-b border-zinc-100 last:border-0">
      <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-zinc-100 text-[11px] font-bold text-zinc-500">
        {question.authorName.charAt(0).toUpperCase()}
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-xs font-semibold text-zinc-800 truncate">{question.authorName}</p>
        <p className="text-[11px] text-zinc-400 truncate leading-tight">{question.questionText}</p>
      </div>
    </div>
  );
}

// ── Shared card shell ─────────────────────────────────────────────────────────

interface ModerationCardProps {
  title: string;
  icon: React.ElementType;
  count: number | undefined;
  isLoading: boolean;
  href: string;
  children?: React.ReactNode;
}

function ModerationCard({ title, icon: Icon, count, isLoading, href, children }: ModerationCardProps) {
  const router = useRouter();
  const urgency = count !== undefined ? getUrgency(count) : "clear";

  return (
    <div
      className={cn(
        "bg-white rounded-xl border border-zinc-100 shadow-sm p-5 flex flex-col gap-4 transition-all duration-300",
        urgencyRing[urgency]
      )}
    >
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className={cn("flex h-8 w-8 items-center justify-center rounded-lg", urgencyIconBg[urgency])}>
            <Icon className="h-4 w-4" strokeWidth={2} />
          </div>
          <span className="text-sm font-semibold text-zinc-700">{title}</span>
        </div>

        {isLoading ? (
          <div className="h-7 w-8 rounded-md bg-zinc-100 animate-pulse" />
        ) : count === 0 ? (
          <CheckCircle2 className="h-5 w-5 text-emerald-500" />
        ) : (
          <span className={cn("text-2xl font-bold tabular-nums leading-none", urgencyCountColor[urgency])}>
            {count}
          </span>
        )}
      </div>

      {/* Preview area */}
      <div className="flex-1 min-h-[88px]">
        {isLoading ? (
          <div className="space-y-2">
            {[1, 2].map((i) => (
              <div key={i} className="h-10 rounded-lg bg-zinc-100 animate-pulse" />
            ))}
          </div>
        ) : count === 0 ? (
          <div className="flex items-center justify-center h-full py-4">
            <p className="text-xs text-zinc-400">All clear — nothing pending.</p>
          </div>
        ) : (
          <div>{children}</div>
        )}
      </div>

      <Button
        variant="outline"
        size="sm"
        className="w-full gap-1.5 text-xs font-medium"
        onClick={() => router.push(href)}
      >
        View all
        <ArrowRight className="h-3.5 w-3.5" />
      </Button>
    </div>
  );
}

// ── Exported component ────────────────────────────────────────────────────────

export function ModerationAlertsRow() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const { data: reviews = [], isLoading: reviewsLoading } = useQuery({
    queryKey: ["pending-reviews"],
    queryFn: fetchPendingReviews,
    staleTime: 30_000,
  });

  const { data: questions = [], isLoading: questionsLoading } = useQuery({
    queryKey: ["pending-questions"],
    queryFn: fetchPendingQuestions,
    staleTime: 30_000,
    enabled: isAdmin,
  });

  return (
    <div>
      <h2 className="text-[11px] font-semibold uppercase tracking-widest text-zinc-400 mb-3">
        Moderation
      </h2>
      <div className={cn("grid gap-4", isAdmin ? "lg:grid-cols-2" : "max-w-md")}>
        <ModerationCard
          title="Pending Reviews"
          icon={Star}
          count={reviewsLoading ? undefined : reviews.length}
          isLoading={reviewsLoading}
          href="/manage/reviews"
        >
          {reviews.slice(0, 2).map((r) => (
            <ReviewPreviewRow key={r.id} review={r} />
          ))}
        </ModerationCard>

        {isAdmin && (
          <ModerationCard
            title="Pending Q&A"
            icon={MessageSquare}
            count={questionsLoading ? undefined : questions.length}
            isLoading={questionsLoading}
            href="/manage/qa"
          >
            {questions.slice(0, 2).map((q) => (
              <QuestionPreviewRow key={q.id} question={q} />
            ))}
          </ModerationCard>
        )}
      </div>
    </div>
  );
}
