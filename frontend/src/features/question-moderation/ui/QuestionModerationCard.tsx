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
    <div className="border rounded-lg p-4 space-y-3 bg-card">
      {/* Header */}
      <div className="flex items-start gap-2">
        <MessageCircleQuestion className="h-4 w-4 mt-0.5 text-muted-foreground shrink-0" />
        <div className="space-y-0.5 min-w-0">
          <p className="text-sm">{question.questionText}</p>
          <p className="text-xs text-muted-foreground">
            {question.authorName} ·{" "}
            {new Date(question.createdAt).toLocaleDateString()}
          </p>
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-2">
        <AnswerDialog questionId={question.id} />
        <Button
          size="sm"
          variant="destructive"
          disabled={rejectMutation.isPending}
          onClick={() => rejectMutation.mutate()}
        >
          <XCircle className="h-3.5 w-3.5 mr-1" />
          {rejectMutation.isPending ? "Rejecting…" : "Reject"}
        </Button>
      </div>
    </div>
  );
}
