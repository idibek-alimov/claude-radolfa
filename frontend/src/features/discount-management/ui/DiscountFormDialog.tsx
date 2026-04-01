"use client";

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/shared/ui/sheet";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { ScrollArea } from "@/shared/ui/scroll-area";
import { fetchDiscountTypes, createDiscount, updateDiscount } from "../api";
import { DiscountStatusBadge } from "./DiscountStatusBadge";
import { QuickDateRange } from "./QuickDateRange";
import { SkuPicker } from "@/entities/product";
import type { DiscountResponse, DiscountFormValues } from "../model/types";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";
import { CalendarDays, Palette, Settings2, ShoppingBag, Eye } from "lucide-react";

export interface DiscountPrefillData {
  typeId: number;
  itemCodes: string[];
  discountValue: number;
  validFrom: string;
  validUpto: string;
  title: string;
  colorHex: string;
}

interface DiscountFormDialogProps {
  open: boolean;
  onClose: () => void;
  /** Existing discount to edit — mutually exclusive with prefillData */
  editTarget?: DiscountResponse;
  /** Pre-populate fields for create mode (e.g. duplicate) */
  prefillData?: DiscountPrefillData;
}

const toLocalInput = (iso: string) => iso.slice(0, 16);
const toIso = (local: string) => `${local}:00Z`;

function getDurationLabel(from: string, to: string): string | null {
  if (!from || !to) return null;
  const diff = new Date(to).getTime() - new Date(from).getTime();
  if (diff <= 0) return null;
  const days = Math.round(diff / (1000 * 60 * 60 * 24));
  if (days === 1) return "Duration: 1 day";
  return `Duration: ${days} days`;
}

function SectionHeading({
  icon: Icon,
  title,
}: {
  icon: React.ElementType;
  title: string;
}) {
  return (
    <div className="flex items-center gap-2 pb-1 border-b border-border">
      <Icon className="h-4 w-4 text-muted-foreground" />
      <h3 className="text-sm font-semibold text-foreground">{title}</h3>
    </div>
  );
}

export function DiscountFormDialog({
  open,
  onClose,
  editTarget,
  prefillData,
}: DiscountFormDialogProps) {
  const isEdit = editTarget !== undefined;
  const qc = useQueryClient();

  const [typeId, setTypeId] = useState(0);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [discountValue, setDiscountValue] = useState(10);
  const [validFrom, setValidFrom] = useState("");
  const [validUpto, setValidUpto] = useState("");
  const [title, setTitle] = useState("");
  const [colorHex, setColorHex] = useState("E74C3C");

  const { data: types = [] } = useQuery({
    queryKey: ["discount-types"],
    queryFn: fetchDiscountTypes,
  });

  useEffect(() => {
    if (!open) return;
    if (editTarget) {
      setTypeId(editTarget.type.id);
      setSelectedCodes(editTarget.itemCodes);
      setDiscountValue(editTarget.discountValue);
      setValidFrom(toLocalInput(editTarget.validFrom));
      setValidUpto(toLocalInput(editTarget.validUpto));
      setTitle(editTarget.title);
      setColorHex(editTarget.colorHex);
    } else if (prefillData) {
      setTypeId(prefillData.typeId);
      setSelectedCodes(prefillData.itemCodes);
      setDiscountValue(prefillData.discountValue);
      setValidFrom(prefillData.validFrom);
      setValidUpto(prefillData.validUpto);
      setTitle(prefillData.title);
      setColorHex(prefillData.colorHex);
    } else {
      setTypeId(types[0]?.id ?? 0);
      setSelectedCodes([]);
      setDiscountValue(10);
      setValidFrom("");
      setValidUpto("");
      setTitle("");
      setColorHex("E74C3C");
    }
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const buildPayload = (): DiscountFormValues => ({
    typeId,
    itemCodes: selectedCodes,
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

  const showPreview = title.trim().length > 0 && colorHex.length === 6;
  const durationLabel = getDurationLabel(validFrom, validUpto);

  const canSubmit =
    typeId > 0 &&
    selectedCodes.length > 0 &&
    discountValue > 0 &&
    validFrom.length > 0 &&
    validUpto.length > 0 &&
    title.trim().length > 0 &&
    colorHex.length === 6 &&
    !mutation.isPending;

  return (
    <Sheet open={open} onOpenChange={(o) => !o && onClose()}>
      <SheetContent
        side="right"
        className="w-full sm:w-[680px] sm:max-w-[680px] p-0 flex flex-col"
      >
        <SheetHeader className="px-6 py-4 border-b border-border flex-shrink-0">
          <SheetTitle>{isEdit ? "Edit Discount" : "New Discount"}</SheetTitle>
        </SheetHeader>

        <ScrollArea className="flex-1 min-h-0">
          <div className="px-6 py-5 space-y-7">
            {/* ─── Section 1: Campaign Details ─── */}
            <section className="space-y-4">
              <SectionHeading icon={Settings2} title="Campaign Details" />

              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2 space-y-1.5">
                  <Label>Campaign title</Label>
                  <Input
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="e.g. Winter Sale"
                  />
                </div>

                <div className="space-y-1.5">
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

                <div className="space-y-1.5">
                  <Label>Discount %</Label>
                  <Input
                    type="number"
                    min={1}
                    max={99}
                    value={discountValue}
                    onChange={(e) => setDiscountValue(Number(e.target.value))}
                  />
                </div>
              </div>

              {/* Badge color */}
              <div className="space-y-1.5">
                <Label className="flex items-center gap-1.5">
                  <Palette className="h-3.5 w-3.5" />
                  Badge color
                </Label>
                <div className="flex items-center gap-3">
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
                  {showPreview && (
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
                  )}
                </div>
              </div>
            </section>

            {/* ─── Section 2: Schedule ─── */}
            <section className="space-y-4">
              <SectionHeading icon={CalendarDays} title="Schedule" />

              <div className="space-y-2">
                <div className="flex gap-3">
                  <div className="flex-1 space-y-1.5">
                    <Label>Valid from</Label>
                    <Input
                      type="datetime-local"
                      value={validFrom}
                      onChange={(e) => setValidFrom(e.target.value)}
                    />
                  </div>
                  <div className="flex-1 space-y-1.5">
                    <Label>Valid until</Label>
                    <Input
                      type="datetime-local"
                      value={validUpto}
                      onChange={(e) => setValidUpto(e.target.value)}
                    />
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <QuickDateRange
                    onSelect={(from, to) => {
                      setValidFrom(from);
                      setValidUpto(to);
                    }}
                  />
                  {durationLabel && (
                    <span className="text-xs text-muted-foreground">
                      {durationLabel}
                    </span>
                  )}
                </div>
              </div>
            </section>

            {/* ─── Section 3: Product Selection ─── */}
            <section className="space-y-4">
              <SectionHeading icon={ShoppingBag} title="Product Selection" />
              <p className="text-xs text-muted-foreground -mt-2">
                Search for products and select the sizes (SKUs) to include in this
                discount.
              </p>
              <SkuPicker
                selectedCodes={selectedCodes}
                onSelectionChange={setSelectedCodes}
              />
            </section>

            {/* ─── Section 4: Preview ─── */}
            {showPreview && (
              <section className="space-y-3">
                <SectionHeading icon={Eye} title="Preview" />
                <div className="rounded-lg border border-border bg-muted/30 p-4 flex items-center gap-4">
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
                  <div className="text-sm text-muted-foreground">
                    This badge will appear on product cards
                    {selectedCodes.length > 0 && (
                      <> for <strong className="text-foreground">{selectedCodes.length} SKU{selectedCodes.length !== 1 ? "s" : ""}</strong></>
                    )}.
                  </div>
                </div>
              </section>
            )}
          </div>
        </ScrollArea>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-border flex-shrink-0 flex items-center justify-between gap-3">
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button disabled={!canSubmit} onClick={() => mutation.mutate()} className="min-w-32">
            {mutation.isPending
              ? isEdit
                ? "Saving…"
                : "Creating…"
              : isEdit
                ? "Save Changes"
                : "Create Discount"}
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  );
}
