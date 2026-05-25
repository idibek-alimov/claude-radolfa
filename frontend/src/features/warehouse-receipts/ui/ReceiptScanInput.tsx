"use client";

import { useRef, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { ScanBarcode, Loader2 } from "lucide-react";
import { Input } from "@/shared/ui/input";
import { Button } from "@/shared/ui/button";
import { Card, CardContent } from "@/shared/ui/card";
import { getErrorMessage } from "@/shared/lib";
import { useLookupSkuByBarcode } from "@/entities/warehouse-sku";
import type { SkuLookupResponse } from "@/entities/warehouse-sku";

interface Props {
  onResult: (sku: SkuLookupResponse) => void;
}

export function ReceiptScanInput({ onResult }: Props) {
  const t = useTranslations("warehouse");
  const [value, setValue] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);
  const lookup = useLookupSkuByBarcode();

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  function refocus() {
    requestAnimationFrame(() => inputRef.current?.focus());
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const code = value.trim();
    if (!code) return;

    lookup.mutate(code, {
      onSuccess: (data) => {
        onResult(data);
        setValue("");
        refocus();
      },
      onError: (err: unknown) => {
        const is404 = (err as { response?: { status?: number } })?.response?.status === 404;
        toast.error(
          is404 ? t("receipts.create.scanNotFound") : getErrorMessage(err, t("receipts.create.scanNotFound"))
        );
        setValue("");
        refocus();
      },
    });
  }

  return (
    <Card>
      <CardContent className="pt-6">
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2 text-amber-600">
            <ScanBarcode className="h-5 w-5" />
            <span className="text-sm font-semibold">{t("common.scan")}</span>
          </div>
          <form onSubmit={handleSubmit} className="flex gap-2">
            <Input
              ref={inputRef}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder={t("receipts.create.scanPrompt")}
              className="text-base h-12 font-mono tracking-wider flex-1"
              disabled={lookup.isPending}
              autoComplete="off"
              autoCorrect="off"
              spellCheck={false}
            />
            <Button type="submit" disabled={!value.trim() || lookup.isPending} className="h-12">
              {lookup.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <ScanBarcode className="h-4 w-4" />
              )}
            </Button>
          </form>
        </div>
      </CardContent>
    </Card>
  );
}
