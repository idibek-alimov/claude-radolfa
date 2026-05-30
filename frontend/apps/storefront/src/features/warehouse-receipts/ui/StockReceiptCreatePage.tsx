"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Loader2, Package } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Textarea } from "@/shared/ui/textarea";
import { Switch } from "@/shared/ui/switch";
import { Card, CardContent } from "@/shared/ui/card";
import { getErrorMessage } from "@/shared/lib";
import type { SkuLookupResponse } from "@/entities/warehouse-sku";
import type { LineItem } from "../types";
import { useCreateStockReceipt } from "../api";
import { ReceiptScanInput } from "./ReceiptScanInput";
import { ManualSkuSearchCombobox } from "./ManualSkuSearchCombobox";
import { ReceiptLineItemsTable } from "./ReceiptLineItemsTable";

export function StockReceiptCreatePage() {
  const t = useTranslations("warehouse");
  const router = useRouter();
  const [supplierReference, setSupplierReference] = useState("");
  const [notes, setNotes] = useState("");
  const [lineItems, setLineItems] = useState<LineItem[]>([]);
  const [manualEntry, setManualEntry] = useState(false);
  const createReceipt = useCreateStockReceipt();

  function addOrIncrementLineItem(sku: SkuLookupResponse) {
    setLineItems((prev) => {
      const existing = prev.find((li) => li.skuId === sku.skuId);
      if (existing) {
        return prev.map((li) =>
          li.skuId === sku.skuId ? { ...li, quantity: li.quantity + 1 } : li
        );
      }
      return [
        ...prev,
        {
          skuId: sku.skuId,
          skuCode: sku.skuCode,
          productName: sku.productName,
          sizeLabel: sku.sizeLabel,
          quantity: 1,
          notes: "",
        },
      ];
    });
  }

  function handleQuantityChange(skuId: number, quantity: number) {
    setLineItems((prev) =>
      prev.map((li) => (li.skuId === skuId ? { ...li, quantity } : li))
    );
  }

  function handleNotesChange(skuId: number, notes: string) {
    setLineItems((prev) =>
      prev.map((li) => (li.skuId === skuId ? { ...li, notes } : li))
    );
  }

  function handleRemove(skuId: number) {
    setLineItems((prev) => prev.filter((li) => li.skuId !== skuId));
  }

  function handleSubmit() {
    if (lineItems.length === 0) {
      toast.error(t("receipts.create.emptyForm"));
      return;
    }
    if (lineItems.some((li) => li.quantity < 1)) {
      toast.error(t("receipts.create.invalidQuantity"));
      return;
    }
    createReceipt.mutate(
      {
        supplierReference,
        notes,
        items: lineItems.map((li) => ({
          skuId: li.skuId,
          quantity: li.quantity,
          notes: li.notes,
        })),
      },
      {
        onSuccess: (data) => {
          toast.success(t("receipts.create.successToast"));
          router.push(`/warehouse/receipts/${data.id}`);
        },
        onError: (err) => {
          toast.error(getErrorMessage(err, t("receipts.create.error")));
        },
      }
    );
  }

  return (
    <div className="flex flex-col gap-6 max-w-4xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-semibold text-zinc-900">{t("receipts.create.title")}</h1>
      </div>

      {/* Header fields */}
      <Card>
        <CardContent className="pt-6 space-y-4">
          <div className="space-y-1.5">
            <Label>{t("receipts.create.supplierRef")}</Label>
            <Input
              value={supplierReference}
              onChange={(e) => setSupplierReference(e.target.value)}
              placeholder={t("receipts.create.supplierRefPlaceholder")}
            />
          </div>
          <div className="space-y-1.5">
            <Label>{t("receipts.create.notes")}</Label>
            <Textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="—"
              rows={3}
            />
          </div>
        </CardContent>
      </Card>

      {/* Scan input */}
      <ReceiptScanInput onResult={addOrIncrementLineItem} />

      {/* Manual entry toggle */}
      <div className="flex items-center gap-3">
        <Switch
          id="manual-entry"
          checked={manualEntry}
          onCheckedChange={setManualEntry}
        />
        <Label htmlFor="manual-entry" className="cursor-pointer">
          {t("receipts.create.manualEntry")}
        </Label>
      </div>
      {manualEntry && (
        <ManualSkuSearchCombobox onSelect={addOrIncrementLineItem} />
      )}

      {/* Line items */}
      <Card>
        <CardContent className="pt-5">
          {lineItems.length === 0 ? (
            <div className="flex flex-col items-center justify-center border border-dashed rounded-xl p-10 text-muted-foreground gap-2">
              <Package className="h-8 w-8 text-muted-foreground/40" />
              <p className="text-sm">{t("receipts.create.emptyForm")}</p>
            </div>
          ) : (
            <ReceiptLineItemsTable
              items={lineItems}
              onQuantityChange={handleQuantityChange}
              onNotesChange={handleNotesChange}
              onRemove={handleRemove}
            />
          )}
        </CardContent>
      </Card>

      {/* Submit */}
      <div className="flex gap-3">
        <Button
          onClick={handleSubmit}
          disabled={lineItems.length === 0 || lineItems.some((li) => li.quantity < 1) || createReceipt.isPending}
        >
          {createReceipt.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
          {createReceipt.isPending ? t("receipts.create.submitting") : t("receipts.create.submitBtn")}
        </Button>
        <Button variant="outline" onClick={() => router.push("/warehouse/receipts")}>
          {t("common.cancel")}
        </Button>
      </div>
    </div>
  );
}
