"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchTags, TagBadge } from "@/entities/tag";
import { CreateTagDialog } from "./CreateTagDialog";

export function TagListPanel() {
  const { data: tags = [], isLoading } = useQuery({
    queryKey: ["tags"],
    queryFn: fetchTags,
  });

  return (
    <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Product Tags</h2>
        <CreateTagDialog />
      </div>
      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading…</p>
      ) : tags.length === 0 ? (
        <p className="text-sm text-muted-foreground">No tags yet.</p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {tags.map((tag) => (
            <TagBadge key={tag.id} tag={tag} size="md" />
          ))}
        </div>
      )}
    </div>
  );
}
