"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Skeleton } from "@/shared/ui/skeleton";
import { fetchDiscountTypes, createDiscountType } from "../api";
import { DiscountTypeRow } from "./DiscountTypeRow";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";
import { Plus } from "lucide-react";

export function DiscountTypesPanel() {
  const [newName, setNewName] = useState("");
  const [newRank, setNewRank] = useState(0);
  const qc = useQueryClient();

  const { data: types = [], isLoading } = useQuery({
    queryKey: ["discount-types"],
    queryFn: fetchDiscountTypes,
  });

  const createMutation = useMutation({
    mutationFn: () => createDiscountType({ name: newName.trim(), rank: newRank }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["discount-types"] });
      setNewName("");
      setNewRank(0);
      toast.success("Discount type created");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className="bg-card rounded-xl border shadow-sm">
      <div className="px-6 py-4 border-b">
        <h2 className="text-lg font-semibold">Discount Types</h2>
      </div>

      <div className="p-6 space-y-1">
        {isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-9 w-full" />
            ))}
          </div>
        ) : types.length === 0 ? (
          <p className="text-sm text-muted-foreground py-2">No discount types yet.</p>
        ) : (
          [...types]
            .sort((a, b) => a.rank - b.rank)
            .map((t) => <DiscountTypeRow key={t.id} discountType={t} />)
        )}
      </div>

      {/* Create form */}
      <div className="px-6 pb-6 pt-2 border-t">
        <div className="flex items-end gap-2">
          <div className="space-y-1">
            <Label className="text-xs">New type name</Label>
            <Input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              maxLength={128}
              placeholder="e.g. Flash Sale"
              className="h-8 w-44 text-sm"
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Rank</Label>
            <Input
              type="number"
              value={newRank}
              onChange={(e) => setNewRank(Number(e.target.value))}
              min={0}
              className="h-8 w-20 text-sm"
            />
          </div>
          <Button
            size="sm"
            className="gap-1.5"
            disabled={!newName.trim() || createMutation.isPending}
            onClick={() => createMutation.mutate()}
          >
            <Plus className="h-3.5 w-3.5" />
            Add
          </Button>
        </div>
      </div>
    </div>
  );
}
