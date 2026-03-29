"use client";

import { useQuery } from "@tanstack/react-query";
import { Loader2, Inbox } from "lucide-react";
import { fetchPendingQuestions } from "@/entities/question";
import { QuestionModerationCard } from "./QuestionModerationCard";

export function QuestionModerationQueue() {
  const { data: questions = [], isLoading } = useQuery({
    queryKey: ["pending-questions"],
    queryFn: fetchPendingQuestions,
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Pending Questions</h2>
        {!isLoading && (
          <span className="text-sm text-muted-foreground">
            {questions.length} awaiting moderation
          </span>
        )}
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
        </div>
      ) : questions.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 gap-2 text-muted-foreground">
          <Inbox className="h-8 w-8" />
          <p className="text-sm">No questions pending moderation.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {questions.map((q) => (
            <QuestionModerationCard key={q.id} question={q} />
          ))}
        </div>
      )}
    </div>
  );
}
