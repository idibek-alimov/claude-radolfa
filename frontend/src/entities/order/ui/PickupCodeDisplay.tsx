"use client";

import { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { Button } from "@/shared/ui/button";

interface Props {
  code: string;
}

export function PickupCodeDisplay({ code }: Props) {
  const [revealed, setRevealed] = useState(false);

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
            Show this code or let staff scan the QR at the counter.
          </p>
        </div>
      )}
    </div>
  );
}
