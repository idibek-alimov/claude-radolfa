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
import { TagBadge } from "@/entities/tag";
import { createTag } from "@/entities/tag/api";
import { getErrorMessage } from "@/shared/lib";

export function CreateTagDialog() {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [colorHex, setColorHex] = useState("E74C3C");
  const qc = useQueryClient();

  const mutation = useMutation({
    mutationFn: createTag,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tags"] });
      setOpen(false);
      setName("");
      setColorHex("E74C3C");
      toast.success("Tag created");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button size="sm">New Tag</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Create Tag</DialogTitle>
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
              <TagBadge tag={{ id: 0, name, colorHex }} size="md" />
            </div>
          )}
          <Button
            className="w-full"
            disabled={!name.trim() || colorHex.length !== 6 || mutation.isPending}
            onClick={() => mutation.mutate({ name: name.trim(), colorHex })}
          >
            {mutation.isPending ? "Creating…" : "Create"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
