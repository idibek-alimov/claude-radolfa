"use client";

import { useState } from "react";
import { DiscountTable } from "./DiscountTable";
import { DiscountFormDialog, type DiscountPrefillData } from "./DiscountFormDialog";
import { DiscountTypesPanel } from "./DiscountTypesPanel";
import type { DiscountResponse } from "../model/types";

const toLocalInput = (iso: string) => iso.slice(0, 16);

export function DiscountsTab() {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<DiscountResponse | undefined>();
  const [prefillData, setPrefillData] = useState<DiscountPrefillData | undefined>();

  const openCreate = () => {
    setEditTarget(undefined);
    setPrefillData(undefined);
    setDialogOpen(true);
  };

  const openEdit = (discount: DiscountResponse) => {
    setPrefillData(undefined);
    setEditTarget(discount);
    setDialogOpen(true);
  };

  const openDuplicate = (discount: DiscountResponse) => {
    setEditTarget(undefined);
    setPrefillData({
      typeId: discount.type.id,
      itemCodes: discount.itemCodes,
      discountValue: discount.discountValue,
      validFrom: toLocalInput(discount.validFrom),
      validUpto: toLocalInput(discount.validUpto),
      title: `Copy of ${discount.title}`,
      colorHex: discount.colorHex,
    });
    setDialogOpen(true);
  };

  return (
    <div className="space-y-8">
      <DiscountTypesPanel />

      <DiscountTable
        onEdit={openEdit}
        onNew={openCreate}
        onDuplicate={openDuplicate}
      />

      <DiscountFormDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        editTarget={editTarget}
        prefillData={prefillData}
      />
    </div>
  );
}
