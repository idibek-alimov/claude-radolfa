"use client";

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { fetchDiscountTypes, createDiscount, updateDiscount } from "../api";
import { DiscountStatusBadge } from "./DiscountStatusBadge";
import type { DiscountResponse, DiscountFormValues } from "../model/types";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";

interface DiscountFormDialogProps {
  open: boolean;
  onClose: () => void;
  /** Pass a discount to edit; undefined = create mode */
  editTarget?: DiscountResponse;
}

/** Convert ISO instant to datetime-local string */
const toLocalInput = (iso: string) => iso.slice(0, 16);

/** Convert datetime-local string to ISO instant */
const toIso = (local: string) => `${local}:00Z`;

export function DiscountFormDialog({ open, onClose, editTarget }: DiscountFormDialogProps) {
  const isEdit = editTarget !== undefined;
  const qc = useQueryClient();

  const [typeId, setTypeId] = useState(0);
  const [itemCodesRaw, setItemCodesRaw] = useState("");
  const [discountValue, setDiscountValue] = useState(10);
  const [validFrom, setValidFrom] = useState("");
  const [validUpto, setValidUpto] = useState("");
  const [title, setTitle] = useState("");
  const [colorHex, setColorHex] = useState("E74C3C");

  const { data: types = [] } = useQuery({
    queryKey: ["discount-types"],
    queryFn: fetchDiscountTypes,
  });

  // Populate / reset form when dialog opens
  useEffect(() => {
    if (!open) return;
    if (editTarget) {
      setTypeId(editTarget.type.id);
      setItemCodesRaw(editTarget.itemCodes.join(", "));
      setDiscountValue(editTarget.discountValue);
      setValidFrom(toLocalInput(editTarget.validFrom));
      setValidUpto(toLocalInput(editTarget.validUpto));
      setTitle(editTarget.title);
      setColorHex(editTarget.colorHex);
    } else {
      setTypeId(types[0]?.id ?? 0);
      setItemCodesRaw("");
      setDiscountValue(10);
      setValidFrom("");
      setValidUpto("");
      setTitle("");
      setColorHex("E74C3C");
    }
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const buildPayload = (): DiscountFormValues => ({
    typeId,
    itemCodes: itemCodesRaw
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean),
    discountValue,
    validFrom: toIso(validFrom),
    validUpto: toIso(validUpto),
    title: title.trim(),
    colorHex,
  });

  const mutation = useMutation({
    mutationFn: () =>
      isEdit
        ? updateDiscount(editTarget!.id, buildPayload())
        : createDiscount(buildPayload()),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["discounts"] });
      toast.success(isEdit ? "Discount updated" : "Discount created");
      onClose();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const skuCount = itemCodesRaw.split(",").filter((s) => s.trim()).length;
  const showPreview = title.trim().length > 0 && colorHex.length === 6;
  const canSubmit =
    typeId > 0 &&
    skuCount > 0 &&
    discountValue > 0 &&
    validFrom.length > 0 &&
    validUpto.length > 0 &&
    title.trim().length > 0 &&
    colorHex.length === 6 &&
    !mutation.isPending;

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit Discount" : "New Discount"}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          {/* Type */}
          <div className="space-y-1">
            <Label>Type</Label>
            <select
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
              value={typeId}
              onChange={(e) => setTypeId(Number(e.target.value))}
            >
              <option value={0} disabled>
                Select type…
              </option>
              {types.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} (rank {t.rank})
                </option>
              ))}
            </select>
          </div>

          {/* Title */}
          <div className="space-y-1">
            <Label>Campaign title</Label>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Winter Sale"
            />
          </div>

          {/* Badge color */}
          <div className="space-y-1">
            <Label>Badge color</Label>
            <div className="flex items-center gap-2">
              <input
                type="color"
                value={`#${colorHex}`}
                onChange={(e) => setColorHex(e.target.value.replace("#", ""))}
                className="h-9 w-12 cursor-pointer rounded border border-input bg-transparent p-0.5"
              />
              <Input
                value={colorHex}
                onChange={(e) => setColorHex(e.target.value.replace("#", ""))}
                maxLength={6}
                placeholder="E74C3C"
                className="font-mono w-28"
              />
            </div>
          </div>

          {/* Live badge preview */}
          {showPreview && (
            <div className="flex items-center gap-2">
              <span className="text-sm text-muted-foreground">Preview:</span>
              <DiscountStatusBadge
                discount={{
                  id: 0,
                  type: { id: 0, name: "", rank: 0 },
                  itemCodes: [],
                  discountValue,
                  validFrom: "",
                  validUpto: "",
                  disabled: false,
                  title,
                  colorHex,
                }}
              />
            </div>
          )}

          {/* Discount % */}
          <div className="space-y-1">
            <Label>Discount %</Label>
            <Input
              type="number"
              min={1}
              max={100}
              value={discountValue}
              onChange={(e) => setDiscountValue(Number(e.target.value))}
              className="w-28"
            />
          </div>

          {/* Valid from / to */}
          <div className="flex gap-3">
            <div className="flex-1 space-y-1">
              <Label>Valid from</Label>
              <Input
                type="datetime-local"
                value={validFrom}
                onChange={(e) => setValidFrom(e.target.value)}
              />
            </div>
            <div className="flex-1 space-y-1">
              <Label>Valid until</Label>
              <Input
                type="datetime-local"
                value={validUpto}
                onChange={(e) => setValidUpto(e.target.value)}
              />
            </div>
          </div>

          {/* SKU codes */}
          <div className="space-y-1">
            <Label>SKU codes (comma-separated)</Label>
            <Input
              value={itemCodesRaw}
              onChange={(e) => setItemCodesRaw(e.target.value)}
              placeholder="SKU-001, SKU-002, SKU-003"
            />
            <p className="text-xs text-muted-foreground">{skuCount} SKU(s) entered</p>
          </div>

          <Button className="w-full" disabled={!canSubmit} onClick={() => mutation.mutate()}>
            {mutation.isPending
              ? isEdit
                ? "Saving…"
                : "Creating…"
              : isEdit
                ? "Save Changes"
                : "Create Discount"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
