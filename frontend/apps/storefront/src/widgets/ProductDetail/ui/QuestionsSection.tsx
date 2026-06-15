"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { fetchQuestions, QuestionList } from "@/entities/question";
import { AskQuestionDialog } from "@/features/ask-question";

const PREVIEW_SIZE = 6;

interface QuestionsSectionProps {
  slug: string;
  productBaseId: number;
  listingVariantId: number;
  isAuthenticated: boolean;
}

export default function QuestionsSection({
  slug,
  productBaseId,
  listingVariantId,
  isAuthenticated,
}: QuestionsSectionProps) {
  const t = useTranslations("questions");
  const tSection = useTranslations("section.tabs");

  const { data } = useQuery({
    queryKey: ["questions", productBaseId, 1, PREVIEW_SIZE],
    queryFn: () => fetchQuestions(productBaseId, 1, PREVIEW_SIZE),
  });

  const questionCount = data?.totalElements ?? 0;

  return (
    <section className="max-w-[1440px] mx-auto px-4 sm:px-6 mt-10">
      <div className="flex items-baseline justify-between mb-3 sm:mb-5">
        <h2 className="font-black text-lg sm:text-3xl">
          {tSection("questions")}
          {questionCount > 0 && ` · ${questionCount}`}
        </h2>
        {isAuthenticated ? (
          <AskQuestionDialog productBaseId={productBaseId} listingVariantId={listingVariantId} />
        ) : (
          <p className="text-[12px] sm:text-[13px] text-ink/55">{t("login")}</p>
        )}
      </div>

      <QuestionList productBaseId={productBaseId} slug={slug} mode="preview" />
    </section>
  );
}
