"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { getErrorMessage } from "@/shared/lib";
import { useWarehouseZones, useShelvesByZone, useBinsByShelf } from "@/entities/warehouse-location";
import { usePutaway } from "../api";
import type { InboundQueueItem } from "../types";

interface Props {
  item: InboundQueueItem | null;
  onClose: () => void;
}

export function PutawayDialog({ item, onClose }: Props) {
  const t = useTranslations("warehouse");
  const [selectedZoneId, setSelectedZoneId] = useState<number | null>(null);
  const [selectedShelfId, setSelectedShelfId] = useState<number | null>(null);
  const [selectedBinId, setSelectedBinId] = useState<number | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [remaining, setRemaining] = useState(0);

  const { data: zones, isLoading: zonesLoading } = useWarehouseZones();
  const { data: shelves, isLoading: shelvesLoading } = useShelvesByZone(selectedZoneId);
  const { data: bins, isLoading: binsLoading } = useBinsByShelf(selectedShelfId);
  const putaway = usePutaway();

  // Reset cascade and remaining count whenever a different item is opened
  useEffect(() => {
    if (item) {
      setSelectedZoneId(null);
      setSelectedShelfId(null);
      setSelectedBinId(null);
      setQuantity(1);
      setRemaining(item.unassignedQuantity);
    }
  }, [item]);

  function handleZoneChange(value: string) {
    setSelectedZoneId(Number(value));
    setSelectedShelfId(null);
    setSelectedBinId(null);
  }

  function handleShelfChange(value: string) {
    setSelectedShelfId(Number(value));
    setSelectedBinId(null);
  }

  function handleQuantityChange(value: string) {
    const parsed = parseInt(value, 10);
    if (isNaN(parsed)) { setQuantity(1); return; }
    setQuantity(Math.min(Math.max(1, parsed), remaining));
  }

  function handleSubmit() {
    if (!item || !selectedBinId || quantity < 1 || quantity > remaining) return;

    const submittedQty = quantity;
    putaway.mutate(
      { skuId: item.skuId, binId: selectedBinId, quantity: submittedQty },
      {
        onSuccess: () => {
          toast.success(t("putaway.success"));
          const newRemaining = remaining - submittedQty;
          setRemaining(newRemaining);
          // Reset cascade and qty for the next bin
          setSelectedZoneId(null);
          setSelectedShelfId(null);
          setSelectedBinId(null);
          setQuantity(1);
          if (newRemaining <= 0) {
            onClose();
          }
        },
        onError: (err: unknown) => {
          toast.error(getErrorMessage(err, t("putaway.errors.generic")));
        },
      },
    );
  }

  function handleOpenChange(open: boolean) {
    if (!open) onClose();
  }

  const shelfPlaceholder = !selectedZoneId
    ? t("putaway.dialog.selectZone")
    : shelvesLoading
      ? t("common.loading")
      : t("putaway.dialog.selectShelf");

  const binPlaceholder = !selectedShelfId
    ? t("putaway.dialog.selectShelf")
    : binsLoading
      ? t("common.loading")
      : t("putaway.dialog.selectBin");

  const canSubmit = !!selectedBinId && quantity >= 1 && quantity <= remaining && !putaway.isPending;

  return (
    <Dialog open={!!item} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{t("putaway.dialog.title")}</DialogTitle>
          {item && (
            <p className="text-sm text-muted-foreground pt-1">
              <span className="font-medium text-foreground">{item.productName}</span>
              {" · "}
              <span className="font-mono">{item.skuCode}</span>
              {" · "}
              {t("putaway.dialog.remaining", { count: remaining })}
            </p>
          )}
        </DialogHeader>

        <div className="space-y-5 py-2">
          {/* Zone */}
          <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">{t("putaway.dialog.zone")}</Label>
            <Select
              value={selectedZoneId != null ? String(selectedZoneId) : undefined}
              onValueChange={handleZoneChange}
              disabled={zonesLoading}
            >
              <SelectTrigger>
                <SelectValue
                  placeholder={zonesLoading ? t("common.loading") : t("putaway.dialog.selectZone")}
                />
              </SelectTrigger>
              <SelectContent>
                {zones?.map((zone) => (
                  <SelectItem key={zone.id} value={String(zone.id)}>
                    {zone.code} — {zone.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Shelf */}
          <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">{t("putaway.dialog.shelf")}</Label>
            <Select
              value={selectedShelfId != null ? String(selectedShelfId) : undefined}
              onValueChange={handleShelfChange}
              disabled={!selectedZoneId || shelvesLoading}
            >
              <SelectTrigger>
                <SelectValue placeholder={shelfPlaceholder} />
              </SelectTrigger>
              <SelectContent>
                {shelves?.map((shelf) => (
                  <SelectItem key={shelf.id} value={String(shelf.id)}>
                    {shelf.code} — {shelf.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Bin */}
          <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">{t("putaway.dialog.bin")}</Label>
            <Select
              value={selectedBinId != null ? String(selectedBinId) : undefined}
              onValueChange={(v) => setSelectedBinId(Number(v))}
              disabled={!selectedShelfId || binsLoading}
            >
              <SelectTrigger>
                <SelectValue placeholder={binPlaceholder} />
              </SelectTrigger>
              <SelectContent>
                {bins?.map((bin) => (
                  <SelectItem key={bin.id} value={String(bin.id)}>
                    {bin.code}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Quantity */}
          <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">{t("putaway.dialog.quantity")}</Label>
            <Input
              type="number"
              min={1}
              max={remaining}
              value={quantity}
              onChange={(e) => handleQuantityChange(e.target.value)}
            />
          </div>
        </div>

        <div className="flex gap-2 justify-end pt-2">
          <Button variant="outline" onClick={onClose} disabled={putaway.isPending}>
            {t("common.cancel")}
          </Button>
          <Button onClick={handleSubmit} disabled={!canSubmit}>
            {putaway.isPending && <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />}
            {t("putaway.dialog.submit")}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
