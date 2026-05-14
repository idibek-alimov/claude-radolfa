"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Save, Loader2 } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { TagBadge } from "@/entities/tag";
import { fetchTags, setVariantTags } from "@/entities/tag/api";
import { getErrorMessage } from "@/shared/lib";
import type { Tag } from "@/entities/tag";

interface TagAssignmentCardProps {
  variantId: number;
  variantSlug: string;
  productBaseId: number;
  currentTags: Tag[];
}

export function TagAssignmentCard({
  variantId,
  variantSlug,
  productBaseId,
  currentTags,
}: TagAssignmentCardProps) {
  const [selected, setSelected] = useState<number[]>(
    currentTags.map((t) => t.id)
  );
  const qc = useQueryClient();

  const { data: allTags = [] } = useQuery({
    queryKey: ["tags"],
    queryFn: fetchTags,
  });

  const mutation = useMutation({
    mutationFn: () => setVariantTags(variantId, selected),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin-product", productBaseId] });
      qc.invalidateQueries({ queryKey: ["listing", variantSlug] });
      toast.success("Tags updated");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const toggle = (id: number) =>
    setSelected((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );

  const isDirty =
    selected.slice().sort().join() !==
    currentTags
      .map((t) => t.id)
      .slice()
      .sort()
      .join();

  return (
    <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
        Tags
      </h2>

      {allTags.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          No tags exist yet. Create some in the Tags admin panel.
        </p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {allTags.map((tag) => (
            <button
              key={tag.id}
              type="button"
              onClick={() => toggle(tag.id)}
              className={`rounded transition-opacity ${
                selected.includes(tag.id)
                  ? "opacity-100 ring-2 ring-offset-1 ring-foreground"
                  : "opacity-40"
              }`}
            >
              <TagBadge tag={tag} size="md" />
            </button>
          ))}
        </div>
      )}

      <div className="flex justify-end">
        <Button
          onClick={() => mutation.mutate()}
          disabled={mutation.isPending || !isDirty}
        >
          {mutation.isPending ? (
            <>
              <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
              Saving
            </>
          ) : (
            <>
              <Save className="h-4 w-4 mr-1.5" />
              Save Tags
            </>
          )}
        </Button>
      </div>
    </div>
  );
}
