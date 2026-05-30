"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { MessageSquare } from "lucide-react";
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
import { replyToReview } from "@/entities/review";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";

interface ReplyDialogProps {
  reviewId: number;
  existingReply: string | null;
}

export function ReplyDialog({ reviewId, existingReply }: ReplyDialogProps) {
  const [open, setOpen] = useState(false);
  const [text, setText] = useState(existingReply ?? "");
  const qc = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => replyToReview(reviewId, text.trim()),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["pending-reviews"] });
      setOpen(false);
      toast.success(existingReply ? "Reply updated" : "Reply posted");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button size="sm" variant="outline">
          <MessageSquare className="h-3.5 w-3.5 mr-1" />
          {existingReply ? "Edit Reply" : "Reply"}
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {existingReply ? "Edit Seller Reply" : "Add Seller Reply"}
          </DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div className="space-y-1">
            <Label>Reply</Label>
            <Textarea
              value={text}
              onChange={(e) => setText(e.target.value)}
              rows={4}
              placeholder="Write a helpful response to the customer…"
              maxLength={1000}
            />
            <p className="text-xs text-muted-foreground text-right">
              {text.length}/1000
            </p>
          </div>
          <Button
            className="w-full"
            disabled={!text.trim() || mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? "Saving…" : "Save Reply"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
