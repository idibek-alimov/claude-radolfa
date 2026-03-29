"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Inbox } from "lucide-react";
import { fetchQuestions } from "../api";
import { QuestionCard } from "./QuestionCard";

interface QuestionListProps {
  productBaseId: number;
}

const PAGE_SIZE = 10;

export function QuestionList({ productBaseId }: QuestionListProps) {
  const [page, setPage] = useState(1);

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["questions", productBaseId, page],
    queryFn: () => fetchQuestions(productBaseId, page, PAGE_SIZE),
  });

  const questions = data?.content ?? [];

  return (
    <div className="space-y-4">
      {isLoading ? (
        <div>
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="space-y-2 py-4 border-b last:border-b-0 animate-pulse">
              <div className="flex items-start gap-2">
                <div className="h-4 w-4 rounded bg-muted shrink-0 mt-0.5" />
                <div className="space-y-1.5 flex-1">
                  <div className="h-4 w-3/4 rounded bg-muted" />
                  <div className="h-3 w-1/4 rounded bg-muted" />
                </div>
              </div>
              <div className="ml-4 h-3 w-1/3 rounded bg-muted" />
            </div>
          ))}
        </div>
      ) : questions.length === 0 ? (
        <div className="flex flex-col items-center gap-2 py-8 text-muted-foreground">
          <Inbox className="h-7 w-7" />
          <p className="text-sm">No questions yet. Be the first to ask!</p>
        </div>
      ) : (
        <>
          <div className={`transition-opacity ${isFetching ? "opacity-50" : ""}`}>
            {questions.map((q) => (
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
        </>
      )}
    </div>
  );
}
