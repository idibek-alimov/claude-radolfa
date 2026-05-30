"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { createColor } from "@/entities/color";
import { getErrorMessage } from "@/shared/lib";
import { PlusIcon } from "lucide-react";

const COLOR_KEY_RE = /^[a-z0-9_]+$/;
const HEX_RE = /^#[0-9A-Fa-f]{6}$/;

export function CreateColorDialog() {
  const t = useTranslations("manage");
  const qc = useQueryClient();

  const [open, setOpen] = useState(false);
  const [colorKey, setColorKey] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [hexCode, setHexCode] = useState("#000000");

  const keyValid = COLOR_KEY_RE.test(colorKey);
  const hexValid = HEX_RE.test(hexCode);
  const canSubmit = colorKey.length > 0 && keyValid && displayName.trim().length > 0 && hexValid;

  const mutation = useMutation({
    mutationFn: () => createColor(colorKey, displayName.trim(), hexCode),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["colors"] });
      setOpen(false);
      setColorKey("");
      setDisplayName("");
      setHexCode("#000000");
      toast.success(t("colorCreated"));
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button size="sm">
          <PlusIcon className="h-4 w-4 mr-1" />
          {t("colorCreate")}
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{t("colorCreateTitle")}</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          {/* Key */}
          <div className="space-y-1">
            <Label>{t("colorKeyLabel")}</Label>
            <Input
              value={colorKey}
              onChange={(e) => setColorKey(e.target.value)}
              placeholder={t("colorKeyPlaceholder")}
              maxLength={64}
            />
            <p className="text-xs text-muted-foreground">{t("colorKeyHint")}</p>
            {colorKey.length > 0 && !keyValid && (
              <p className="text-xs text-destructive">{t("colorKeyHint")}</p>
            )}
          </div>

          {/* Display name */}
          <div className="space-y-1">
            <Label>{t("colorDisplayNameLabel")}</Label>
            <Input
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              maxLength={128}
            />
          </div>

          {/* Hex code */}
          <div className="space-y-1">
            <Label>{t("colorHexLabel")}</Label>
            <div className="flex items-center gap-2">
              <input
                type="color"
                value={hexCode}
                onChange={(e) => setHexCode(e.target.value)}
                className="h-8 w-10 cursor-pointer rounded border border-input bg-transparent p-0.5"
              />
              <Input
                value={hexCode}
                onChange={(e) => setHexCode(e.target.value)}
                className="h-8 text-sm w-28 font-mono"
                maxLength={7}
              />
            </div>
          </div>

          {/* Live preview */}
          {hexValid && (
            <div className="flex items-center gap-3 pt-1">
              <div
                className="h-8 w-8 rounded-full border shadow-sm"
                style={{ backgroundColor: hexCode }}
              />
              <span className="text-sm text-muted-foreground">
                {displayName || colorKey || "—"}
              </span>
            </div>
          )}

          <Button
            className="w-full"
            disabled={!canSubmit || mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? t("colorCreateSubmit") + "…" : t("colorCreateSubmit")}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
