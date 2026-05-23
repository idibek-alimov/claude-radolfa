"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { ScanBarcode } from "lucide-react";
import type { SkuLookupResponse } from "../types";
import { ScanInputPanel } from "./ScanInputPanel";
import { SkuResultCard } from "./SkuResultCard";

export function BarcodeLookupPage() {
  const t = useTranslations("warehouse");
  const [lastResult, setLastResult] = useState<SkuLookupResponse | null>(null);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-zinc-900">{t("lookup.title")}</h1>
      </div>

      <div className="grid grid-cols-3 gap-6 items-start">
        <div className="col-span-1">
          <ScanInputPanel onResult={setLastResult} />
        </div>

        <div className="col-span-2">
          {lastResult ? (
            <SkuResultCard result={lastResult} onResultChange={setLastResult} />
          ) : (
            <div className="flex flex-col items-center justify-center border border-dashed rounded-xl p-12 text-muted-foreground">
              <ScanBarcode className="h-10 w-10 text-muted-foreground/40 mb-3" />
              <p className="text-sm">{t("lookup.empty")}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
