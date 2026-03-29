"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { fetchRatingSummary } from "@/entities/review/api";
import { RatingSummaryCard, ReviewList } from "@/entities/review";
import { SubmitReviewForm } from "@/features/review-submission";
import { fetchQuestions } from "@/entities/question/api";
import { QuestionList } from "@/entities/question";
import { AskQuestionDialog } from "@/features/ask-question";

interface ReviewsAndQuestionsSectionProps {
  slug: string;
  productBaseId: number;
  listingVariantId: number;
  isAuthenticated: boolean;
}

type ActiveTab = "reviews" | "questions";

export function ReviewsAndQuestionsSection({
  slug,
  productBaseId,
  listingVariantId,
  isAuthenticated,
}: ReviewsAndQuestionsSectionProps) {
  const t = useTranslations("section.tabs");
  const tQuestions = useTranslations("questions");

  const [activeTab, setActiveTab] = useState<ActiveTab>("reviews");

  const { data: ratingSummary } = useQuery({
    queryKey: ["rating", slug],
    queryFn: () => fetchRatingSummary(slug),
  });

  const { data: questionsData } = useQuery({
    queryKey: ["questions", productBaseId, 1, 6],
    queryFn: () => fetchQuestions(productBaseId, 1, 6),
  });

  const reviewCount = ratingSummary?.reviewCount ?? 0;
  const questionCount = questionsData?.totalElements ?? 0;

  return (
    <div className="mt-12 pt-8 border-t space-y-6">
      {/* Tab switcher */}
      <div className="flex items-center gap-6 border-b">
        <button
          onClick={() => setActiveTab("reviews")}
          className={`pb-3 text-base transition-colors ${
            activeTab === "reviews"
              ? "font-semibold border-b-2 border-foreground -mb-px"
              : "text-muted-foreground hover:text-foreground"
          }`}
        >
          {t("reviews")}
          {reviewCount > 0 && (
            <span className="ml-1.5 text-sm">{reviewCount}</span>
          )}
        </button>
        <button
          onClick={() => setActiveTab("questions")}
          className={`pb-3 text-base transition-colors ${
            activeTab === "questions"
              ? "font-semibold border-b-2 border-foreground -mb-px"
              : "text-muted-foreground hover:text-foreground"
          }`}
        >
          {t("questions")}
          {questionCount > 0 && (
            <span className="ml-1.5 text-sm">{questionCount}</span>
          )}
        </button>
      </div>

      {/* Reviews tab */}
      {activeTab === "reviews" && (
        <div className="space-y-6">
          {isAuthenticated && (
            <div className="flex justify-end">
              <SubmitReviewForm listingVariantId={listingVariantId} slug={slug} />
            </div>
          )}
          <RatingSummaryCard slug={slug} />
          <ReviewList slug={slug} mode="preview" />
        </div>
      )}

      {/* Questions tab */}
      {activeTab === "questions" && (
        <div className="space-y-6">
          {isAuthenticated ? (
            <div className="flex justify-end">
              <AskQuestionDialog productBaseId={productBaseId} />
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              {tQuestions("login")}
            </p>
          )}
          <QuestionList
            productBaseId={productBaseId}
            slug={slug}
            mode="preview"
          />
        </div>
      )}
    </div>
  );
}
