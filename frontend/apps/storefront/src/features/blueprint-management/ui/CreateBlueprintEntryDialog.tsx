"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { createBlueprintEntry } from "@/features/product-creation/api/blueprint";
import type { CreateBlueprintEntryRequest } from "@/features/product-creation/model/types";
import { getErrorMessage } from "@/shared/lib";
import { BlueprintEntryForm, type BlueprintFormValue } from "./BlueprintEntryForm";

interface Props {
  categoryId: number;
  nextSortOrder: number;
}

const ENUM_TYPES = ["ENUM_SINGLE", "ENUM_MULTI"] as const;

export function CreateBlueprintEntryDialog({ categoryId, nextSortOrder }: Props) {
  const [open, setOpen] = useState(false);
  const [formValue, setFormValue] = useState<BlueprintFormValue>({
    attributeKey: "",
    type: "TEXT",
    unitName: "",
    allowedValues: [],
    required: false,
    sortOrder: String(nextSortOrder),
  });
  const [newValueInput, setNewValueInput] = useState("");
  const qc = useQueryClient();

  const isEnum = ENUM_TYPES.includes(formValue.type as (typeof ENUM_TYPES)[number]);

  function resetForm() {
    setFormValue({
      attributeKey: "",
      type: "TEXT",
      unitName: "",
      allowedValues: [],
      required: false,
      sortOrder: String(nextSortOrder),
    });
    setNewValueInput("");
  }

  const mutation = useMutation({
    mutationFn: () => {
      const body: CreateBlueprintEntryRequest = {
        attributeKey: formValue.attributeKey.trim(),
        type: formValue.type,
        required: formValue.required,
        sortOrder: parseInt(formValue.sortOrder, 10) || nextSortOrder,
        ...(formValue.unitName.trim() && { unitName: formValue.unitName.trim() }),
        ...(isEnum && formValue.allowedValues.length > 0 && { allowedValues: formValue.allowedValues }),
      };
      return createBlueprintEntry(categoryId, body);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["blueprint-admin", categoryId] });
      setOpen(false);
      resetForm();
      toast.success("Blueprint entry created");
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const canSubmit =
    formValue.attributeKey.trim().length > 0 &&
    (!isEnum || formValue.allowedValues.length > 0) &&
    !mutation.isPending;

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        setOpen(v);
        if (!v) resetForm();
      }}
    >
      <DialogTrigger asChild>
        <Button size="sm">
          <Plus className="h-4 w-4 mr-1" />
          Add Entry
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>New Blueprint Entry</DialogTitle>
        </DialogHeader>

        <BlueprintEntryForm
          mode="create"
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
          {mutation.isPending ? "Creating…" : "Create Entry"}
        </Button>
      </DialogContent>
    </Dialog>
  );
}
