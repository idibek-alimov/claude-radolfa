"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Plus } from "lucide-react";
import { Button } from "@/shared/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/shared/ui/dialog";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { getErrorMessage } from "@/shared/lib";
import { useCreateBin } from "../api";

interface Props {
  shelfId: number;
  shelfLabel: string;
  disabled?: boolean;
}

export function CreateBinDialog({ shelfId, shelfLabel, disabled }: Props) {
  const t = useTranslations("warehouse");
  const [open, setOpen] = useState(false);
  const [code, setCode] = useState("");
  const [codeError, setCodeError] = useState("");
  const create = useCreateBin(shelfId);

  function handleOpenChange(next: boolean) {
    setOpen(next);
    if (!next) {
      setCode("");
      setCodeError("");
    }
  }

  function handleSubmit() {
    if (!code.trim()) {
      setCodeError(t("structure.validation.codeRequired"));
      return;
    }
    setCodeError("");
    create.mutate(
      { code: code.trim() },
      {
        onSuccess: () => {
          toast.success(t("structure.create.bin.success"));
          handleOpenChange(false);
        },
        onError: (err) => {
          toast.error(getErrorMessage(err, t("structure.errors.create")));
        },
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="sm" className="h-7 w-7 p-0" disabled={disabled}>
          <Plus className="h-4 w-4" />
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>{t("structure.create.bin.title", { shelfLabel })}</DialogTitle>
        </DialogHeader>
        <div className="space-y-4 py-2">
          <div className="space-y-1.5">
            <Label>{t("structure.create.bin.code")}</Label>
            <Input
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder={t("structure.create.bin.codePlaceholder")}
              maxLength={20}
              autoFocus
            />
            {codeError && <p className="text-destructive text-sm">{codeError}</p>}
          </div>
        </div>
        <div className="flex gap-2 justify-end pt-2">
          <Button variant="outline" onClick={() => handleOpenChange(false)} disabled={create.isPending}>
            {t("common.cancel")}
          </Button>
          <Button onClick={handleSubmit} disabled={create.isPending}>
            {create.isPending ? t("structure.create.bin.submitting") : t("structure.create.bin.submit")}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
