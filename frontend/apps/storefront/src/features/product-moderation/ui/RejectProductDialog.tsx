"use client";

import { useState } from "react";
import { XCircle } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Textarea } from "@/shared/ui/textarea";
import { Label } from "@/shared/ui/label";
import { useTranslations } from "next-intl";
import { useRejectProduct } from "@/entities/product/api/moderation";

interface Props {
  productBaseId: number;
}

export function RejectProductDialog({ productBaseId }: Props) {
  const t = useTranslations("manage.products.rejectDialog");
  const [open, setOpen] = useState(false);
  const [text, setText] = useState("");
  const reject = useRejectProduct();

  function handleSubmit() {
    reject.mutate(
      { id: productBaseId, reason: text.trim() },
      {
        onSuccess: () => {
          setOpen(false);
          setText("");
        },
      }
    );
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <Button
        size="sm"
        variant="destructive"
        onClick={() => setOpen(true)}
      >
        <XCircle className="h-3.5 w-3.5 mr-1" />
        {t("trigger")}
      </Button>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>{t("title")}</DialogTitle>
          <DialogDescription>{t("description")}</DialogDescription>
        </DialogHeader>
        <div className="space-y-3 mt-2">
          <div className="space-y-1.5">
            <Label>{t("noteLabel")}</Label>
            <Textarea
              value={text}
              onChange={(e) => setText(e.target.value)}
              rows={4}
              maxLength={1000}
              placeholder={t("notePlaceholder")}
            />
            <p className="text-xs text-muted-foreground text-right">
              {text.length}/1000
            </p>
          </div>
          <div className="flex justify-end gap-2">
            <Button
              variant="outline"
              onClick={() => setOpen(false)}
              disabled={reject.isPending}
            >
              {t("cancel")}
            </Button>
            <Button
              variant="destructive"
              disabled={!text.trim() || reject.isPending}
              onClick={handleSubmit}
            >
              {reject.isPending ? t("submitting") : t("submit")}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
