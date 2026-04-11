"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ChevronLeft } from "lucide-react";
import { fetchListingBySlug } from "@/entities/product/api";
import { fetchQuestions } from "@/entities/question/api";
import { QuestionList } from "@/entities/question";
import { AskQuestionDialog } from "@/features/ask-question";
import { useAuth } from "@/features/auth";

interface ProductQuestionsPageProps {
  slug: string;
}

export function ProductQuestionsPage({ slug }: ProductQuestionsPageProps) {
  const t = useTranslations("questions.page");
  const tQuestions = useTranslations("questions");
  const { isAuthenticated } = useAuth();

  const { data: listing } = useQuery({
    queryKey: ["listing", slug],
    queryFn: () => fetchListingBySlug(slug),
  });

  const productBaseId = listing?.productBaseId;

  const { data: questionsData } = useQuery({
    queryKey: ["questions", productBaseId, 1, 10],
    queryFn: () => fetchQuestions(productBaseId!, 1, 10),
    enabled: productBaseId != null,
  });

  const totalQuestions = questionsData?.totalElements;

  return (
    <div className="container mx-auto px-4 py-8 max-w-2xl">
      {/* Back link */}
      <Link
        href={`/products/${slug}`}
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-6"
      >
        <ChevronLeft className="h-4 w-4" />
        {t("back")}
      </Link>

      {/* Heading */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">
          {t("title")}
          {totalQuestions != null && totalQuestions > 0 && (
            <span className="ml-2 text-lg font-normal text-muted-foreground">
              {totalQuestions}
            </span>
          )}
        </h1>

        {isAuthenticated && productBaseId != null && (
          <AskQuestionDialog productBaseId={productBaseId} listingVariantId={listing?.variantId} />
        )}
      </div>

      {!isAuthenticated && (
        <p className="text-sm text-muted-foreground mb-6">
          {tQuestions("login")}
        </p>
      )}

      {productBaseId != null && (
        <QuestionList
          productBaseId={productBaseId}
          slug={slug}
          mode="full"
        />
      )}
    </div>
  );
}
