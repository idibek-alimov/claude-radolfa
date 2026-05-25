"use client";

import { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { useTranslations } from "next-intl";
import { Button } from "@/shared/ui/button";

interface Props {
  code: string;
  pickpointName?: string | null;
  pickpointAddress?: string | null;
}

export function PickupCodeDisplay({ code, pickpointName, pickpointAddress }: Props) {
  const [revealed, setRevealed] = useState(false);
  const t = useTranslations("pickupCode");

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-3">
        <span
          className={
            revealed
              ? "font-mono text-2xl tracking-[0.5em] text-primary select-all"
              : "font-mono text-2xl tracking-[0.5em] select-none"
          }
        >
          {revealed ? code : "••••••"}
        </span>
        <Button
          variant={revealed ? "ghost" : "outline"}
          size="sm"
          onClick={() => setRevealed((v) => !v)}
        >
          {revealed ? "Hide" : "Show Code"}
        </Button>
      </div>

      {revealed && (
        <div className="space-y-2">
          <div className="flex justify-center bg-white p-3 rounded-lg">
            <QRCodeSVG value={code} size={180} level="M" />
          </div>
          <p className="text-xs text-muted-foreground text-center">
            {t("scanCaption")}
          </p>
          {(pickpointName || pickpointAddress) && (
            <div className="mt-3 rounded-lg border bg-muted/50 px-3 py-2 text-left">
              {pickpointName && (
                <p className="text-sm font-medium">{pickpointName}</p>
              )}
              {pickpointAddress && (
                <p className="text-xs text-muted-foreground">{pickpointAddress}</p>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
