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
import { useRefundOrder } from "@/entities/order";
import { getErrorMessage } from "@/shared/lib";

interface RefundOrderModalProps {
  open: boolean;
  onClose: () => void;
  orderId: number;
}

export function RefundOrderModal({ open, onClose, orderId }: RefundOrderModalProps) {
  const t = useTranslations("manage.orders");
  const refundOrder = useRefundOrder();
  const [reason, setReason] = useState("");

  function handleConfirm() {
    refundOrder.mutate(
      { orderId, reason: reason.trim() || undefined },
      {
        onSuccess: () => {
          toast.success(t("toast.refunded"));
          setReason("");
          onClose();
        },
        onError: (err) => toast.error(getErrorMessage(err, t("toast.refundFailed"))),
      }
    );
  }

  return (
    <AlertDialog open={open} onOpenChange={(o) => !o && onClose()}>
      <AlertDialogContent className="max-w-sm">
        <AlertDialogHeader>
          <AlertDialogTitle>{t("refundModal.title", { id: orderId })}</AlertDialogTitle>
          <AlertDialogDescription>{t("refundModal.description")}</AlertDialogDescription>
        </AlertDialogHeader>

        <div className="space-y-1.5 py-2">
          <Label htmlFor="refund-reason" className="text-sm">{t("refundModal.reasonLabel")}</Label>
          <Textarea
            id="refund-reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder={t("refundModal.reasonPlaceholder")}
            rows={3}
          />
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel onClick={onClose}>{t("refundModal.keep")}</AlertDialogCancel>
          <AlertDialogAction
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            onClick={handleConfirm}
            disabled={refundOrder.isPending}
          >
            {refundOrder.isPending ? t("refundModal.refunding") : t("refundModal.confirm")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
