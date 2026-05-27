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
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { getErrorMessage } from "@/shared/lib";
import { useWarehouseZones, useShelvesByZone, useBinsByShelf } from "@/entities/warehouse-location";
import type { Placement } from "@/entities/warehouse-sku";
import { useRelocateStock } from "../api";

interface Props {
  skuId: number;
  placements: Placement[];
  onSuccess: () => void;
  onCancel: () => void;
}

export function RelocateForm({ skuId, placements, onSuccess, onCancel }: Props) {
  const t = useTranslations("warehouse");

  const binPlacements = placements.filter((p) => p.binId != null);

  const [fromBinId, setFromBinId] = useState<number | null>(null);
  const [selectedZoneId, setSelectedZoneId] = useState<number | null>(null);
  const [selectedShelfId, setSelectedShelfId] = useState<number | null>(null);
  const [toBinId, setToBinId] = useState<number | null>(null);
  const [quantity, setQuantity] = useState(1);

  const sourceQty = binPlacements.find((p) => p.binId === fromBinId)?.quantity ?? 1;

  const { data: zones, isLoading: zonesLoading } = useWarehouseZones();
  const { data: shelves, isLoading: shelvesLoading } = useShelvesByZone(selectedZoneId);
  const { data: bins, isLoading: binsLoading } = useBinsByShelf(selectedShelfId);
  const relocate = useRelocateStock();

  function handleFromChange(value: string) {
    setFromBinId(Number(value));
    setQuantity(1);
  }

  function handleZoneChange(value: string) {
    setSelectedZoneId(Number(value));
    setSelectedShelfId(null);
    setToBinId(null);
  }

  function handleShelfChange(value: string) {
    setSelectedShelfId(Number(value));
    setToBinId(null);
  }

  function handleQuantityChange(value: string) {
    const parsed = parseInt(value, 10);
    if (isNaN(parsed)) { setQuantity(1); return; }
    setQuantity(Math.min(Math.max(1, parsed), sourceQty));
  }

  function handleSave() {
    if (!fromBinId || !toBinId || fromBinId === toBinId || quantity < 1 || quantity > sourceQty) return;

    relocate.mutate(
      { skuId, fromBinId, toBinId, quantity },
      {
        onSuccess: () => {
          toast.success(t("lookup.relocate.success"));
          onSuccess();
        },
        onError: (err: unknown) => {
          toast.error(getErrorMessage(err, t("lookup.relocate.errors.generic")));
        },
      },
    );
  }

  const shelfPlaceholder = !selectedZoneId
    ? t("lookup.relocate.selectZone")
    : shelvesLoading
      ? t("common.loading")
      : t("lookup.relocate.selectShelf");

  const toBinPlaceholder = !selectedShelfId
    ? t("lookup.relocate.selectShelf")
    : binsLoading
      ? t("common.loading")
      : t("lookup.relocate.selectBin");

  const canSave =
    !!fromBinId &&
    !!toBinId &&
    fromBinId !== toBinId &&
    quantity >= 1 &&
    quantity <= sourceQty &&
    !relocate.isPending;

  return (
    <div className="border border-zinc-200 rounded-xl p-4 bg-zinc-50 space-y-4">
      <p className="text-sm font-medium text-zinc-700">{t("lookup.relocate.title")}</p>

      {/* From bin */}
      <div className="space-y-1.5">
        <Label className="text-xs text-muted-foreground">{t("lookup.relocate.from")}</Label>
        <Select onValueChange={handleFromChange}>
          <SelectTrigger>
            <SelectValue placeholder={t("lookup.relocate.selectFrom")} />
          </SelectTrigger>
          <SelectContent>
            {binPlacements.map((p) => (
              <SelectItem key={p.binId!} value={String(p.binId!)}>
                {p.binLabel} — {p.quantity}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* To: Zone */}
      <div className="space-y-1.5">
        <Label className="text-xs text-muted-foreground">{t("lookup.relocate.zone")}</Label>
        <Select
          value={selectedZoneId != null ? String(selectedZoneId) : undefined}
          onValueChange={handleZoneChange}
          disabled={zonesLoading}
        >
          <SelectTrigger>
            <SelectValue
              placeholder={zonesLoading ? t("common.loading") : t("lookup.relocate.selectZone")}
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

      {/* To: Shelf */}
      <div className="space-y-1.5">
        <Label className="text-xs text-muted-foreground">{t("lookup.relocate.shelf")}</Label>
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

      {/* To: Bin */}
      <div className="space-y-1.5">
        <Label className="text-xs text-muted-foreground">{t("lookup.relocate.to")}</Label>
        <Select
          value={toBinId != null ? String(toBinId) : undefined}
          onValueChange={(v) => setToBinId(Number(v))}
          disabled={!selectedShelfId || binsLoading}
        >
          <SelectTrigger>
            <SelectValue placeholder={toBinPlaceholder} />
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
        <Label className="text-xs text-muted-foreground">{t("lookup.relocate.quantity")}</Label>
        <Input
          type="number"
          min={1}
          max={sourceQty}
          value={quantity}
          disabled={!fromBinId}
          onChange={(e) => handleQuantityChange(e.target.value)}
        />
      </div>

      <div className="flex gap-2 pt-1">
        <Button size="sm" disabled={!canSave} onClick={handleSave}>
          {relocate.isPending && <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />}
          {t("lookup.relocate.submit")}
        </Button>
        <Button size="sm" variant="outline" onClick={onCancel} disabled={relocate.isPending}>
          {t("common.cancel")}
        </Button>
      </div>
    </div>
  );
}
