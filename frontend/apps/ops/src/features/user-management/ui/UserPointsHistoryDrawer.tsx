"use client";

import { useState, useEffect } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ChevronLeft, ChevronRight } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@radolfa/shared/ui/sheet";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@radolfa/shared/ui/table";
import { Button } from "@radolfa/shared/ui/button";
import { Input } from "@radolfa/shared/ui/input";
import { Label } from "@radolfa/shared/ui/label";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { cn } from "@radolfa/shared/lib";
import { getErrorMessage } from "@radolfa/shared/lib";
import { formatDate } from "@radolfa/shared/lib/format";
import { useAuth } from "@radolfa/shared/auth";
import { useUserLoyaltyLedger, adjustUserPoints } from "@/entities/loyalty/api";
import type { LoyaltyReason } from "@/entities/loyalty/model/types";
import { toast } from "sonner";

const PAGE_SIZE = 20;

// Pill styles per reason — credit reasons are green/teal/blue family,
// debit reasons are rose/orange/zinc family.
const REASON_STYLES: Record<LoyaltyReason, string> = {
  EARN_CASHBACK:      "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200",
  REVIEW_BONUS:       "bg-teal-50 text-teal-700 ring-1 ring-teal-200",
  RESTORE:            "bg-sky-50 text-sky-700 ring-1 ring-sky-200",
  OPENING_BALANCE:    "bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200",
  MANUAL_ADJUSTMENT:  "bg-amber-50 text-amber-700 ring-1 ring-amber-200",
  REDEEM:             "bg-rose-50 text-rose-700 ring-1 ring-rose-200",
  REVOKE:             "bg-orange-50 text-orange-700 ring-1 ring-orange-200",
  EXPIRE:             "bg-zinc-100 text-zinc-500 ring-1 ring-zinc-200",
};

interface Props {
  open: boolean;
  onClose: () => void;
  userId: number | null;
  userName: string | undefined;
}

export function UserPointsHistoryDrawer({ open, onClose, userId, userName }: Props) {
  const t = useTranslations("manage");
  const { user: currentUser } = useAuth();
  const qc = useQueryClient();

  const isAdmin = currentUser?.role === "ADMIN";

  const [page, setPage] = useState(1);
  useEffect(() => { setPage(1); }, [userId]);

  // Adjust form state
  const [delta, setDelta] = useState("");
  const [reason, setReason] = useState("");

  const { data, isLoading } = useUserLoyaltyLedger(open ? userId : null, page);

  const adjustMutation = useMutation({
    mutationFn: () =>
      adjustUserPoints({ userId: userId!, delta: Number(delta), reason }),
    onSuccess: (result) => {
      toast.success(
        t("pointsHistory.adjust.success", { balance: result.balance })
      );
      qc.invalidateQueries({ queryKey: ["user-loyalty-ledger", userId] });
      qc.invalidateQueries({ queryKey: ["admin-users"] });
      setPage(1);
      setDelta("");
      setReason("");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const canSubmit =
    delta !== "" &&
    Number(delta) !== 0 &&
    reason.trim().length > 0 &&
    !adjustMutation.isPending;

  return (
    <Sheet open={open} onOpenChange={(o) => !o && onClose()}>
      <SheetContent side="right" className="sm:max-w-3xl flex flex-col">
        <SheetHeader className="shrink-0">
          <SheetTitle>{t("pointsHistory.title")}</SheetTitle>
          {userName && (
            <p className="text-sm text-muted-foreground mt-1">{userName}</p>
          )}
        </SheetHeader>

        {/* ADMIN-only manual adjustment form */}
        {isAdmin && (
          <div className="shrink-0 bg-muted/30 rounded-xl p-4 space-y-3 mt-4">
            <p className="text-sm font-semibold">{t("pointsHistory.adjust.title")}</p>
            <div className="flex gap-3 items-end">
              <div className="space-y-1.5 w-32">
                <Label className="text-xs">{t("pointsHistory.adjust.deltaLabel")}</Label>
                <Input
                  type="number"
                  placeholder="e.g. 100 or -50"
                  value={delta}
                  onChange={(e) => setDelta(e.target.value)}
                  className="h-8 text-sm"
                />
              </div>
              <div className="space-y-1.5 flex-1">
                <Label className="text-xs">{t("pointsHistory.adjust.reasonLabel")}</Label>
                <Input
                  placeholder={t("pointsHistory.adjust.reasonLabel")}
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className="h-8 text-sm"
                />
              </div>
              <Button
                size="sm"
                disabled={!canSubmit}
                onClick={() => adjustMutation.mutate()}
                className="h-8 shrink-0"
              >
                {adjustMutation.isPending
                  ? t("pointsHistory.adjust.submitting")
                  : t("pointsHistory.adjust.submit")}
              </Button>
            </div>
          </div>
        )}

        <div className="flex-1 overflow-y-auto mt-4">
          {isLoading ? (
            <div className="space-y-2">
              {[...Array(6)].map((_, i) => (
                <Skeleton key={i} className="h-10 w-full rounded-lg" />
              ))}
            </div>
          ) : !data || data.content.length === 0 ? (
            <div className="flex items-center justify-center border border-dashed rounded-xl p-12 text-muted-foreground">
              <p className="text-sm">{t("pointsHistory.empty")}</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("pointsHistory.columns.when")}</TableHead>
                  <TableHead className="text-right">{t("pointsHistory.columns.change")}</TableHead>
                  <TableHead>{t("pointsHistory.columns.reason")}</TableHead>
                  <TableHead className="text-right">{t("pointsHistory.columns.balance")}</TableHead>
                  <TableHead>{t("pointsHistory.columns.expires")}</TableHead>
                  <TableHead>{t("pointsHistory.columns.by")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((entry) => (
                  <TableRow key={entry.id}>
                    <TableCell
                      className="text-xs text-muted-foreground whitespace-nowrap"
                      title={entry.createdAt}
                    >
                      {formatDate(entry.createdAt)}
                    </TableCell>

                    <TableCell className="text-right font-medium tabular-nums">
                      <span
                        className={
                          entry.delta > 0 ? "text-emerald-600" : "text-rose-600"
                        }
                      >
                        {entry.delta > 0 ? `+${entry.delta}` : entry.delta}
                      </span>
                    </TableCell>

                    <TableCell>
                      <span
                        className={cn(
                          "inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide",
                          REASON_STYLES[entry.reason] ?? "bg-zinc-100 text-zinc-600",
                        )}
                      >
                        {t(`pointsHistory.reasons.${camelReason(entry.reason)}`)}
                      </span>
                    </TableCell>

                    <TableCell className="text-right tabular-nums text-sm">
                      {entry.balanceAfter}
                    </TableCell>

                    <TableCell className="text-xs text-muted-foreground whitespace-nowrap">
                      {entry.expiresAt ? formatDate(entry.expiresAt) : "—"}
                    </TableCell>

                    <TableCell className="text-xs text-muted-foreground">
                      {entry.actorUserId ? (
                        <span>
                          User #{entry.actorUserId}
                          {entry.note && (
                            <span
                              className="block text-[11px] text-muted-foreground/70 truncate max-w-[120px]"
                              title={entry.note}
                            >
                              {entry.note}
                            </span>
                          )}
                        </span>
                      ) : (
                        "—"
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>

        {/* Pagination */}
        {data && data.totalElements > PAGE_SIZE && (
          <div className="flex items-center justify-between text-sm text-muted-foreground shrink-0 pt-4 border-t border-zinc-100 mt-4">
            <span>
              {(page - 1) * PAGE_SIZE + 1}–
              {Math.min(page * PAGE_SIZE, data.totalElements)} / {data.totalElements}
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p - 1)}
                disabled={page <= 1}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={data.last}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}

/** Convert SCREAMING_SNAKE reason enum to camelCase for i18n key lookup. */
function camelReason(reason: LoyaltyReason): string {
  return reason
    .toLowerCase()
    .replace(/_([a-z])/g, (_, c: string) => c.toUpperCase());
}
