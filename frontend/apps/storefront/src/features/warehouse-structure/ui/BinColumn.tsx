"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Trash2, Box } from "lucide-react";
import { Button } from "@radolfa/shared/ui/button";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { Card } from "@radolfa/shared/ui/card";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@radolfa/shared/ui/alert-dialog";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@radolfa/shared/ui/tooltip";
import { useBinsByShelf } from "@/entities/warehouse-location";
import type { WarehouseBinDto } from "@/entities/warehouse-location";
import { getErrorMessage } from "@radolfa/shared/lib";
import { useDeleteBin } from "../api";
import { CreateBinDialog } from "./CreateBinDialog";

interface BinDeleteConfirmProps {
  bin: WarehouseBinDto;
  shelfId: number;
  onClose: () => void;
}

function BinDeleteConfirm({ bin, shelfId, onClose }: BinDeleteConfirmProps) {
  const t = useTranslations("warehouse");
  const deleteBin = useDeleteBin(shelfId);

  function handleDelete() {
    deleteBin.mutate(bin.id, {
      onSuccess: () => {
        toast.success(t("structure.delete.bin.success"));
        onClose();
      },
      onError: (err) => {
        toast.error(getErrorMessage(err, t("structure.errors.delete")));
        onClose();
      },
    });
  }

  return (
    <AlertDialog open onOpenChange={(open) => { if (!open) onClose(); }}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            {t("structure.delete.bin.title", { code: bin.code })}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {t("structure.delete.bin.body", { code: bin.code })}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel onClick={onClose}>{t("common.cancel")}</AlertDialogCancel>
          <AlertDialogAction
            className="bg-rose-600 hover:bg-rose-700 text-white"
            onClick={handleDelete}
            disabled={deleteBin.isPending}
          >
            {t("common.delete")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

interface Props {
  shelfId: number | null;
  shelfCode: string;
}

export function BinColumn({ shelfId, shelfCode }: Props) {
  const t = useTranslations("warehouse");
  const { data: bins, isLoading } = useBinsByShelf(shelfId);
  const [deletingBin, setDeletingBin] = useState<WarehouseBinDto | null>(null);

  return (
    <div className="col-span-5 flex flex-col min-h-0">
      <Card className={`flex flex-col flex-1 min-h-0 overflow-hidden ${!shelfId ? "opacity-60" : ""}`}>
        <div className="flex items-center justify-between px-4 py-3 border-b shrink-0">
          <span className="text-sm font-semibold">{t("structure.bins.title")}</span>
          {shelfId && (
            <CreateBinDialog
              shelfId={shelfId}
              shelfLabel={shelfCode}
              disabled={!shelfId}
            />
          )}
        </div>

        <div className="flex-1 overflow-y-auto">
          {!shelfId ? (
            <div className="flex flex-col items-center justify-center m-3 p-8 text-muted-foreground gap-2">
              <p className="text-xs text-center">{t("structure.bins.selectToShow")}</p>
            </div>
          ) : isLoading ? (
            <div className="p-3 space-y-2">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-9 w-full rounded-lg" />
              ))}
            </div>
          ) : !bins || bins.length === 0 ? (
            <div className="flex flex-col items-center justify-center border border-dashed rounded-xl m-3 p-8 text-muted-foreground gap-2">
              <Box className="h-8 w-8 text-muted-foreground/40" />
              <p className="text-xs text-center">{t("structure.bins.empty")}</p>
            </div>
          ) : (
            <div className="p-2 grid grid-cols-3 gap-1">
              {bins.map((bin) => (
                <div
                  key={bin.id}
                  className="group relative flex items-center justify-between px-3 py-2 rounded-lg border bg-card hover:bg-muted/50 transition-colors"
                >
                  <span className="text-sm font-mono font-medium">{bin.code}</span>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-6 w-6 p-0 opacity-0 group-hover:opacity-100 text-rose-500 hover:text-rose-600 hover:bg-rose-50"
                        onClick={() => setDeletingBin(bin)}
                      >
                        <Trash2 className="h-3 w-3" />
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>{t("common.delete")}</TooltipContent>
                  </Tooltip>
                </div>
              ))}
            </div>
          )}
        </div>
      </Card>

      {deletingBin && shelfId && (
        <BinDeleteConfirm
          bin={deletingBin}
          shelfId={shelfId}
          onClose={() => setDeletingBin(null)}
        />
      )}
    </div>
  );
}
