"use client";

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
} from "@radolfa/shared/ui/alert-dialog";
import { useAdminOrder, useUpdateOrderStatus } from "@/entities/order";
import { getErrorMessage } from "@radolfa/shared/lib";

interface UnclaimOrderDialogProps {
  open: boolean;
  onClose: () => void;
  orderId: number;
}

export function UnclaimOrderDialog({ open, onClose, orderId }: UnclaimOrderDialogProps) {
  const t = useTranslations("manage.orders");
  const { data: order } = useAdminOrder(open ? orderId : null);
  const updateStatus = useUpdateOrderStatus();

  function handleConfirm() {
    updateStatus.mutate(
      { orderId, status: "PICKED" },
      {
        onSuccess: () => {
          toast.success(t("toast.unclaimed"));
          onClose();
        },
        onError: (err) => toast.error(getErrorMessage(err, t("toast.unclaimFailed"))),
      }
    );
  }

  return (
    <AlertDialog open={open} onOpenChange={(o) => !o && onClose()}>
      <AlertDialogContent className="max-w-sm">
        <AlertDialogHeader>
          <AlertDialogTitle>
            {t("unclaimModal.title", { id: orderId })}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {t("unclaimModal.description", { courier: order?.courierName ?? "—" })}
          </AlertDialogDescription>
        </AlertDialogHeader>

        <AlertDialogFooter>
          <AlertDialogCancel onClick={onClose}>
            {t("unclaimModal.keep")}
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleConfirm}
            disabled={updateStatus.isPending}
          >
            {updateStatus.isPending
              ? t("unclaimModal.unclaiming")
              : t("unclaimModal.confirm")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
