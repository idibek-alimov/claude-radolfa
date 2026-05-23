"use client";

import { useRef, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { ScanBarcode, Loader2 } from "lucide-react";
import { Input } from "@/shared/ui/input";
import { Button } from "@/shared/ui/button";
import { Card, CardContent } from "@/shared/ui/card";
import { getErrorMessage } from "@/shared/lib";
import type { SkuLookupResponse } from "../types";
import { useLookupSkuByBarcode } from "../api";

interface Props {
  onResult: (result: SkuLookupResponse) => void;
}

export function ScanInputPanel({ onResult }: Props) {
  const t = useTranslations("warehouse");
  const [value, setValue] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);
  const lookup = useLookupSkuByBarcode();

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  function refocusInput() {
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
        refocusInput();
      },
      onError: (err: unknown) => {
        const is404 = (err as { response?: { status?: number } })?.response?.status === 404;
        toast.error(is404 ? t("lookup.notFound") : getErrorMessage(err, t("lookup.notFound")));
        setValue("");
        refocusInput();
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

          <form onSubmit={handleSubmit} className="flex flex-col gap-3">
            <Input
              ref={inputRef}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder={t("lookup.scanPrompt")}
              className="text-base h-12 font-mono tracking-wider"
              disabled={lookup.isPending}
              autoComplete="off"
              autoCorrect="off"
              spellCheck={false}
            />
            <Button type="submit" disabled={!value.trim() || lookup.isPending} className="w-full">
              {lookup.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  {t("common.loading")}
                </>
              ) : (
                t("common.scan")
              )}
            </Button>
          </form>

          <p className="text-xs text-muted-foreground text-center">{t("lookup.scanPrompt")}</p>
        </div>
      </CardContent>
    </Card>
  );
}
