"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { fetchTags, TagBadge } from "@/entities/tag";
import { deleteTag } from "@/entities/tag/api";
import { Button } from "@/shared/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/shared/ui/alert-dialog";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/shared/ui/tooltip";
import { Skeleton } from "@/shared/ui/skeleton";
import { getErrorMessage } from "@/shared/lib";
import { CreateTagDialog } from "./CreateTagDialog";
import { EditTagDialog } from "./EditTagDialog";
import type { Tag } from "@/entities/tag";

export function TagListPanel() {
  const [editingTag, setEditingTag] = useState<Tag | null>(null);
  const [deletingTag, setDeletingTag] = useState<Tag | null>(null);
  const qc = useQueryClient();

  const { data: tags = [], isLoading } = useQuery({
    queryKey: ["tags"],
    queryFn: fetchTags,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteTag(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tags"] });
      setDeletingTag(null);
      toast.success("Tag deleted");
    },
    onError: (err) => {
      setDeletingTag(null);
      toast.error(getErrorMessage(err));
    },
  });

  return (
    <TooltipProvider>
      <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Product Tags</h2>
          <CreateTagDialog />
        </div>

        {isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-9 w-full rounded-lg" />
            ))}
          </div>
        ) : tags.length === 0 ? (
          <div className="border border-dashed rounded-xl p-12 flex flex-col items-center text-center">
            <p className="text-sm text-muted-foreground">No tags yet. Create your first tag above.</p>
          </div>
        ) : (
          <div className="space-y-2">
            {tags.map((tag) => (
              <div
                key={tag.id}
                className="flex items-center justify-between px-3 py-2 rounded-lg hover:bg-muted/50 transition-colors"
              >
                <TagBadge tag={tag} size="md" />
                <div className="flex items-center gap-1">
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setEditingTag(tag)}
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>Edit tag</TooltipContent>
                  </Tooltip>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-rose-500 hover:text-rose-600 hover:bg-rose-50"
                        onClick={() => setDeletingTag(tag)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>Delete tag</TooltipContent>
                  </Tooltip>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {editingTag && (
        <EditTagDialog
          tag={editingTag}
          open={!!editingTag}
          onOpenChange={(open) => { if (!open) setEditingTag(null); }}
        />
      )}

      <AlertDialog open={!!deletingTag} onOpenChange={(open) => { if (!open) setDeletingTag(null); }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete &quot;{deletingTag?.name}&quot;?</AlertDialogTitle>
            <AlertDialogDescription>
              This tag will be permanently removed. This action cannot be undone.
              If any product variants are still using this tag, the deletion will be blocked.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="bg-rose-600 hover:bg-rose-700 text-white"
              onClick={() => deletingTag && deleteMutation.mutate(deletingTag.id)}
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </TooltipProvider>
  );
}
