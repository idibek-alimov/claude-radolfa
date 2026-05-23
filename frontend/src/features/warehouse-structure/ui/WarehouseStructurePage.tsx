"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useWarehouseZones, useShelvesByZone } from "@/entities/warehouse-location";
import { ZoneColumn } from "./ZoneColumn";
import { ShelfColumn } from "./ShelfColumn";
import { BinColumn } from "./BinColumn";

export function WarehouseStructurePage() {
  const t = useTranslations("warehouse");
  const [selectedZoneId, setSelectedZoneId] = useState<number | null>(null);
  const [selectedShelfId, setSelectedShelfId] = useState<number | null>(null);
  const { data: zones } = useWarehouseZones();
  const { data: shelves } = useShelvesByZone(selectedZoneId);

  const selectedZone = zones?.find((z) => z.id === selectedZoneId);
  const selectedShelf = shelves?.find((s) => s.id === selectedShelfId);

  function handleSelectZone(id: number) {
    setSelectedZoneId(id);
    setSelectedShelfId(null);
  }

  return (
    <div className="flex flex-col gap-5 h-full">
      <div>
        <h1 className="text-2xl font-semibold">{t("structure.title")}</h1>
        <p className="text-sm text-muted-foreground mt-0.5">{t("structure.subtitle")}</p>
      </div>

      <div className="grid grid-cols-12 gap-4 flex-1 min-h-0">
        <ZoneColumn
          selectedZoneId={selectedZoneId}
          onSelectZone={handleSelectZone}
        />
        <ShelfColumn
          zoneId={selectedZoneId}
          zoneCode={selectedZone?.code ?? ""}
          selectedShelfId={selectedShelfId}
          onSelectShelf={setSelectedShelfId}
        />
        <BinColumn
          shelfId={selectedShelfId}
          shelfCode={selectedShelf?.code ?? ""}
        />
      </div>
    </div>
  );
}
