"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2 } from "lucide-react";
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
import { answerQuestion } from "@/entities/question";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";

interface AnswerDialogProps {
  questionId: number;
}

export function AnswerDialog({ questionId }: AnswerDialogProps) {
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
        <Button size="sm" variant="default" className="bg-green-600 hover:bg-green-700 text-white">
          <CheckCircle2 className="h-3.5 w-3.5 mr-1" />
          Answer
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Write an Answer</DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div className="space-y-1">
            <Label>Answer</Label>
            <Textarea
              value={text}
              onChange={(e) => setText(e.target.value)}
              rows={5}
              maxLength={2000}
              placeholder="Provide a helpful answer to the customer's question…"
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
            {mutation.isPending ? "Posting…" : "Post Answer"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
