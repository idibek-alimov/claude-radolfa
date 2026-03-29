"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { fetchQuestions } from "../api";
import { QuestionCard } from "./QuestionCard";

const PREVIEW_SIZE = 6;
const FULL_SIZE = 10;
const CARDS_PER_SLIDE = 3;

interface QuestionListProps {
  productBaseId: number;
  slug: string;
  mode?: "preview" | "full";
}

export function QuestionList({ productBaseId, slug, mode = "preview" }: QuestionListProps) {
  const t = useTranslations("questions");
  const [page, setPage] = useState(1);
  const [carouselPage, setCarouselPage] = useState(0);

  const fetchSize = mode === "preview" ? PREVIEW_SIZE : FULL_SIZE;
  const fetchPage = mode === "preview" ? 1 : page;

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["questions", productBaseId, fetchPage, fetchSize],
    queryFn: () => fetchQuestions(productBaseId, fetchPage, fetchSize),
  });

  const questionsPageUrl = `/products/${slug}/questions`;

  // ── Preview mode ──────────────────────────────────────────────────────────
  if (mode === "preview") {
    const content = data?.content ?? [];
    const totalSlides = Math.ceil(content.length / CARDS_PER_SLIDE);
    const visibleCards = content.slice(
      carouselPage * CARDS_PER_SLIDE,
      (carouselPage + 1) * CARDS_PER_SLIDE,
    );

    return (
      <div className="space-y-4">
        {isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="rounded-xl border p-4 h-28 animate-pulse bg-muted" />
            ))}
          </div>
        ) : content.length === 0 ? (
          <p className="py-4 text-sm text-muted-foreground text-center">{t("empty")}</p>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {visibleCards.map((q) => (
                <QuestionCard key={q.id} question={q} />
              ))}
            </div>

            {totalSlides > 1 && (
              <div className="flex justify-center items-center gap-3">
                <button
                  disabled={carouselPage === 0}
                  onClick={() => setCarouselPage((p) => p - 1)}
                  className="p-1 rounded-full border hover:bg-muted transition-colors disabled:opacity-30"
                  aria-label="Previous slide"
                >
                  <ChevronLeft className="h-4 w-4" />
                </button>
                <span className="text-xs text-muted-foreground">
                  {carouselPage + 1} / {totalSlides}
                </span>
                <button
                  disabled={carouselPage === totalSlides - 1}
                  onClick={() => setCarouselPage((p) => p + 1)}
                  className="p-1 rounded-full border hover:bg-muted transition-colors disabled:opacity-30"
                  aria-label="Next slide"
                >
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            )}
          </>
        )}

        <div className="flex justify-center">
          <Link
            href={questionsPageUrl}
            className="text-sm px-5 py-2 rounded-full border border-primary text-primary hover:bg-primary hover:text-primary-foreground transition-colors"
          >
            {t("seeAll")}
          </Link>
        </div>
      </div>
    );
  }

  // ── Full mode ─────────────────────────────────────────────────────────────
  const questions = data?.content ?? [];

  return (
    <div className="space-y-4">
      <div className={`space-y-4 transition-opacity ${isFetching && !isLoading ? "opacity-50" : ""}`}>
        {isLoading
          ? Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="rounded-xl border p-4 h-28 animate-pulse bg-muted" />
            ))
          : questions.length === 0
            ? <p className="py-4 text-sm text-muted-foreground">{t("empty")}</p>
            : questions.map((q) => (
                <QuestionCard key={q.id} question={q} />
              ))}
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex justify-center items-center gap-2">
          <button
            disabled={page === 1}
            onClick={() => setPage((p) => p - 1)}
            className="px-3 py-1 text-sm border rounded disabled:opacity-30 hover:bg-muted transition-colors"
          >
            Previous
          </button>
          <span className="px-3 py-1 text-sm text-muted-foreground">
            {page} / {data.totalPages}
          </span>
          <button
            disabled={page === data.totalPages}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1 text-sm border rounded disabled:opacity-30 hover:bg-muted transition-colors"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
