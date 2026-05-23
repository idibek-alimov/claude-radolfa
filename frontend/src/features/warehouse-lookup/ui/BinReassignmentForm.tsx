"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { Button } from "@/shared/ui/button";
import { Label } from "@/shared/ui/label";
import { getErrorMessage } from "@/shared/lib";
import { useWarehouseZones, useShelvesByZone, useBinsByShelf, useAssignSkuToBin } from "../api";

interface Props {
  skuId: number;
  onSuccess: (binLabel: string) => void;
  onCancel: () => void;
}

export function BinReassignmentForm({ skuId, onSuccess, onCancel }: Props) {
  const t = useTranslations("warehouse");
  const [selectedZoneId, setSelectedZoneId] = useState<number | null>(null);
  const [selectedShelfId, setSelectedShelfId] = useState<number | null>(null);
  const [selectedBinId, setSelectedBinId] = useState<number | null>(null);

  const { data: zones, isLoading: zonesLoading } = useWarehouseZones();
  const { data: shelves, isLoading: shelvesLoading } = useShelvesByZone(selectedZoneId);
  const { data: bins, isLoading: binsLoading } = useBinsByShelf(selectedShelfId);
  const assign = useAssignSkuToBin();

  const selectedZone = zones?.find((z) => z.id === selectedZoneId);
  const selectedShelf = shelves?.find((s) => s.id === selectedShelfId);
  const selectedBin = bins?.find((b) => b.id === selectedBinId);

  function handleZoneChange(value: string) {
    setSelectedZoneId(Number(value));
    setSelectedShelfId(null);
    setSelectedBinId(null);
  }

  function handleShelfChange(value: string) {
    setSelectedShelfId(Number(value));
    setSelectedBinId(null);
  }

  function handleSave() {
    if (!selectedBinId || !selectedZone || !selectedShelf || !selectedBin) return;

    assign.mutate(
      { skuId, binId: selectedBinId },
      {
        onSuccess: () => {
          toast.success(t("lookup.reassign.success"));
          onSuccess(`${selectedZone.code} / ${selectedShelf.code} / ${selectedBin.code}`);
        },
        onError: (err) => {
          toast.error(getErrorMessage(err, t("lookup.reassign.error")));
        },
      },
    );
  }

  const shelfPlaceholder = !selectedZoneId
    ? t("lookup.reassign.selectZone")
    : shelvesLoading
      ? t("common.loading")
      : t("lookup.reassign.selectShelf");

  const binPlaceholder = !selectedShelfId
    ? t("lookup.reassign.selectShelf")
    : binsLoading
      ? t("common.loading")
      : t("lookup.reassign.selectBin");

  return (
    <div className="border border-zinc-200 rounded-xl p-4 bg-zinc-50 space-y-4">
      <p className="text-sm font-medium text-zinc-700">{t("lookup.reassign.title")}</p>

      {/* Zone */}
      <div className="space-y-1.5">
        <Label className="text-xs text-muted-foreground">{t("lookup.reassign.zone")}</Label>
        <Select onValueChange={handleZoneChange} disabled={zonesLoading}>
          <SelectTrigger>
            <SelectValue
              placeholder={
                zonesLoading ? t("common.loading") : t("lookup.reassign.selectZone")
              }
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
        <Label className="text-xs text-muted-foreground">{t("lookup.reassign.shelf")}</Label>
        <Select
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
        <Label className="text-xs text-muted-foreground">{t("lookup.reassign.bin")}</Label>
        <Select
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

      {/* Actions */}
      <div className="flex gap-2 pt-1">
        <Button size="sm" disabled={!selectedBinId || assign.isPending} onClick={handleSave}>
          {assign.isPending && <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />}
          {t("common.save")}
        </Button>
        <Button size="sm" variant="outline" onClick={onCancel} disabled={assign.isPending}>
          {t("common.cancel")}
        </Button>
      </div>
    </div>
  );
}
