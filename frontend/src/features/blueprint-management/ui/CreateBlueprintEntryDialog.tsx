"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, X } from "lucide-react";
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
import { createBlueprintEntry } from "@/features/product-creation/api/blueprint";
import type {
  BlueprintFieldType,
  CreateBlueprintEntryRequest,
} from "@/features/product-creation/model/types";
import { getErrorMessage } from "@/shared/lib";

interface Props {
  categoryId: number;
  nextSortOrder: number;
}

const FIELD_TYPES: { value: BlueprintFieldType; label: string }[] = [
  { value: "ENUM_SINGLE", label: "Single Choice" },
  { value: "ENUM_MULTI", label: "Multi Choice" },
  { value: "TEXT", label: "Text" },
  { value: "NUMERIC", label: "Numeric" },
];

const ENUM_TYPES: BlueprintFieldType[] = ["ENUM_SINGLE", "ENUM_MULTI"];

export function CreateBlueprintEntryDialog({ categoryId, nextSortOrder }: Props) {
  const [open, setOpen] = useState(false);
  const [attributeKey, setAttributeKey] = useState("");
  const [type, setType] = useState<BlueprintFieldType>("TEXT");
  const [unitName, setUnitName] = useState("");
  const [required, setRequired] = useState(false);
  const [sortOrder, setSortOrder] = useState(String(nextSortOrder));
  const [allowedValues, setAllowedValues] = useState<string[]>([]);
  const [newValue, setNewValue] = useState("");
  const qc = useQueryClient();

  const isEnum = ENUM_TYPES.includes(type);

  function resetForm() {
    setAttributeKey("");
    setType("TEXT");
    setUnitName("");
    setRequired(false);
    setSortOrder(String(nextSortOrder));
    setAllowedValues([]);
    setNewValue("");
  }

  function addAllowedValue() {
    const trimmed = newValue.trim();
    if (trimmed && !allowedValues.includes(trimmed)) {
      setAllowedValues((prev) => [...prev, trimmed]);
    }
    setNewValue("");
  }

  function removeAllowedValue(v: string) {
    setAllowedValues((prev) => prev.filter((x) => x !== v));
  }

  const mutation = useMutation({
    mutationFn: () => {
      const body: CreateBlueprintEntryRequest = {
        attributeKey: attributeKey.trim(),
        type,
        required,
        sortOrder: parseInt(sortOrder, 10) || nextSortOrder,
        ...(unitName.trim() && { unitName: unitName.trim() }),
        ...(isEnum && allowedValues.length > 0 && { allowedValues }),
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
    attributeKey.trim().length > 0 &&
    (!isEnum || allowedValues.length > 0) &&
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

        <div className="space-y-4">
          {/* Attribute key */}
          <div className="space-y-1.5">
            <Label>Attribute Key</Label>
            <Input
              value={attributeKey}
              onChange={(e) => setAttributeKey(e.target.value)}
              maxLength={128}
              placeholder="e.g. Material"
            />
          </div>

          {/* Type */}
          <div className="space-y-1.5">
            <Label>Type</Label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value as BlueprintFieldType)}
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              {FIELD_TYPES.map((ft) => (
                <option key={ft.value} value={ft.value}>
                  {ft.label}
                </option>
              ))}
            </select>
          </div>

          {/* Unit name — only for NUMERIC */}
          {type === "NUMERIC" && (
            <div className="space-y-1.5">
              <Label>
                Unit{" "}
                <span className="text-xs text-muted-foreground font-normal">
                  (optional)
                </span>
              </Label>
              <Input
                value={unitName}
                onChange={(e) => setUnitName(e.target.value)}
                maxLength={64}
                placeholder="e.g. kg, cm"
              />
            </div>
          )}

          {/* Allowed values — only for ENUM types */}
          {isEnum && (
            <div className="space-y-1.5">
              <Label>
                Allowed Values{" "}
                <span className="text-xs text-muted-foreground font-normal">
                  (at least one)
                </span>
              </Label>
              <div className="flex gap-2">
                <Input
                  value={newValue}
                  onChange={(e) => setNewValue(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      addAllowedValue();
                    }
                  }}
                  placeholder="Type and press Enter"
                />
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={addAllowedValue}
                >
                  Add
                </Button>
              </div>
              {allowedValues.length > 0 && (
                <div className="flex flex-wrap gap-1.5 mt-1">
                  {allowedValues.map((v) => (
                    <span
                      key={v}
                      className="inline-flex items-center gap-1 bg-muted rounded px-2 py-0.5 text-xs"
                    >
                      {v}
                      <button
                        type="button"
                        onClick={() => removeAllowedValue(v)}
                        className="text-muted-foreground hover:text-destructive"
                        aria-label={`Remove ${v}`}
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </span>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Sort order + Required */}
          <div className="flex gap-4 items-end">
            <div className="space-y-1.5 w-24">
              <Label>Sort Order</Label>
              <Input
                type="number"
                min={0}
                value={sortOrder}
                onChange={(e) => setSortOrder(e.target.value)}
              />
            </div>
            <label className="flex items-center gap-2 cursor-pointer select-none pb-1">
              <input
                type="checkbox"
                checked={required}
                onChange={(e) => setRequired(e.target.checked)}
                className="h-4 w-4 rounded border-input"
              />
              <span className="text-sm">Required</span>
            </label>
          </div>

          <Button
            className="w-full"
            disabled={!canSubmit}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? "Creating…" : "Create Entry"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
