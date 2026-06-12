"use client";

import { useTranslations } from "next-intl";
import type { QuestionView } from "../model/types";

function getInitials(name: string): string {
  return name
    .split(" ")
    .map((part) => part[0] ?? "")
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

interface QuestionCardProps {
  question: QuestionView;
}

export function QuestionCard({ question }: QuestionCardProps) {
  const t = useTranslations("questions.card");

  return (
    <div className="rounded-xl border bg-card p-4 space-y-3">
      {/* Header: avatar + name / date */}
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2.5 min-w-0">
          <div className="h-8 w-8 rounded-full bg-muted flex items-center justify-center text-xs font-semibold text-muted-foreground shrink-0">
            {getInitials(question.authorName)}
          </div>
          <span className="text-sm font-semibold truncate">{question.authorName}</span>
        </div>
        <span className="text-xs text-muted-foreground shrink-0">
          {new Date(question.createdAt).toLocaleDateString()}
        </span>
      </div>

      {/* Question text */}
      <p className="text-sm">{question.questionText}</p>

      {/* Seller reply box — only when answered */}
      {question.answerText && (
        <div className="bg-muted rounded-lg p-3 space-y-1">
          <p className="text-xs font-medium text-muted-foreground">{t("sellerReply")}</p>
          <p className="text-sm">{question.answerText}</p>
        </div>
      )}
    </div>
  );
}
