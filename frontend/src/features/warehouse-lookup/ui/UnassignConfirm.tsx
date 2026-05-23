"use client";

import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/shared/ui/alert-dialog";
import { useAssignSkuToBin } from "../api";

interface Props {
  open: boolean;
  skuId: number;
  productName: string;
  sizeLabel: string;
  currentBin: string | null;
  onSuccess: () => void;
  onClose: () => void;
}

export function UnassignConfirm({
  open,
  skuId,
  productName,
  sizeLabel,
  currentBin,
  onSuccess,
  onClose,
}: Props) {
  const t = useTranslations("warehouse");
  const assign = useAssignSkuToBin();

  function handleConfirm() {
    assign.mutate(
      { skuId, binId: null },
      {
        onSuccess: () => {
          toast.success(t("lookup.unassign.success"));
          onSuccess();
        },
        onError: () => {
          toast.error("Failed to unassign bin");
        },
      },
    );
  }

  return (
    <AlertDialog open={open} onOpenChange={(o) => !o && onClose()}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{t("lookup.unassign.confirmTitle")}</AlertDialogTitle>
          <AlertDialogDescription>
            {t("lookup.unassign.confirmBody", { product: productName, size: sizeLabel })}
            {currentBin && (
              <span className="block mt-1 font-mono text-xs text-muted-foreground">
                {t("lookup.binLocation")}: {currentBin}
              </span>
            )}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel onClick={onClose} disabled={assign.isPending}>
            {t("common.cancel")}
          </AlertDialogCancel>
          <AlertDialogAction onClick={handleConfirm} disabled={assign.isPending}>
            {assign.isPending && <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />}
            {t("common.confirm")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
