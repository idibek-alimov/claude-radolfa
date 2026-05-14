"use client";

import { useState, useEffect } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { updateBlueprintEntry } from "@/features/product-creation/api/blueprint";
import type {
  AdminBlueprintEntry,
  UpdateBlueprintEntryRequest,
} from "@/features/product-creation/model/types";
import { getErrorMessage } from "@/shared/lib";
import { BlueprintEntryForm, type BlueprintFormValue } from "./BlueprintEntryForm";

interface Props {
  entry: AdminBlueprintEntry;
  categoryId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const ENUM_TYPES = ["ENUM_SINGLE", "ENUM_MULTI"] as const;

export function EditBlueprintEntryDialog({ entry, categoryId, open, onOpenChange }: Props) {
  const [formValue, setFormValue] = useState<BlueprintFormValue>({
    attributeKey: entry.attributeKey,
    type: entry.type,
    unitName: entry.unitName ?? "",
    allowedValues: entry.allowedValues,
    required: entry.required,
    sortOrder: String(entry.sortOrder),
  });
  const [newValueInput, setNewValueInput] = useState("");
  const qc = useQueryClient();

  // Sync form when the entry prop changes (e.g. user opens edit for a different row)
  useEffect(() => {
    if (open) {
      setFormValue({
        attributeKey: entry.attributeKey,
        type: entry.type,
        unitName: entry.unitName ?? "",
        allowedValues: entry.allowedValues,
        required: entry.required,
        sortOrder: String(entry.sortOrder),
      });
      setNewValueInput("");
    }
  }, [open, entry]);

  const isEnum = ENUM_TYPES.includes(formValue.type as (typeof ENUM_TYPES)[number]);

  const mutation = useMutation({
    mutationFn: () => {
      const body: UpdateBlueprintEntryRequest = {
        attributeKey: formValue.attributeKey.trim(),
        required: formValue.required,
        sortOrder: parseInt(formValue.sortOrder, 10) || entry.sortOrder,
        ...(formValue.unitName.trim() && { unitName: formValue.unitName.trim() }),
        ...(isEnum && { allowedValues: formValue.allowedValues }),
      };
      return updateBlueprintEntry(categoryId, entry.id, body);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["blueprint-admin", categoryId] });
      onOpenChange(false);
      toast.success(`"${formValue.attributeKey.trim()}" updated`);
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const canSubmit =
    formValue.attributeKey.trim().length > 0 &&
    (!isEnum || formValue.allowedValues.length > 0) &&
    !mutation.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Edit Blueprint Entry</DialogTitle>
        </DialogHeader>

        <BlueprintEntryForm
          mode="edit"
          value={formValue}
          onChange={(patch) => setFormValue((prev) => ({ ...prev, ...patch }))}
          newValueInput={newValueInput}
          onNewValueInputChange={setNewValueInput}
        />

        <Button
          className="w-full mt-2"
          disabled={!canSubmit}
          onClick={() => mutation.mutate()}
        >
          {mutation.isPending ? "Saving…" : "Save Changes"}
        </Button>
      </DialogContent>
    </Dialog>
  );
}
