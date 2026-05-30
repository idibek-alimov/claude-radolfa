"use client";

import { useState, useEffect, useRef } from "react";
import jsQR from "jsqr";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/shared/ui/dialog";
import {
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
} from "@/shared/ui/tabs";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { useVerifyPickup } from "@/features/pickpoint/api";
import { getErrorMessage } from "@/shared/lib";

interface Props {
  open: boolean;
  onClose: () => void;
}

function classifyError(err: unknown): string {
  const msg = getErrorMessage(err, "").toLowerCase();
  if (msg.includes("mismatch") || msg.includes("incorrect")) return "Incorrect code. Try again.";
  if (msg.includes("expired")) return "This code has expired. Ask an admin to regenerate it.";
  if (msg.includes("max") || msg.includes("attempts")) return "Too many failed attempts. Contact an admin.";
  if (msg.includes("already used") || msg.includes("used")) return "This code has already been used.";
  return getErrorMessage(err, "Failed to verify pickup");
}

async function decodeQrFromFile(file: File): Promise<string | null> {
  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new globalThis.Image();
      img.onload = () => {
        const canvas = document.createElement("canvas");
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext("2d");
        if (!ctx) { resolve(null); return; }
        ctx.drawImage(img, 0, 0);
        const imageData = ctx.getImageData(0, 0, img.width, img.height);
        const result = jsQR(imageData.data, img.width, img.height);
        resolve(result?.data ?? null);
      };
      img.onerror = () => resolve(null);
      img.src = e.target?.result as string;
    };
    reader.onerror = () => resolve(null);
    reader.readAsDataURL(file);
  });
}

export function PickupVerifyModal({ open, onClose }: Props) {
  const verify = useVerifyPickup();
  const [code, setCode]   = useState("");
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (open) { setCode(""); setError(null); }
  }, [open]);

  function handleVerify() {
    if (code.length !== 6) { setError("Enter the 6-digit code."); return; }
    setError(null);
    verify.mutate(code, {
      onSuccess: () => { toast.success("Pickup verified!"); onClose(); },
      onError: (err) => setError(classifyError(err)),
    });
  }

  async function handleScan(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = "";

    const decoded = await decodeQrFromFile(file);
    if (decoded && /^\d{6}$/.test(decoded)) {
      setCode(decoded);
      setError(null);
      verify.mutate(decoded, {
        onSuccess: () => { toast.success("Pickup verified!"); onClose(); },
        onError: (err) => setError(classifyError(err)),
      });
    } else {
      setError("Could not read QR. Please type the code manually.");
    }
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Verify Pickup</DialogTitle>
        </DialogHeader>

        <Tabs defaultValue="manual" className="w-full">
          <TabsList className="grid grid-cols-2 w-full">
            <TabsTrigger value="manual">Enter Code</TabsTrigger>
            <TabsTrigger value="scan">Scan QR</TabsTrigger>
          </TabsList>

          <TabsContent value="manual" className="py-3 space-y-4">
            <p className="text-sm text-muted-foreground text-center">
              Ask the customer for their 6-digit pickup code.
            </p>
            <Input
              type="text"
              inputMode="numeric"
              maxLength={6}
              pattern="[0-9]*"
              autoFocus
              value={code}
              onChange={(e) => { setCode(e.target.value.replace(/\D/g, "")); setError(null); }}
              onKeyDown={(e) => e.key === "Enter" && handleVerify()}
              className="text-center text-2xl tracking-[0.5em] h-14 font-mono"
              placeholder="000000"
            />
          </TabsContent>

          <TabsContent value="scan" className="py-3 space-y-4">
            <p className="text-sm text-muted-foreground text-center">
              Point the camera at the customer&apos;s QR code.
            </p>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              capture="environment"
              className="hidden"
              onChange={handleScan}
            />
            <div className="flex justify-center">
              <Button
                variant="outline"
                onClick={() => fileInputRef.current?.click()}
                disabled={verify.isPending}
              >
                {verify.isPending ? "Verifying…" : "Open Camera"}
              </Button>
            </div>
          </TabsContent>

          {error && <p className="text-destructive text-sm text-center pb-1">{error}</p>}
        </Tabs>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={verify.isPending}>Cancel</Button>
          <Button onClick={handleVerify} disabled={verify.isPending || code.length !== 6}>
            {verify.isPending ? "Verifying…" : "Confirm"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
