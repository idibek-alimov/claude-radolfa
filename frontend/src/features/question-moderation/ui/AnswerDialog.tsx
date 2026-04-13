"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Pencil, ExternalLink, Package } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Textarea } from "@/shared/ui/textarea";
import { Label } from "@/shared/ui/label";
import { answerQuestion, updateAnswer } from "@/entities/question";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";

interface AnswerDialogProps {
  questionId: number;
  questionText: string;
  authorName: string;
  // Product context
  productName?: string;
  productSlug?: string;
  thumbnailUrl?: string | null;
  colorName?: string | null;
  colorHex?: string | null;
  // Edit mode
  initialAnswer?: string;
  isEdit?: boolean;
}

export function AnswerDialog({
  questionId,
  questionText,
  authorName,
  productName,
  productSlug,
  thumbnailUrl,
  colorName,
  colorHex,
  initialAnswer,
  isEdit = false,
}: AnswerDialogProps) {
  const [open, setOpen] = useState(false);
  const [text, setText] = useState(initialAnswer ?? "");
  const qc = useQueryClient();

  // Reset text whenever the dialog opens, picking up the latest initialAnswer.
  useEffect(() => {
    if (open) setText(initialAnswer ?? "");
  }, [open, initialAnswer]);

  const mutation = useMutation({
    mutationFn: () =>
      isEdit ? updateAnswer(questionId, text.trim()) : answerQuestion(questionId, text.trim()),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin-questions"] });
      setOpen(false);
      toast.success(isEdit ? "Answer updated" : "Answer posted");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button size="sm" variant="default">
          {isEdit ? (
            <>
              <Pencil className="h-3.5 w-3.5 mr-1.5" />
              Edit Answer
            </>
          ) : (
            <>
              <CheckCircle2 className="h-3.5 w-3.5 mr-1.5" />
              Answer
            </>
          )}
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit Answer" : "Answer Customer Question"}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? "Update the published answer. Changes are visible on the product page immediately."
              : "Your answer will be visible on the product page once posted."}
          </DialogDescription>
        </DialogHeader>

        {/* Product context block */}
        {productName && (
          <div className="bg-muted/40 rounded-xl p-4 flex items-center gap-3">
            {thumbnailUrl ? (
              <Image
                src={thumbnailUrl}
                alt={productName}
                width={48}
                height={48}
                unoptimized
                className="rounded-lg object-cover shrink-0"
              />
            ) : (
              <div className="h-12 w-12 rounded-lg bg-muted flex items-center justify-center shrink-0">
                <Package className="h-5 w-5 text-muted-foreground/40" />
              </div>
            )}
            <div className="min-w-0 space-y-1">
              <p className="text-sm font-semibold truncate">{productName}</p>
              {colorName && (
                <div className="flex items-center gap-1.5">
                  <span
                    className="h-2.5 w-2.5 rounded-full shrink-0 border border-black/10"
                    style={{ backgroundColor: colorHex ?? undefined }}
                  />
                  <span className="text-xs text-muted-foreground">{colorName}</span>
                </div>
              )}
              {productSlug && (
                <Link
                  href={`/product/${productSlug}`}
                  target="_blank"
                  className="inline-flex items-center gap-0.5 text-xs text-primary hover:underline"
                >
                  View Product
                  <ExternalLink className="h-3 w-3" />
                </Link>
              )}
            </div>
          </div>
        )}

        {/* Original question */}
        <div className="bg-muted/40 rounded-xl p-4 space-y-1.5">
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
            Question from {authorName}
          </p>
          <p className="text-sm text-foreground">{questionText}</p>
        </div>

        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="answer-text">Your Answer</Label>
            <Textarea
              id="answer-text"
              value={text}
              onChange={(e) => setText(e.target.value)}
              rows={6}
              maxLength={2000}
              placeholder="Provide a helpful, accurate answer…"
            />
            <p className="text-xs text-muted-foreground text-right tabular-nums">
              {text.length}/2000
            </p>
          </div>

          <div className="flex items-center justify-end gap-2">
            <Button variant="outline" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button
              disabled={!text.trim() || mutation.isPending}
              onClick={() => mutation.mutate()}
            >
              {mutation.isPending
                ? isEdit ? "Updating…" : "Posting…"
                : isEdit ? "Update Answer" : "Post Answer"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
