"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { RadioGroup, RadioGroupItem } from "@/shared/ui/radio-group";
import { AlertCircle, Plus } from "lucide-react";
import { createReviewTrait } from "@/entities/review-trait";
import { getErrorMessage } from "@/shared/lib";
import type { ReviewTraitInputType } from "@/entities/review-trait";

const KEY_REGEX = /^[a-z][a-z0-9_]*$/;

export function CreateReviewTraitDialog() {
  const [open, setOpen] = useState(false);
  const [key, setKey] = useState("");
  const [labelI18n, setLabelI18n] = useState("");
  const [inputType, setInputType] = useState<ReviewTraitInputType>("SLIDER");
  const [error, setError] = useState("");
  const qc = useQueryClient();

  const mutation = useMutation({
    mutationFn: createReviewTrait,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["review-traits"] });
      setOpen(false);
      reset();
      toast.success("Trait created");
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  function reset() {
    setKey("");
    setLabelI18n("");
    setInputType("SLIDER");
    setError("");
  }

  function validate(): string {
    if (!key.trim()) return "Key is required";
    if (!KEY_REGEX.test(key)) return "Key must start with a letter; only lowercase letters, digits, and underscores";
    if (key.length > 64) return "Key must be 64 characters or fewer";
    if (!labelI18n.trim()) return "Label is required";
    if (labelI18n.length > 255) return "Label must be 255 characters or fewer";
    return "";
  }

  function handleSubmit() {
    const err = validate();
    if (err) { setError(err); return; }
    setError("");
    mutation.mutate({ key: key.trim(), labelI18n: labelI18n.trim(), inputType });
  }

  return (
    <Dialog open={open} onOpenChange={(v) => { setOpen(v); if (!v) reset(); }}>
      <DialogTrigger asChild>
        <Button size="sm">
          <Plus className="h-4 w-4 mr-1.5" />
          New Trait
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Create Review Trait</DialogTitle>
        </DialogHeader>
        <div className="space-y-5 py-2">
          <div className="space-y-1.5">
            <Label htmlFor="trait-key">Key</Label>
            <Input
              id="trait-key"
              value={key}
              onChange={(e) => { setKey(e.target.value); setError(""); }}
              maxLength={64}
              placeholder="e.g. comfort_level"
              className="font-mono"
            />
            <p className="text-xs text-muted-foreground">
              Lowercase letters, digits, and underscores. Cannot be changed later.
            </p>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="trait-label">Label</Label>
            <Input
              id="trait-label"
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
                <RadioGroupItem value="SLIDER" id="type-slider" />
                <Label htmlFor="type-slider" className="cursor-pointer font-normal">
                  Slider (1–5)
                </Label>
              </div>
              <div className="flex items-center gap-2">
                <RadioGroupItem value="RADIO" id="type-radio" />
                <Label htmlFor="type-radio" className="cursor-pointer font-normal">
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
            {mutation.isPending ? "Creating…" : "Create Trait"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
