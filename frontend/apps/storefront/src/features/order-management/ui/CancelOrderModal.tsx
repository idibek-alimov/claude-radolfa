"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from "@/shared/ui/alert-dialog";
import { Label } from "@/shared/ui/label";
import { Textarea } from "@/shared/ui/textarea";
import { useCancelOrder } from "@/entities/order";
import { getErrorMessage } from "@/shared/lib";

interface CancelOrderModalProps {
  open: boolean;
  onClose: () => void;
  orderId: number;
}

export function CancelOrderModal({ open, onClose, orderId }: CancelOrderModalProps) {
  const t = useTranslations("manage.orders");
  const cancelOrder = useCancelOrder();
  const [reason, setReason] = useState("");

  function handleConfirm() {
    cancelOrder.mutate(
      { orderId, reason: reason.trim() || undefined },
      {
        onSuccess: () => {
          toast.success(t("toast.cancelled"));
          setReason("");
          onClose();
        },
        onError: (err) => toast.error(getErrorMessage(err, t("toast.cancelFailed"))),
      }
    );
  }

  return (
    <AlertDialog open={open} onOpenChange={(o) => !o && onClose()}>
      <AlertDialogContent className="max-w-sm">
        <AlertDialogHeader>
          <AlertDialogTitle>{t("cancelModal.title", { id: orderId })}</AlertDialogTitle>
          <AlertDialogDescription>{t("cancelModal.description")}</AlertDialogDescription>
        </AlertDialogHeader>

        <div className="space-y-1.5 py-2">
          <Label htmlFor="cancel-reason" className="text-sm">{t("cancelModal.reasonLabel")}</Label>
          <Textarea
            id="cancel-reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder={t("cancelModal.reasonPlaceholder")}
            rows={3}
          />
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel onClick={onClose}>{t("cancelModal.keep")}</AlertDialogCancel>
          <AlertDialogAction
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            onClick={handleConfirm}
            disabled={cancelOrder.isPending}
          >
            {cancelOrder.isPending ? t("cancelModal.cancelling") : t("cancelModal.confirm")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
