"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useLoyaltyTiers, updateTierColor } from "@/entities/loyalty";
import type { LoyaltyTier } from "@/entities/loyalty";
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@/shared/ui/table";
import { Button } from "@/shared/ui/button";
import { Skeleton } from "@/shared/ui/skeleton";
import { getErrorMessage } from "@/shared/lib";
import { Lock, Check, Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";

export function LoyaltyTierManagementTable() {
  const t = useTranslations("manage");
  const pt = useTranslations("profile");
  const queryClient = useQueryClient();
  const { data: tiers, isLoading } = useLoyaltyTiers();

  const [pendingColors, setPendingColors] = useState<Record<number, string>>({});

  const colorMutation = useMutation({
    mutationFn: ({ id, color }: { id: number; color: string }) =>
      updateTierColor(id, color),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["loyalty-tiers"] });
      setPendingColors((prev) => {
        const next = { ...prev };
        delete next[variables.id];
        return next;
      });
      toast.success(t("colorUpdated"));
    },
    onError: (err: unknown) => {
      toast.error(getErrorMessage(err, "Failed to update color"));
    },
  });

  const handleColorChange = (tier: LoyaltyTier, color: string) => {
    setPendingColors((prev) => ({ ...prev, [tier.id]: color }));
  };

  const handleSaveColor = (tier: LoyaltyTier) => {
    const color = pendingColors[tier.id];
    if (!color) return;
    colorMutation.mutate({ id: tier.id, color });
  };

  const hasChanged = (tier: LoyaltyTier) =>
    pendingColors[tier.id] !== undefined && pendingColors[tier.id] !== tier.color;

  if (isLoading) {
    return (
      <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-14 w-full" />
        ))}
      </div>
    );
  }

  if (!tiers || tiers.length === 0) {
    return (
      <div className="bg-card rounded-xl border shadow-sm p-12 text-center text-muted-foreground">
        {t("loyaltyTiers")}
      </div>
    );
  }

  return (
    <div className="bg-card rounded-xl border shadow-sm">
      <div className="px-6 py-4 border-b">
        <h2 className="text-lg font-semibold">{t("loyaltyTiers")}</h2>
      </div>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="pl-4 w-[48px]">{t("tierColor")}</TableHead>
            <TableHead>{pt("allTiers")}</TableHead>
            <TableHead>{pt("discount")}</TableHead>
            <TableHead>{pt("cashback")}</TableHead>
            <TableHead>{pt("minSpend")}</TableHead>
            <TableHead>{t("tierColor")}</TableHead>
            <TableHead className="text-right pr-4">{t("tableActions")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {tiers.map((tier) => (
            <TableRow key={tier.id}>
              <TableCell className="pl-4">
                <div
                  className="h-6 w-6 rounded-full border"
                  style={{ backgroundColor: pendingColors[tier.id] ?? tier.color }}
                />
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-1.5">
                  <Lock className="h-3 w-3 text-muted-foreground shrink-0" />
                  <span className="font-medium text-sm">{tier.name}</span>
                </div>
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-1">
                  <Lock className="h-3 w-3 text-muted-foreground" />
                  <span className="text-sm">{tier.discountPercentage}%</span>
                </div>
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-1">
                  <Lock className="h-3 w-3 text-muted-foreground" />
                  <span className="text-sm">{tier.cashbackPercentage}%</span>
                </div>
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-1">
                  <Lock className="h-3 w-3 text-muted-foreground" />
                  <span className="text-sm">{tier.minSpendRequirement} TJS</span>
                </div>
              </TableCell>
              <TableCell>
                <input
                  type="color"
                  value={pendingColors[tier.id] ?? tier.color}
                  onChange={(e) => handleColorChange(tier, e.target.value)}
                  className="h-8 w-12 cursor-pointer rounded border border-input bg-transparent p-0.5"
                />
              </TableCell>
              <TableCell className="text-right pr-4">
                {hasChanged(tier) && (
                  <Button
                    size="sm"
                    onClick={() => handleSaveColor(tier)}
                    disabled={colorMutation.isPending}
                    className="gap-1"
                  >
                    {colorMutation.isPending ? (
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    ) : (
                      <Check className="h-3.5 w-3.5" />
                    )}
                    {t("save")}
                  </Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
