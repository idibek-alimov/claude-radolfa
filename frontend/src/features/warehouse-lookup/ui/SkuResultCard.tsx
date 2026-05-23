"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { MapPin, History, Unlink } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib";
import type { SkuLookupResponse } from "@/entities/warehouse-sku";
import { BinReassignmentForm } from "./BinReassignmentForm";
import { UnassignConfirm } from "./UnassignConfirm";
import { InventoryHistoryDrawer } from "./InventoryHistoryDrawer";

interface Props {
  result: SkuLookupResponse;
  onResultChange: (result: SkuLookupResponse) => void;
}

export function SkuResultCard({ result, onResultChange }: Props) {
  const t = useTranslations("warehouse");
  const [reassignOpen, setReassignOpen] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [confirmUnassign, setConfirmUnassign] = useState(false);

  return (
    <>
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-xl font-semibold leading-tight">
            {result.productName}
          </CardTitle>
          <div className="flex items-center gap-2 text-sm text-muted-foreground mt-1">
            <span>
              {t("lookup.size")}:{" "}
              <span className="font-medium text-zinc-700">{result.sizeLabel}</span>
            </span>
            <span>·</span>
            <span className="font-mono text-xs bg-zinc-100 px-1.5 py-0.5 rounded">
              {result.skuCode}
            </span>
          </div>
        </CardHeader>

        <CardContent className="space-y-5">
          {/* Stock */}
          <div>
            <p className="text-xs uppercase tracking-wide text-muted-foreground mb-1">
              {t("lookup.stock")}
            </p>
            <p
              className={cn(
                "text-2xl font-bold tabular-nums",
                result.stockQuantity > 0 ? "text-zinc-900" : "text-rose-600",
              )}
            >
              {result.stockQuantity}
            </p>
          </div>

          {/* Bin location */}
          <div>
            <p className="text-xs uppercase tracking-wide text-muted-foreground mb-1">
              {t("lookup.binLocation")}
            </p>
            <p
              className={cn(
                "text-sm",
                result.binLocation
                  ? "font-mono text-zinc-900"
                  : "italic text-muted-foreground",
              )}
            >
              {result.binLocation ?? t("lookup.unassigned")}
            </p>
          </div>

          {/* Action row */}
          <div className="flex flex-wrap gap-2 pt-1 border-t border-zinc-100">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setReassignOpen((o) => !o)}
            >
              <MapPin className="h-4 w-4 mr-1.5" />
              {t("lookup.actions.reassign")}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              disabled={!result.binLocation}
              onClick={() => setConfirmUnassign(true)}
            >
              <Unlink className="h-4 w-4 mr-1.5" />
              {t("lookup.actions.unassign")}
            </Button>
            <Button variant="ghost" size="sm" onClick={() => setHistoryOpen(true)}>
              <History className="h-4 w-4 mr-1.5" />
              {t("lookup.actions.history")}
            </Button>
          </div>

          {/* Inline bin reassignment form */}
          {reassignOpen && (
            <BinReassignmentForm
              skuId={result.skuId}
              onSuccess={(binLabel) => {
                onResultChange({ ...result, binLocation: binLabel });
                setReassignOpen(false);
              }}
              onCancel={() => setReassignOpen(false)}
            />
          )}
        </CardContent>
      </Card>

      <UnassignConfirm
        open={confirmUnassign}
        skuId={result.skuId}
        productName={result.productName}
        sizeLabel={result.sizeLabel}
        currentBin={result.binLocation}
        onSuccess={() => {
          onResultChange({ ...result, binLocation: null });
          setConfirmUnassign(false);
        }}
        onClose={() => setConfirmUnassign(false)}
      />

      <InventoryHistoryDrawer
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        skuId={result.skuId}
        productName={result.productName}
        skuCode={result.skuCode}
      />
    </>
  );
}
