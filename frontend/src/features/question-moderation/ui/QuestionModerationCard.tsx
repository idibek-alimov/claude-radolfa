"use client";

import Image from "next/image";
import Link from "next/link";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { XCircle, ExternalLink, Package } from "lucide-react";
import { Button } from "@/shared/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/shared/ui/alert-dialog";
import { rejectQuestion } from "@/entities/question";
import type { QuestionAdminView } from "@/entities/question";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";
import { AnswerDialog } from "./AnswerDialog";

interface QuestionModerationCardProps {
  question: QuestionAdminView;
}

export function QuestionModerationCard({ question }: QuestionModerationCardProps) {
  const qc = useQueryClient();
  const isAnswered = question.status === "PUBLISHED";

  const rejectMutation = useMutation({
    mutationFn: () => rejectQuestion(question.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin-questions"] });
      toast.success("Question rejected");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className={`p-5 border-l-4 bg-card hover:bg-muted/30 transition-colors ${isAnswered ? "border-l-green-500" : "border-l-orange-400"}`}>
      <div className="flex items-start gap-5">
        {/* Left column — product context */}
        <div className="shrink-0 w-56 flex items-start gap-3">
          {question.thumbnailUrl ? (
            <Image
              src={question.thumbnailUrl}
              alt={question.productName}
              width={64}
              height={64}
              unoptimized
              className="rounded-lg object-cover shrink-0"
            />
          ) : (
            <div className="h-16 w-16 rounded-lg bg-muted flex items-center justify-center shrink-0">
              <Package className="h-6 w-6 text-muted-foreground/40" />
            </div>
          )}
          <div className="min-w-0 space-y-1 pt-0.5">
            <p className="text-sm font-semibold truncate">{question.productName}</p>
            {question.colorName && (
              <div className="flex items-center gap-1.5">
                <span
                  className="h-2.5 w-2.5 rounded-full shrink-0 border border-black/10"
                  style={{ backgroundColor: question.colorHex ?? undefined }}
                />
                <span className="text-xs text-muted-foreground truncate">{question.colorName}</span>
              </div>
            )}
            <Link
              href={`/product/${question.productSlug}`}
              target="_blank"
              className="inline-flex items-center gap-0.5 text-xs text-primary hover:underline"
            >
              View Product
              <ExternalLink className="h-3 w-3" />
            </Link>
          </div>
        </div>

        {/* Right column — question + answer + actions */}
        <div className="flex-1 min-w-0 flex flex-col gap-3">
          <p className="text-base font-medium text-foreground break-words">
            {question.questionText}
          </p>

          {isAnswered && question.answerText && (
            <div className="bg-green-50 border border-green-100 rounded-lg px-4 py-3 space-y-1">
              <p className="text-xs font-medium text-green-700 uppercase tracking-wide">Answer</p>
              <p className="text-sm text-foreground break-words">{question.answerText}</p>
            </div>
          )}

          <div className="flex items-center flex-wrap gap-2 text-xs text-muted-foreground">
            <span>{question.authorName}</span>
            <span>·</span>
            <span>{new Date(question.createdAt).toLocaleDateString()}</span>
            {isAnswered && question.answeredAt && (
              <>
                <span>·</span>
                <span>Answered {new Date(question.answeredAt).toLocaleDateString()}</span>
              </>
            )}
            {isAnswered ? (
              <span className="inline-flex items-center gap-1 rounded-full bg-green-50 px-2 py-0.5 text-xs font-medium text-green-700">
                <span className="h-1.5 w-1.5 rounded-full bg-green-500" />
                Answered
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 rounded-full bg-orange-50 px-2 py-0.5 text-xs font-medium text-orange-700">
                <span className="h-1.5 w-1.5 rounded-full bg-orange-500" />
                Pending
              </span>
            )}
          </div>

          <div className="flex items-center gap-2">
            <AnswerDialog
              questionId={question.id}
              questionText={question.questionText}
              authorName={question.authorName}
              productName={question.productName}
              productSlug={question.productSlug}
              thumbnailUrl={question.thumbnailUrl}
              colorName={question.colorName}
              colorHex={question.colorHex}
              initialAnswer={question.answerText ?? undefined}
              isEdit={isAnswered}
            />
            {isAnswered ? (
              <AlertDialog>
                <AlertDialogTrigger asChild>
                  <Button
                    size="sm"
                    variant="outline"
                    className="text-rose-600 border-rose-200 hover:bg-rose-50 hover:text-rose-700"
                    disabled={rejectMutation.isPending}
                  >
                    <XCircle className="h-3.5 w-3.5 mr-1.5" />
                    {rejectMutation.isPending ? "Rejecting…" : "Reject"}
                  </Button>
                </AlertDialogTrigger>
                <AlertDialogContent className="max-w-sm">
                  <AlertDialogHeader>
                    <AlertDialogTitle>Reject published Q&amp;A?</AlertDialogTitle>
                    <AlertDialogDescription>
                      This will remove the published answer for{" "}
                      <span className="font-medium text-foreground">{question.productName}</span>{" "}
                      from the product page immediately.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>
                    <AlertDialogAction
                      className="bg-rose-600 hover:bg-rose-700 text-white"
                      onClick={() => rejectMutation.mutate()}
                    >
                      Reject
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            ) : (
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
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
