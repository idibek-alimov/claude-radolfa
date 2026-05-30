"use client";

import { X } from "lucide-react";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import type { BlueprintFieldType } from "@/features/product-creation/model/types";

export interface BlueprintFormValue {
  attributeKey: string;
  type: BlueprintFieldType;
  unitName: string;
  allowedValues: string[];
  required: boolean;
  sortOrder: string;
}

interface Props {
  mode: "create" | "edit";
  value: BlueprintFormValue;
  onChange: (patch: Partial<BlueprintFormValue>) => void;
  newValueInput: string;
  onNewValueInputChange: (v: string) => void;
}

const FIELD_TYPES: { value: BlueprintFieldType; label: string }[] = [
  { value: "ENUM_SINGLE", label: "Single Choice" },
  { value: "ENUM_MULTI", label: "Multi Choice" },
  { value: "TEXT", label: "Text" },
  { value: "NUMERIC", label: "Numeric" },
];

const TYPE_LABELS: Record<string, string> = {
  ENUM_SINGLE: "Single Choice",
  ENUM_MULTI: "Multi Choice",
  TEXT: "Text",
  NUMERIC: "Numeric",
};

const ENUM_TYPES: BlueprintFieldType[] = ["ENUM_SINGLE", "ENUM_MULTI"];

export function BlueprintEntryForm({ mode, value, onChange, newValueInput, onNewValueInputChange }: Props) {
  const isEnum = ENUM_TYPES.includes(value.type);
  const isNumeric = value.type === "NUMERIC";

  function addAllowedValue() {
    const trimmed = newValueInput.trim();
    if (trimmed && !value.allowedValues.includes(trimmed)) {
      onChange({ allowedValues: [...value.allowedValues, trimmed] });
    }
    onNewValueInputChange("");
  }

  function removeAllowedValue(v: string) {
    onChange({ allowedValues: value.allowedValues.filter((x) => x !== v) });
  }

  return (
    <div className="space-y-4">
      {/* Attribute key */}
      <div className="space-y-1.5">
        <Label>Attribute Key</Label>
        <Input
          value={value.attributeKey}
          onChange={(e) => onChange({ attributeKey: e.target.value })}
          maxLength={128}
          placeholder="e.g. Material"
        />
      </div>

      {/* Type — editable in create, read-only in edit */}
      <div className="space-y-1.5">
        <Label>Type</Label>
        {mode === "create" ? (
          <select
            value={value.type}
            onChange={(e) => onChange({ type: e.target.value as BlueprintFieldType, unitName: "", allowedValues: [] })}
            className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            {FIELD_TYPES.map((ft) => (
              <option key={ft.value} value={ft.value}>
                {ft.label}
              </option>
            ))}
          </select>
        ) : (
          <div className="flex items-center h-9">
            <Badge variant="secondary" className="text-sm">
              {TYPE_LABELS[value.type] ?? value.type}
            </Badge>
            <span className="ml-2 text-xs text-muted-foreground">Cannot be changed</span>
          </div>
        )}
      </div>

      {/* Unit name — only for NUMERIC */}
      {isNumeric && (
        <div className="space-y-1.5">
          <Label>
            Unit{" "}
            <span className="text-xs text-muted-foreground font-normal">(optional)</span>
          </Label>
          <Input
            value={value.unitName}
            onChange={(e) => onChange({ unitName: e.target.value })}
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
            <span className="text-xs text-muted-foreground font-normal">(at least one)</span>
          </Label>
          <div className="flex gap-2">
            <Input
              value={newValueInput}
              onChange={(e) => onNewValueInputChange(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  addAllowedValue();
                }
              }}
              placeholder="Type and press Enter"
            />
            <Button type="button" variant="outline" size="sm" onClick={addAllowedValue}>
              Add
            </Button>
          </div>
          {value.allowedValues.length > 0 && (
            <div className="flex flex-wrap gap-1.5 mt-1">
              {value.allowedValues.map((v) => (
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
            value={value.sortOrder}
            onChange={(e) => onChange({ sortOrder: e.target.value })}
          />
        </div>
        <label className="flex items-center gap-2 cursor-pointer select-none pb-1">
          <input
            type="checkbox"
            checked={value.required}
            onChange={(e) => onChange({ required: e.target.checked })}
            className="h-4 w-4 rounded border-input"
          />
          <span className="text-sm">Required</span>
        </label>
      </div>
    </div>
  );
}
