"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Label } from "@/shared/ui/label";
import { Badge } from "@/shared/ui/badge";
import { useLoyaltyTiers } from "@/entities/loyalty/api";
import { changeUserRole, assignUserTier, setLoyaltyPermanent } from "../api";
import type { UserDto } from "../types";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";

const ROLE_RANK: Record<string, number> = { USER: 0, MANAGER: 1, ADMIN: 2 };

interface ManageUserDialogProps {
  open: boolean;
  onClose: () => void;
  target: UserDto;
  callerRole: string;
  callerId: number | null;
}

type Role = "USER" | "MANAGER" | "ADMIN";
const ALL_ROLES: Role[] = ["USER", "MANAGER", "ADMIN"];

export function ManageUserDialog({
  open,
  onClose,
  target,
  callerRole,
  callerId,
}: ManageUserDialogProps) {
  const qc = useQueryClient();
  const { data: tiers = [] } = useLoyaltyTiers();

  const [selectedRole, setSelectedRole] = useState<Role>(target.role);
  const [selectedTierId, setSelectedTierId] = useState<number | "">(
    target.loyalty.tier?.id ?? ""
  );

  const roleMutation = useMutation({
    mutationFn: () => changeUserRole({ userId: target.id, role: selectedRole }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin-users"] });
      toast.success(`Role updated to ${selectedRole}`);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const tierMutation = useMutation({
    mutationFn: () =>
      assignUserTier({ userId: target.id, tierId: selectedTierId as number }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin-users"] });
      toast.success("Tier assigned");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const permanentMutation = useMutation({
    mutationFn: (permanent: boolean) =>
      setLoyaltyPermanent({ userId: target.id, permanent }),
    onSuccess: (updatedUser) => {
      qc.invalidateQueries({ queryKey: ["admin-users"] });
      toast.success(
        updatedUser.loyalty.permanent
          ? "Tier locked — user is exempt from monthly evaluation"
          : "Tier unlocked — user will be evaluated monthly"
      );
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const isAdmin = callerRole === "ADMIN";
  const isManager = (ROLE_RANK[callerRole] ?? 0) >= ROLE_RANK["MANAGER"];
  const isSelf = callerId === target.id;

  const sortedTiers = [...tiers].sort((a, b) => a.displayOrder - b.displayOrder);

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Manage User</DialogTitle>
        </DialogHeader>

        {/* User summary */}
        <div className="flex items-center gap-3 py-2 border-b">
          <div>
            <p className="font-medium text-sm">{target.name || target.phone}</p>
            <p className="text-xs text-muted-foreground">{target.phone}</p>
          </div>
          <Badge variant="outline" className="ml-auto">{target.role}</Badge>
        </div>

        <div className="space-y-5 pt-1">
          {/* Role change — ADMIN only */}
          {isAdmin && !isSelf && (
            <div className="space-y-2">
              <Label>Role</Label>
              <div className="flex gap-2">
                {ALL_ROLES.map((role) => (
                  <button
                    key={role}
                    onClick={() => setSelectedRole(role)}
                    className={`flex-1 py-1.5 rounded border text-sm font-medium transition-colors ${
                      selectedRole === role
                        ? "bg-foreground text-background border-foreground"
                        : "border-border text-muted-foreground hover:border-foreground"
                    }`}
                  >
                    {role}
                  </button>
                ))}
              </div>
              <Button
                size="sm"
                className="w-full"
                disabled={selectedRole === target.role || roleMutation.isPending}
                onClick={() => roleMutation.mutate()}
              >
                {roleMutation.isPending ? "Saving…" : "Save Role"}
              </Button>
            </div>
          )}

          {/* Tier assignment — MANAGER+ */}
          {isManager && !isSelf && (
            <div className="space-y-2">
              <Label>Loyalty Tier</Label>
              {target.loyalty.permanent && (
                <p className="text-xs text-amber-600 font-medium">🔒 Tier is locked</p>
              )}
              <select
                className="w-full border rounded px-3 py-2 text-sm bg-background"
                value={selectedTierId}
                onChange={(e) =>
                  setSelectedTierId(
                    e.target.value === "" ? "" : Number(e.target.value)
                  )
                }
              >
                <option value="">No tier</option>
                {sortedTiers.map((tier) => (
                  <option key={tier.id} value={tier.id}>
                    {tier.name} — {tier.discountPercentage}% off
                  </option>
                ))}
              </select>
              {target.loyalty.floorTierName && (
                <p className="text-xs text-muted-foreground">
                  Floor tier: {target.loyalty.floorTierName}
                </p>
              )}
              <Button
                size="sm"
                className="w-full"
                disabled={
                  selectedTierId === (target.loyalty.tier?.id ?? "") ||
                  selectedTierId === "" ||
                  tierMutation.isPending
                }
                onClick={() => tierMutation.mutate()}
              >
                {tierMutation.isPending ? "Saving…" : "Assign Tier"}
              </Button>
            </div>
          )}

          {/* Loyalty lock — ADMIN only */}
          {isAdmin && !isSelf && (
            <div className="space-y-2 border-t pt-4">
              <Label>Tier Lock</Label>
              <p className="text-xs text-muted-foreground">
                When locked, this user's tier is never downgraded by the monthly evaluation job.
              </p>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium">
                    {target.loyalty.permanent ? "Locked" : "Unlocked"}
                  </p>
                  {target.loyalty.floorTierName && (
                    <p className="text-xs text-muted-foreground">
                      Floor: {target.loyalty.floorTierName}
                    </p>
                  )}
                </div>
                <Button
                  size="sm"
                  variant={target.loyalty.permanent ? "destructive" : "outline"}
                  disabled={permanentMutation.isPending}
                  onClick={() => permanentMutation.mutate(!target.loyalty.permanent)}
                >
                  {permanentMutation.isPending
                    ? "Saving…"
                    : target.loyalty.permanent
                    ? "Unlock"
                    : "Lock Tier"}
                </Button>
              </div>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
