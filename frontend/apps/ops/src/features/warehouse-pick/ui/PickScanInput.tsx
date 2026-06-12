"use client";

import { useRef, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { ScanBarcode, Loader2 } from "lucide-react";
import { Input } from "@radolfa/shared/ui/input";
import { Button } from "@radolfa/shared/ui/button";
import { cn, getErrorMessage } from "@radolfa/shared/lib";
import { useScanUnit } from "../api";

interface Props {
  orderId: number;
  onScanned: (orderItemId: number) => void;
}

export function PickScanInput({ orderId, onScanned }: Props) {
  const t = useTranslations("warehouse");
  const [value, setValue] = useState("");
  const [errorFlash, setErrorFlash] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const scan = useScanUnit(orderId);

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

    scan.mutate(code, {
      onSuccess: (result) => {
        onScanned(result.orderItemId);
        setValue("");
        refocusInput();
      },
      onError: (err: unknown) => {
        const code422 = (err as { response?: { data?: { code?: string } } })?.response?.data?.code;
        if (code422 === "BARCODE_MISMATCH") {
          toast.error(t("pick.session.mismatch"));
          setErrorFlash(true);
          setTimeout(() => setErrorFlash(false), 800);
        } else if (code422 === "ALREADY_FULLY_PICKED") {
          toast.error(t("pick.session.alreadyPicked"));
        } else {
          toast.error(getErrorMessage(err));
        }
        setValue("");
        refocusInput();
      },
    });
  }

  return (
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
          placeholder={t("pick.session.scanPrompt")}
          className={cn(
            "text-base h-12 font-mono tracking-wider transition-all",
            errorFlash && "ring-2 ring-destructive",
          )}
          disabled={scan.isPending}
          autoComplete="off"
          autoCorrect="off"
          spellCheck={false}
        />
        <Button type="submit" disabled={!value.trim() || scan.isPending} className="w-full">
          {scan.isPending ? (
            <>
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              {t("common.loading")}
            </>
          ) : (
            t("common.scan")
          )}
        </Button>
      </form>
    </div>
  );
}
