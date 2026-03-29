"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { updateDiscountType, deleteDiscountType } from "../api";
import type { DiscountType } from "../model/types";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";

interface DiscountTypeRowProps {
  discountType: DiscountType;
}

export function DiscountTypeRow({ discountType }: DiscountTypeRowProps) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(discountType.name);
  const [rank, setRank] = useState(discountType.rank);
  const qc = useQueryClient();

  const updateMutation = useMutation({
    mutationFn: () => updateDiscountType(discountType.id, { name, rank }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["discount-types"] });
      setEditing(false);
      toast.success("Type updated");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteDiscountType(discountType.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["discount-types"] });
      toast.success("Type deleted");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (editing) {
    return (
      <div className="flex items-center gap-2 py-1.5">
        <Input
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="h-8 w-44 text-sm"
          maxLength={128}
        />
        <Input
          type="number"
          value={rank}
          onChange={(e) => setRank(Number(e.target.value))}
          className="h-8 w-20 text-sm"
          min={0}
        />
        <Button
          size="sm"
          onClick={() => updateMutation.mutate()}
          disabled={updateMutation.isPending || !name.trim()}
        >
          Save
        </Button>
        <Button
          size="sm"
          variant="ghost"
          onClick={() => {
            setName(discountType.name);
            setRank(discountType.rank);
            setEditing(false);
          }}
        >
          Cancel
        </Button>
      </div>
    );
  }

  return (
    <div className="flex items-center gap-3 py-1.5 group">
      <span className="font-medium text-sm w-44 truncate">{discountType.name}</span>
      <span className="text-sm text-muted-foreground w-20">Rank: {discountType.rank}</span>
      <Button
        size="sm"
        variant="ghost"
        className="opacity-0 group-hover:opacity-100"
        onClick={() => setEditing(true)}
      >
        Edit
      </Button>
      <Button
        size="sm"
        variant="ghost"
        className="opacity-0 group-hover:opacity-100 text-destructive hover:text-destructive hover:bg-destructive/10"
        onClick={() => deleteMutation.mutate()}
        disabled={deleteMutation.isPending}
      >
        Delete
      </Button>
    </div>
  );
}
