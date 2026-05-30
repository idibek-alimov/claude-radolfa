"use client";

import { useQuery } from "@tanstack/react-query";
import { BookOpen, Loader2 } from "lucide-react";
import { fetchAdminBlueprint } from "@/features/product-creation/api/blueprint";
import { BlueprintEntryRow } from "./BlueprintEntryRow";
import { CreateBlueprintEntryDialog } from "./CreateBlueprintEntryDialog";

interface Props {
  categoryId: number;
  categoryName: string;
}

export function BlueprintManagementPanel({ categoryId, categoryName }: Props) {
  const { data: entries = [], isLoading } = useQuery({
    queryKey: ["blueprint-admin", categoryId],
    queryFn: () => fetchAdminBlueprint(categoryId),
  });

  const nextSortOrder =
    entries.length > 0
      ? Math.max(...entries.map((e) => e.sortOrder)) + 1
      : 1;

  return (
    <div className="space-y-3 pt-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <BookOpen className="h-4 w-4" />
          <span>
            Blueprint for{" "}
            <strong className="text-foreground">{categoryName}</strong>
          </span>
        </div>
        <CreateBlueprintEntryDialog
          categoryId={categoryId}
          nextSortOrder={nextSortOrder}
        />
      </div>

      {isLoading ? (
        <div className="flex justify-center py-6">
          <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
        </div>
      ) : entries.length === 0 ? (
        <p className="text-sm text-muted-foreground py-4 text-center">
          No blueprint entries yet. Add the first attribute.
        </p>
      ) : (
        <div className="space-y-2">
          {[...entries]
            .sort((a, b) => a.sortOrder - b.sortOrder)
            .map((entry) => (
              <BlueprintEntryRow
                key={entry.id}
                entry={entry}
                categoryId={categoryId}
              />
            ))}
        </div>
      )}
    </div>
  );
}
