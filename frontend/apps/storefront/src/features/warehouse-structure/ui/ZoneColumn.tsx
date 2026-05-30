"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Trash2, MapPin } from "lucide-react";
import { useQueries } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
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
import { useWarehouseZones, useShelvesByZone } from "@/entities/warehouse-location";
import type { WarehouseZoneDto, WarehouseShelfDto, WarehouseBinDto } from "@/entities/warehouse-location";
import { getErrorMessage } from "@radolfa/shared/lib";
import { useDeleteZone } from "../api";
import { CreateZoneDialog } from "./CreateZoneDialog";

interface ZoneDeleteConfirmProps {
  zone: WarehouseZoneDto;
  onClose: () => void;
}

function ZoneDeleteConfirm({ zone, onClose }: ZoneDeleteConfirmProps) {
  const t = useTranslations("warehouse");
  const { data: shelves, isLoading: shelvesLoading } = useShelvesByZone(zone.id);
  const shelfIds = shelves?.map((s) => s.id) ?? [];

  const binResults = useQueries({
    queries: shelfIds.map((shelfId) => ({
      queryKey: ["warehouse-bins", shelfId],
      queryFn: () =>
        apiClient
          .get<WarehouseBinDto[]>(`/api/v1/admin/warehouse/shelves/${shelfId}/bins`)
          .then((r) => r.data),
    })),
  });

  const shelfCount = shelves?.length ?? 0;
  const binCount = binResults.reduce((acc, q) => acc + (q.data?.length ?? 0), 0);
  const countsLoading = shelvesLoading || binResults.some((q) => q.isLoading);

  const deleteZone = useDeleteZone();

  function handleDelete() {
    deleteZone.mutate(zone.id, {
      onSuccess: () => {
        toast.success(t("structure.delete.zone.success"));
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
            {t("structure.delete.zone.title", { code: zone.code })}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {countsLoading
              ? t("structure.delete.zone.loadingCounts")
              : t("structure.delete.zone.body", { code: zone.code, shelfCount, binCount })}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel onClick={onClose}>{t("common.cancel")}</AlertDialogCancel>
          <AlertDialogAction
            className="bg-rose-600 hover:bg-rose-700 text-white"
            onClick={handleDelete}
            disabled={deleteZone.isPending || countsLoading}
          >
            {t("common.delete")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

interface Props {
  selectedZoneId: number | null;
  onSelectZone: (id: number) => void;
}

export function ZoneColumn({ selectedZoneId, onSelectZone }: Props) {
  const t = useTranslations("warehouse");
  const { data: zones, isLoading } = useWarehouseZones();
  const [deletingZone, setDeletingZone] = useState<WarehouseZoneDto | null>(null);

  return (
    <div className="col-span-3 flex flex-col min-h-0">
      <Card className="flex flex-col flex-1 min-h-0 overflow-hidden">
        <div className="flex items-center justify-between px-4 py-3 border-b shrink-0">
          <span className="text-sm font-semibold">{t("structure.zones.title")}</span>
          <CreateZoneDialog />
        </div>

        <div className="flex-1 overflow-y-auto">
          {isLoading ? (
            <div className="p-3 space-y-2">
              {[...Array(3)].map((_, i) => (
                <Skeleton key={i} className="h-10 w-full rounded-lg" />
              ))}
            </div>
          ) : !zones || zones.length === 0 ? (
            <div className="flex flex-col items-center justify-center border border-dashed rounded-xl m-3 p-8 text-muted-foreground gap-2">
              <MapPin className="h-8 w-8 text-muted-foreground/40" />
              <p className="text-xs text-center">{t("structure.zones.empty")}</p>
            </div>
          ) : (
            <div className="p-2 space-y-0.5">
              {zones.map((zone) => (
                <div
                  key={zone.id}
                  className={`group flex items-center justify-between px-3 py-2.5 rounded-lg cursor-pointer transition-colors ${
                    selectedZoneId === zone.id ? "bg-muted" : "hover:bg-muted/50"
                  }`}
                  onClick={() => onSelectZone(zone.id)}
                >
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium truncate">
                      {zone.code}
                      {zone.label && (
                        <span className="text-muted-foreground font-normal"> — {zone.label}</span>
                      )}
                    </p>
                  </div>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-7 w-7 p-0 opacity-0 group-hover:opacity-100 text-rose-500 hover:text-rose-600 hover:bg-rose-50 shrink-0 ml-1"
                        onClick={(e) => {
                          e.stopPropagation();
                          setDeletingZone(zone);
                        }}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
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

      {deletingZone && (
        <ZoneDeleteConfirm
          zone={deletingZone}
          onClose={() => setDeletingZone(null)}
        />
      )}
    </div>
  );
}
