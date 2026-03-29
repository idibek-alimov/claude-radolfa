"use client";

import { useState } from "react";
import { DiscountTable } from "./DiscountTable";
import { DiscountFormDialog } from "./DiscountFormDialog";
import { DiscountTypesPanel } from "./DiscountTypesPanel";
import type { DiscountResponse } from "../model/types";

export function DiscountsTab() {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<DiscountResponse | undefined>();

  const openCreate = () => {
    setEditTarget(undefined);
    setDialogOpen(true);
  };

  const openEdit = (discount: DiscountResponse) => {
    setEditTarget(discount);
    setDialogOpen(true);
  };

  return (
    <div className="space-y-8">
      <DiscountTypesPanel />

      <DiscountTable onEdit={openEdit} onNew={openCreate} />

      <DiscountFormDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        editTarget={editTarget}
      />
    </div>
  );
}
