"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Pencil, Trash2, Sparkles } from "lucide-react";
import { fetchReviewTraits, deleteReviewTrait } from "@/entities/review-trait";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/table";
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
import { Badge } from "@/shared/ui/badge";
import { getErrorMessage } from "@/shared/lib";
import { CreateReviewTraitDialog } from "./CreateReviewTraitDialog";
import { EditReviewTraitDialog } from "./EditReviewTraitDialog";
import type { ReviewTrait } from "@/entities/review-trait";

export function ReviewTraitsPanel() {
  const [editingTrait, setEditingTrait] = useState<ReviewTrait | null>(null);
  const [deletingTrait, setDeletingTrait] = useState<ReviewTrait | null>(null);
  const qc = useQueryClient();

  const { data: traits = [], isLoading } = useQuery({
    queryKey: ["review-traits"],
    queryFn: fetchReviewTraits,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteReviewTrait(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["review-traits"] });
      setDeletingTrait(null);
      toast.success("Trait deleted");
    },
    onError: (err) => {
      setDeletingTrait(null);
      toast.error(getErrorMessage(err));
    },
  });

  return (
    <TooltipProvider>
      <div className="bg-card rounded-xl border shadow-sm">
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <h2 className="text-sm font-semibold">Trait Bank</h2>
          <CreateReviewTraitDialog />
        </div>

        {isLoading ? (
          <div className="p-6 space-y-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full rounded-lg" />
            ))}
          </div>
        ) : traits.length === 0 ? (
          <div className="border border-dashed rounded-xl m-6 p-12 flex flex-col items-center gap-3 text-center">
            <Sparkles className="h-10 w-10 text-muted-foreground/40" />
            <p className="text-sm text-muted-foreground">
              No review traits defined yet. Create your first trait to get started.
            </p>
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Key</TableHead>
                <TableHead>Label</TableHead>
                <TableHead>Input Type</TableHead>
                <TableHead className="w-24 text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {traits.map((trait) => (
                <TableRow key={trait.id}>
                  <TableCell>
                    <code className="text-xs bg-muted px-2 py-0.5 rounded font-mono">
                      {trait.key}
                    </code>
                  </TableCell>
                  <TableCell className="font-medium">{trait.labelI18n}</TableCell>
                  <TableCell>
                    <Badge variant={trait.inputType === "SLIDER" ? "default" : "secondary"}>
                      {trait.inputType === "SLIDER" ? "Slider (1–5)" : "Radio"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-1">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setEditingTrait(trait)}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>Edit trait</TooltipContent>
                      </Tooltip>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-rose-500 hover:text-rose-600 hover:bg-rose-50"
                            onClick={() => setDeletingTrait(trait)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>Delete trait</TooltipContent>
                      </Tooltip>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      {editingTrait && (
        <EditReviewTraitDialog
          trait={editingTrait}
          open={!!editingTrait}
          onOpenChange={(open) => { if (!open) setEditingTrait(null); }}
        />
      )}

      <AlertDialog open={!!deletingTrait} onOpenChange={(open) => { if (!open) setDeletingTrait(null); }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete &quot;{deletingTrait?.labelI18n}&quot;?</AlertDialogTitle>
            <AlertDialogDescription>
              This trait will be permanently removed from the bank and unlinked from all categories.
              Existing reviews that recorded answers for this trait will retain their data,
              but the trait will no longer be shown to new reviewers.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="bg-rose-600 hover:bg-rose-700 text-white"
              onClick={() => deletingTrait && deleteMutation.mutate(deletingTrait.id)}
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </TooltipProvider>
  );
}
