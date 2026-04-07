"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2 } from "lucide-react";
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
import { answerQuestion } from "@/entities/question";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";

interface AnswerDialogProps {
  questionId: number;
  questionText: string;
  authorName: string;
}

export function AnswerDialog({ questionId, questionText, authorName }: AnswerDialogProps) {
  const [open, setOpen] = useState(false);
  const [text, setText] = useState("");
  const qc = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => answerQuestion(questionId, text.trim()),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["pending-questions"] });
      setOpen(false);
      setText("");
      toast.success("Answer posted");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button size="sm" variant="default">
          <CheckCircle2 className="h-3.5 w-3.5 mr-1.5" />
          Answer
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Answer Customer Question</DialogTitle>
          <DialogDescription>
            Your answer will be visible on the product page once posted.
          </DialogDescription>
        </DialogHeader>

        {/* Original question context */}
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
              {mutation.isPending ? "Posting…" : "Post Answer"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
