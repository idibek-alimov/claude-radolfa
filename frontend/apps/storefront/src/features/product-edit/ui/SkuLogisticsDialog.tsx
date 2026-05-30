"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { updateSkuDimensions } from "@/entities/product/api/admin";
import { getErrorMessage } from "@/shared/lib";
import type { ProductCardSku } from "@/entities/product/model/types";

interface Props {
  open: boolean;
  onClose: () => void;
  sku: ProductCardSku;
  productBaseId: number;
}

export function SkuLogisticsDialog({ open, onClose, sku, productBaseId }: Props) {
  const qc = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const [weightKg, setWeightKg] = useState(sku.weightKg?.toString() ?? "");
  const [lengthCm, setLengthCm] = useState(sku.lengthCm?.toString() ?? "");
  const [widthCm,  setWidthCm]  = useState(sku.widthCm?.toString()  ?? "");
  const [heightCm, setHeightCm] = useState(sku.heightCm?.toString() ?? "");

  function parseNullable(v: string): number | null {
    const n = parseFloat(v);
    return isNaN(n) ? null : n;
  }

  async function handleSave() {
    setIsPending(true);
    try {
      await updateSkuDimensions(sku.skuId, {
        weightKg: parseNullable(weightKg),
        lengthCm: parseNullable(lengthCm) ? Math.round(parseNullable(lengthCm)!) : null,
        widthCm:  parseNullable(widthCm)  ? Math.round(parseNullable(widthCm)!)  : null,
        heightCm: parseNullable(heightCm) ? Math.round(parseNullable(heightCm)!) : null,
      });
      await qc.invalidateQueries({ queryKey: ["admin-product", productBaseId] });
      toast.success(`SKU ${sku.sizeLabel} logistics updated.`);
      onClose();
    } catch (err) {
      toast.error(getErrorMessage(err, "Failed to update logistics"));
    } finally {
      setIsPending(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Logistics — SKU {sku.sizeLabel}</DialogTitle>
        </DialogHeader>

        <div className="py-2 space-y-4">
          <p className="text-xs text-muted-foreground">
            Catalog Data — editable by MANAGER and ADMIN. Leave blank to clear the value.
          </p>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5 col-span-2">
              <Label htmlFor="weight" className="text-sm">Weight (kg)</Label>
              <Input
                id="weight"
                type="number"
                min={0}
                step={0.001}
                value={weightKg}
                onChange={(e) => setWeightKg(e.target.value)}
                placeholder="e.g. 0.350"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="length" className="text-sm">Length (cm)</Label>
              <Input
                id="length"
                type="number"
                min={0}
                step={1}
                value={lengthCm}
                onChange={(e) => setLengthCm(e.target.value)}
                placeholder="e.g. 30"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="width" className="text-sm">Width (cm)</Label>
              <Input
                id="width"
                type="number"
                min={0}
                step={1}
                value={widthCm}
                onChange={(e) => setWidthCm(e.target.value)}
                placeholder="e.g. 20"
              />
            </div>
            <div className="space-y-1.5 col-span-2">
              <Label htmlFor="height" className="text-sm">Height (cm)</Label>
              <Input
                id="height"
                type="number"
                min={0}
                step={1}
                value={heightCm}
                onChange={(e) => setHeightCm(e.target.value)}
                placeholder="e.g. 5"
              />
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={isPending}>Cancel</Button>
          <Button onClick={handleSave} disabled={isPending}>
            {isPending ? "Saving…" : "Save"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
