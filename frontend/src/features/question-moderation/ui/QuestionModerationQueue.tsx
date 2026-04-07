"use client";

import { useQuery } from "@tanstack/react-query";
import { Inbox, MessageCircleQuestion } from "lucide-react";
import { fetchPendingQuestions } from "@/entities/question";
import { QuestionModerationCard } from "./QuestionModerationCard";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/shared/ui/card";
import { Skeleton } from "@/shared/ui/skeleton";

export function QuestionModerationQueue() {
  const { data: questions = [], isLoading } = useQuery({
    queryKey: ["pending-questions"],
    queryFn: fetchPendingQuestions,
  });

  return (
    <>
      {/* Stats row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="rounded-xl border bg-card p-5 shadow-sm">
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
            Awaiting Moderation
          </p>
          <p className="text-2xl font-bold tabular-nums mt-1">
            {isLoading ? "—" : questions.length}
          </p>
          <p className="text-xs text-muted-foreground mt-1">Pending review</p>
        </div>
      </div>

      {/* Content card */}
      <Card>
        <CardHeader className="flex flex-row items-center gap-2 border-b py-4">
          <MessageCircleQuestion className="h-4 w-4 text-muted-foreground" />
          <CardTitle className="text-base font-semibold">Pending Questions</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-5 space-y-3">
              <Skeleton className="h-24 w-full rounded-xl" />
              <Skeleton className="h-24 w-full rounded-xl" />
              <Skeleton className="h-24 w-full rounded-xl" />
            </div>
          ) : questions.length === 0 ? (
            <div className="m-5 flex flex-col items-center justify-center gap-2 border border-dashed rounded-xl p-12">
              <Inbox className="h-10 w-10 text-muted-foreground/40" />
              <p className="text-sm text-muted-foreground">
                No questions pending moderation.
              </p>
            </div>
          ) : (
            <div className="divide-y">
              {questions.map((q) => (
                <QuestionModerationCard key={q.id} question={q} />
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </>
  );
}
