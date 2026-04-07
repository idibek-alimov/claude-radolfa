"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { XCircle, MessageCircleQuestion } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { rejectQuestion } from "@/entities/question";
import type { QuestionView } from "@/entities/question";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";
import { AnswerDialog } from "./AnswerDialog";

interface QuestionModerationCardProps {
  question: QuestionView;
}

export function QuestionModerationCard({ question }: QuestionModerationCardProps) {
  const qc = useQueryClient();

  const rejectMutation = useMutation({
    mutationFn: () => rejectQuestion(question.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["pending-questions"] });
      toast.success("Question rejected");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className="p-5 border-l-4 border-l-orange-400 bg-card hover:bg-muted/30 transition-colors">
      <div className="flex items-start justify-between gap-4">
        {/* Question block */}
        <div className="flex items-start gap-3 min-w-0 flex-1">
          <div className="shrink-0 rounded-full bg-orange-100 p-2">
            <MessageCircleQuestion className="h-4 w-4 text-orange-600" />
          </div>
          <div className="space-y-1.5 min-w-0">
            <p className="text-base font-medium text-foreground break-words">
              {question.questionText}
            </p>
            <div className="flex items-center flex-wrap gap-2 text-xs text-muted-foreground">
              <span>{question.authorName}</span>
              <span>·</span>
              <span>{new Date(question.createdAt).toLocaleDateString()}</span>
              <span className="inline-flex items-center gap-1 rounded-full bg-orange-50 px-2 py-0.5 text-xs font-medium text-orange-700">
                <span className="h-1.5 w-1.5 rounded-full bg-orange-500" />
                Pending
              </span>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2 shrink-0">
          <AnswerDialog
            questionId={question.id}
            questionText={question.questionText}
            authorName={question.authorName}
          />
          <Button
            size="sm"
            variant="outline"
            className="text-rose-600 border-rose-200 hover:bg-rose-50 hover:text-rose-700"
            disabled={rejectMutation.isPending}
            onClick={() => rejectMutation.mutate()}
          >
            <XCircle className="h-3.5 w-3.5 mr-1.5" />
            {rejectMutation.isPending ? "Rejecting…" : "Reject"}
          </Button>
        </div>
      </div>
    </div>
  );
}
