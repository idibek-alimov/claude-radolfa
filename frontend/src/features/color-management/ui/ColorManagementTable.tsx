"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { fetchColors, updateColor } from "@/entities/color";
import type { Color } from "@/entities/product/model/types";
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@/shared/ui/table";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import { getErrorMessage } from "@/shared/lib";
import { CreateColorDialog } from "./CreateColorDialog";
import { Check, Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";

export function ColorManagementTable() {
  const t = useTranslations("manage");
  const queryClient = useQueryClient();

  const { data: colors, isLoading } = useQuery({
    queryKey: ["colors"],
    queryFn: fetchColors,
  });

  const [pending, setPending] = useState<
    Record<number, { displayName: string; hexCode: string }>
  >({});

  const updateMutation = useMutation({
    mutationFn: ({ id, displayName, hexCode }: { id: number; displayName: string; hexCode: string }) =>
      updateColor(id, displayName, hexCode),
    onSuccess: (result) => {
      toast.success(t("colorSaved"));
      setPending((prev) => {
        const next = { ...prev };
        delete next[result.id];
        return next;
      });
      queryClient.invalidateQueries({ queryKey: ["colors"] });
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const getDisplayName = (color: Color) => pending[color.id]?.displayName ?? color.displayName ?? "";
  const getHexCode = (color: Color) => pending[color.id]?.hexCode ?? color.hexCode ?? "#000000";

  const handleChange = (color: Color, field: "displayName" | "hexCode", value: string) => {
    setPending((prev) => ({
      ...prev,
      [color.id]: {
        displayName: getDisplayName(color),
        hexCode: getHexCode(color),
        [field]: value,
      },
    }));
  };

  const hasChanged = (color: Color) => {
    const p = pending[color.id];
    if (!p) return false;
    return (
      p.displayName !== (color.displayName ?? "") ||
      p.hexCode !== (color.hexCode ?? "#000000")
    );
  };

  if (isLoading) {
    return (
      <div className="bg-card rounded-xl border shadow-sm p-6 space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    );
  }

  if (!colors || colors.length === 0) {
    return (
      <div className="bg-card rounded-xl border shadow-sm p-12 text-center text-muted-foreground">
        {t("noColorsFound")}
      </div>
    );
  }

  return (
    <div className="bg-card rounded-xl border shadow-sm">
      <div className="px-6 py-4 border-b flex items-center justify-between">
        <h2 className="text-lg font-semibold">{t("colorTitle")}</h2>
        <CreateColorDialog />
      </div>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="pl-4 w-[48px]">{t("tierColor")}</TableHead>
            <TableHead>{t("colorKey")}</TableHead>
            <TableHead>{t("colorDisplayNameLabel")}</TableHead>
            <TableHead>{t("colorHexLabel")}</TableHead>
            <TableHead className="text-right pr-4">{t("tableActions")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {colors.map((color) => {
            const displayName = getDisplayName(color);
            const hexCode = getHexCode(color);
            return (
              <TableRow key={color.id}>
                <TableCell className="pl-4">
                  <div className="h-6 w-6 rounded-full border" style={{ backgroundColor: hexCode }} />
                </TableCell>
                <TableCell>
                  <code className="text-xs bg-muted px-1.5 py-0.5 rounded">{color.colorKey}</code>
                </TableCell>
                <TableCell>
                  <Input
                    value={displayName}
                    onChange={(e) => handleChange(color, "displayName", e.target.value)}
                    className="h-8 text-sm max-w-[180px]"
                  />
                </TableCell>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <input
                      type="color"
                      value={hexCode}
                      onChange={(e) => handleChange(color, "hexCode", e.target.value)}
                      className="h-8 w-10 cursor-pointer rounded border border-input bg-transparent p-0.5"
                    />
                    <Input
                      value={hexCode}
                      onChange={(e) => handleChange(color, "hexCode", e.target.value)}
                      className="h-8 text-sm w-24 font-mono"
                    />
                  </div>
                </TableCell>
                <TableCell className="text-right pr-4">
                  {hasChanged(color) && (
                    <Button
                      size="sm"
                      onClick={() => updateMutation.mutate({ id: color.id, displayName, hexCode })}
                      disabled={updateMutation.isPending}
                      className="gap-1"
                    >
                      {updateMutation.isPending ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      ) : (
                        <Check className="h-3.5 w-3.5" />
                      )}
                      {t("save")}
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
