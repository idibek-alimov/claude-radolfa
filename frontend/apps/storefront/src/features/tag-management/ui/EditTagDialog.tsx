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
import { TagBadge } from "@/entities/tag";
import { updateTag } from "@/entities/tag/api";
import { getErrorMessage } from "@/shared/lib";
import type { Tag } from "@/entities/tag";

interface EditTagDialogProps {
  tag: Tag;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditTagDialog({ tag, open, onOpenChange }: EditTagDialogProps) {
  const [name, setName] = useState(tag.name);
  const [colorHex, setColorHex] = useState(tag.colorHex);
  const qc = useQueryClient();

  useEffect(() => {
    if (open) {
      setName(tag.name);
      setColorHex(tag.colorHex);
    }
  }, [open, tag]);

  const mutation = useMutation({
    mutationFn: () => updateTag(tag.id, { name: name.trim(), colorHex }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tags"] });
      onOpenChange(false);
      toast.success("Tag updated");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit Tag</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-1">
            <Label>Name</Label>
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={64}
              placeholder="e.g. New Arrival"
            />
          </div>
          <div className="space-y-1">
            <Label>Color (hex, no #)</Label>
            <div className="flex items-center gap-2">
              <input
                type="color"
                value={`#${colorHex}`}
                onChange={(e) => setColorHex(e.target.value.replace("#", ""))}
                className="h-9 w-12 cursor-pointer rounded border border-input bg-transparent p-0.5"
              />
              <Input
                value={colorHex}
                onChange={(e) => setColorHex(e.target.value.replace("#", ""))}
                maxLength={6}
                placeholder="E74C3C"
                className="font-mono w-28"
              />
            </div>
          </div>
          {name && colorHex.length === 6 && (
            <div className="flex items-center gap-2">
              <span className="text-sm text-muted-foreground">Preview:</span>
              <TagBadge tag={{ id: tag.id, name, colorHex }} size="md" />
            </div>
          )}
          <Button
            className="w-full"
            disabled={!name.trim() || colorHex.length !== 6 || mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? "Saving…" : "Save Changes"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
