"use client";

import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { RadioGroup, RadioGroupItem } from "@/shared/ui/radio-group";
import { AlertCircle, Lock } from "lucide-react";
import { updateReviewTrait } from "@/entities/review-trait";
import { getErrorMessage } from "@/shared/lib";
import type { ReviewTrait, ReviewTraitInputType } from "@/entities/review-trait";

interface EditReviewTraitDialogProps {
  trait: ReviewTrait;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditReviewTraitDialog({ trait, open, onOpenChange }: EditReviewTraitDialogProps) {
  const [labelI18n, setLabelI18n] = useState(trait.labelI18n);
  const [inputType, setInputType] = useState<ReviewTraitInputType>(trait.inputType);
  const [error, setError] = useState("");
  const qc = useQueryClient();

  useEffect(() => {
    if (open) {
      setLabelI18n(trait.labelI18n);
      setInputType(trait.inputType);
      setError("");
    }
  }, [open, trait]);

  const mutation = useMutation({
    mutationFn: () => updateReviewTrait(trait.id, { labelI18n: labelI18n.trim(), inputType }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["review-traits"] });
      onOpenChange(false);
      toast.success("Trait updated");
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  function handleSubmit() {
    if (!labelI18n.trim()) { setError("Label is required"); return; }
    if (labelI18n.length > 255) { setError("Label must be 255 characters or fewer"); return; }
    setError("");
    mutation.mutate();
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Edit Review Trait</DialogTitle>
        </DialogHeader>
        <div className="space-y-5 py-2">
          <div className="space-y-1.5">
            <Label>Key</Label>
            <div className="relative">
              <Input
                value={trait.key}
                disabled
                className="font-mono pr-9 bg-muted/40 text-muted-foreground cursor-not-allowed"
              />
              <Lock className="absolute right-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
            </div>
            <p className="text-xs text-muted-foreground">Key is system-managed and cannot be changed.</p>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="edit-trait-label">Label</Label>
            <Input
              id="edit-trait-label"
              value={labelI18n}
              onChange={(e) => { setLabelI18n(e.target.value); setError(""); }}
              maxLength={255}
              placeholder="e.g. Comfort Level"
            />
          </div>

          <div className="space-y-2">
            <Label>Input Type</Label>
            <RadioGroup
              value={inputType}
              onValueChange={(v) => setInputType(v as ReviewTraitInputType)}
              className="flex gap-6"
            >
              <div className="flex items-center gap-2">
                <RadioGroupItem value="SLIDER" id="edit-type-slider" />
                <Label htmlFor="edit-type-slider" className="cursor-pointer font-normal">
                  Slider (1–5)
                </Label>
              </div>
              <div className="flex items-center gap-2">
                <RadioGroupItem value="RADIO" id="edit-type-radio" />
                <Label htmlFor="edit-type-radio" className="cursor-pointer font-normal">
                  Radio Options
                </Label>
              </div>
            </RadioGroup>
          </div>

          {error && (
            <p className="text-sm text-destructive flex items-center gap-1.5">
              <AlertCircle className="h-3.5 w-3.5" />
              {error}
            </p>
          )}

          <Button
            className="w-full"
            disabled={mutation.isPending}
            onClick={handleSubmit}
          >
            {mutation.isPending ? "Saving…" : "Save Changes"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
