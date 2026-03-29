"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { deleteBlueprintEntry } from "@/features/product-creation/api/blueprint";
import type { AdminBlueprintEntry } from "@/features/product-creation/model/types";
import { getErrorMessage } from "@/shared/lib";

interface Props {
  entry: AdminBlueprintEntry;
  categoryId: number;
}

const TYPE_LABELS: Record<string, string> = {
  ENUM_SINGLE: "Single Choice",
  ENUM_MULTI: "Multi Choice",
  TEXT: "Text",
  NUMERIC: "Numeric",
};

export function BlueprintEntryRow({ entry, categoryId }: Props) {
  const qc = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => deleteBlueprintEntry(categoryId, entry.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["blueprint-admin", categoryId] });
      toast.success(`"${entry.attributeKey}" removed`);
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className="flex items-center justify-between gap-3 rounded-lg border px-4 py-3">
      <div className="min-w-0 space-y-0.5">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-sm font-medium">{entry.attributeKey}</span>
          {entry.required && (
            <Badge variant="destructive" className="text-[10px] px-1.5 py-0">
              Required
            </Badge>
          )}
          <Badge variant="secondary" className="text-[10px] px-1.5 py-0">
            {TYPE_LABELS[entry.type] ?? entry.type}
          </Badge>
          {entry.unitName && (
            <span className="text-xs text-muted-foreground">
              ({entry.unitName})
            </span>
          )}
        </div>
        {entry.allowedValues.length > 0 && (
          <p className="text-xs text-muted-foreground truncate">
            Values: {entry.allowedValues.join(", ")}
          </p>
        )}
      </div>

      <Button
        size="icon"
        variant="ghost"
        className="shrink-0 text-destructive hover:text-destructive hover:bg-destructive/10"
        disabled={mutation.isPending}
        onClick={() => mutation.mutate()}
        aria-label={`Delete ${entry.attributeKey}`}
      >
        <Trash2 className="h-4 w-4" />
      </Button>
    </div>
  );
}
