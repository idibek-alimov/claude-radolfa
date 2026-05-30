"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Move, History } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib";
import type { SkuLookupResponse } from "@/entities/warehouse-sku";
import { useLookupSkuByBarcode } from "@/entities/warehouse-sku";
import { RelocateForm } from "./RelocateForm";
import { InventoryHistoryDrawer } from "./InventoryHistoryDrawer";

interface Props {
  result: SkuLookupResponse;
  onResultChange: (result: SkuLookupResponse) => void;
}

export function SkuResultCard({ result, onResultChange }: Props) {
  const t = useTranslations("warehouse");
  const [relocateOpen, setRelocateOpen] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);
  const lookup = useLookupSkuByBarcode();

  const binPlacements = result.placements.filter((p) => p.binId != null);
  const canRelocate = binPlacements.length > 0;

  function handleRelocateSuccess() {
    lookup.mutate(result.barcode, {
      onSuccess: (fresh) => {
        onResultChange(fresh);
        setRelocateOpen(false);
      },
    });
  }

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

          {/* Placements */}
          <div>
            <p className="text-xs uppercase tracking-wide text-muted-foreground mb-2">
              {t("lookup.placements.title")}
            </p>
            {result.placements.length === 0 ? (
              <p className="text-sm italic text-muted-foreground">
                {t("lookup.placements.empty")}
              </p>
            ) : (
              <ul className="space-y-1">
                {result.placements.map((p, i) => (
                  <li key={i} className="flex items-center justify-between text-sm">
                    {p.binLabel != null ? (
                      <span className="font-mono text-zinc-900">{p.binLabel}</span>
                    ) : (
                      <span className="italic text-muted-foreground">
                        {t("lookup.placements.inbound")}
                      </span>
                    )}
                    <span className="tabular-nums font-medium text-zinc-700 ml-4">
                      {p.quantity}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Action row */}
          <div className="flex flex-wrap gap-2 pt-1 border-t border-zinc-100">
            <Button
              variant="outline"
              size="sm"
              disabled={!canRelocate}
              onClick={() => setRelocateOpen((o) => !o)}
            >
              <Move className="h-4 w-4 mr-1.5" />
              {t("lookup.actions.relocate")}
            </Button>
            <Button variant="ghost" size="sm" onClick={() => setHistoryOpen(true)}>
              <History className="h-4 w-4 mr-1.5" />
              {t("lookup.actions.history")}
            </Button>
          </div>

          {/* Inline relocate form */}
          {relocateOpen && (
            <RelocateForm
              skuId={result.skuId}
              placements={result.placements}
              onSuccess={handleRelocateSuccess}
              onCancel={() => setRelocateOpen(false)}
            />
          )}
        </CardContent>
      </Card>

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
