"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/shared/ui/button";
import { Skeleton } from "@/shared/ui/skeleton";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/shared/ui/dialog";
import { fetchDiscountTypes, deleteDiscountType } from "../api";
import { DiscountTypeCard } from "./DiscountTypeCard";
import { DiscountTypeCreateDialog } from "./DiscountTypeCreateDialog";
import type { DiscountType } from "../model/types";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";
import { Plus, Loader2 } from "lucide-react";

export function DiscountTypesPanel() {
  const [createOpen, setCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<DiscountType | undefined>();
  const [deleteTarget, setDeleteTarget] = useState<DiscountType | null>(null);

  const qc = useQueryClient();

  const { data: types = [], isLoading } = useQuery({
    queryKey: ["discount-types"],
    queryFn: fetchDiscountTypes,
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteDiscountType(deleteTarget!.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["discount-types"] });
      toast.success("Discount type deleted");
      setDeleteTarget(null);
    },
    onError: (err: unknown) => {
      const axiosErr = err as { response?: { data?: { discountCount?: number } } };
      const count = axiosErr?.response?.data?.discountCount;
      if (count !== undefined) {
        toast.error(
          `Cannot delete — ${count} discount${count !== 1 ? "s" : ""} still use this type.`
        );
      } else {
        toast.error(getErrorMessage(err));
      }
      setDeleteTarget(null);
    },
  });

  const sorted = [...types].sort((a, b) => a.rank - b.rank);

  const openEdit = (type: DiscountType) => {
    setEditTarget(type);
    setCreateOpen(true);
  };

  const closeCreateDialog = () => {
    setCreateOpen(false);
    setEditTarget(undefined);
  };

  return (
    <div className="flex flex-col flex-1 min-h-0">
      {/* Header row */}
      <div className="flex items-start justify-between mb-5 shrink-0">
        <div>
          <h2 className="text-base font-semibold">Discount Types</h2>
          <p className="text-sm text-muted-foreground mt-0.5">
            Define the categories of discounts applied to campaigns.
          </p>
        </div>
        <Button
          className="gap-1.5 shrink-0"
          onClick={() => {
            setEditTarget(undefined);
            setCreateOpen(true);
          }}
        >
          <Plus className="h-4 w-4" />
          New Discount Type
        </Button>
      </div>

      {/* Card grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 flex-1 min-h-0 overflow-auto content-start">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-[88px] rounded-xl" />
          ))}
        </div>
      ) : sorted.length === 0 ? (
        <div className="rounded-xl border border-dashed bg-card p-12 text-center text-sm text-muted-foreground flex-1 min-h-0 flex items-center justify-center">
          No discount types yet. Create your first one.
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 flex-1 min-h-0 overflow-auto content-start pb-4">
          {sorted.map((type, index) => (
            <DiscountTypeCard
              key={type.id}
              discountType={type}
              index={index}
              onEdit={() => openEdit(type)}
              onDelete={() => setDeleteTarget(type)}
            />
          ))}
        </div>
      )}

      {/* Create / Edit dialog */}
      <DiscountTypeCreateDialog
        open={createOpen}
        onClose={closeCreateDialog}
        editTarget={editTarget}
      />

      {/* Delete confirmation */}
      <Dialog
        open={deleteTarget !== null}
        onOpenChange={(v) => !v && setDeleteTarget(null)}
      >
        <DialogContent className="sm:max-w-[400px]">
          <DialogHeader>
            <DialogTitle>Delete discount type?</DialogTitle>
            <DialogDescription>
              <strong className="text-foreground">{deleteTarget?.name}</strong> will
              be permanently removed. This cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeleteTarget(null)}
              disabled={deleteMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={deleteMutation.isPending}
              onClick={() => deleteMutation.mutate()}
            >
              {deleteMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                "Delete"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
