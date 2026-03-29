"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { HelpCircle } from "lucide-react";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Textarea } from "@/shared/ui/textarea";
import { Label } from "@/shared/ui/label";
import { askQuestion } from "@/entities/question";
import { useAuth } from "@/features/auth";
import { getErrorMessage } from "@/shared/lib";

interface AskQuestionDialogProps {
  productBaseId: number;
}

export function AskQuestionDialog({ productBaseId }: AskQuestionDialogProps) {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const [text, setText] = useState("");
  const qc = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => askQuestion({ productBaseId, questionText: text.trim() }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["questions", productBaseId] });
      handleClose();
      toast.success("Your question has been submitted.");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  function handleClose() {
    setOpen(false);
    setText("");
  }

  if (!user) {
    return (
      <p className="text-sm text-muted-foreground">
        <a href="/login" className="underline underline-offset-2">
          Sign in
        </a>{" "}
        to ask a question.
      </p>
    );
  }

  return (
    <Dialog open={open} onOpenChange={(v) => { if (!v) handleClose(); else setOpen(true); }}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <HelpCircle className="h-4 w-4 mr-1.5" />
          Ask a Question
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Ask a Question</DialogTitle>
        </DialogHeader>
        <div className="space-y-3 pt-1">
          <div className="space-y-1.5">
            <Label htmlFor="question-text">Your question</Label>
            <Textarea
              id="question-text"
              value={text}
              onChange={(e) => setText(e.target.value)}
              rows={4}
              maxLength={2000}
              placeholder="What would you like to know about this product?"
            />
            <p className="text-xs text-muted-foreground text-right">
              {text.length}/2000
            </p>
          </div>
          <Button
            className="w-full"
            disabled={!text.trim() || mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? "Submitting…" : "Submit Question"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
