"use client";

import { Loader2, Save, Undo2 } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { useDraft } from "../model/ProductCardDraftContext";

interface Props {
  onSave: () => void;
  onDiscard: () => void;
  isSaving: boolean;
}

export function SaveBar({ onSave, onDiscard, isSaving }: Props) {
  const { isDirty, diff } = useDraft();

  if (!isDirty) return null;

  return (
    <div className="sticky bottom-0 z-30 bg-card border-t shadow-lg px-8 py-3 flex items-center justify-between">
      <span className="text-sm text-muted-foreground">
        {diff.length} unsaved change{diff.length !== 1 ? "s" : ""}
      </span>
      <div className="flex gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={onDiscard}
          disabled={isSaving}
        >
          <Undo2 className="h-3.5 w-3.5 mr-1.5" />
          Discard
        </Button>
        <Button size="sm" onClick={onSave} disabled={isSaving}>
          {isSaving ? (
            <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />
          ) : (
            <Save className="h-3.5 w-3.5 mr-1.5" />
          )}
          Save Changes
        </Button>
      </div>
    </div>
  );
}
