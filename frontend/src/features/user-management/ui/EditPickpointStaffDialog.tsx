"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { reassignPickpointStaff, fetchPickpoints } from "../api";
import type { UserDto } from "../types";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";

interface Props {
  open: boolean;
  onClose: () => void;
  target: UserDto;
}

export function EditPickpointStaffDialog({ open, onClose, target }: Props) {
  const qc = useQueryClient();
  const [pickpointId, setPickpointId] = useState(String(target.pickpointId ?? ""));

  const { data: pickpoints = [] } = useQuery({
    queryKey: ["admin-pickpoints"],
    queryFn: fetchPickpoints,
    staleTime: 60_000,
  });

  const mutation = useMutation({
    mutationFn: () => reassignPickpointStaff(target.id, parseInt(pickpointId)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin-users", "pickpoint-staff"] });
      toast.success("Pickpoint assignment updated");
      onClose();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) onClose(); }}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Reassign Pickpoint</DialogTitle>
        </DialogHeader>
        <div className="space-y-5">
          <div className="bg-muted/30 rounded-xl p-4 space-y-1">
            <p className="text-xs text-muted-foreground">Staff member</p>
            <p className="text-sm font-medium">{target.name || target.phone}</p>
            <p className="text-xs text-muted-foreground">{target.phone}</p>
          </div>

          <div className="space-y-2">
            <Label>Assign to Pickpoint</Label>
            <Select value={pickpointId} onValueChange={setPickpointId}>
              <SelectTrigger>
                <SelectValue placeholder="Select a pickpoint" />
              </SelectTrigger>
              <SelectContent>
                {pickpoints.map((p) => (
                  <SelectItem key={p.id} value={String(p.id)}>
                    {p.name} — {p.address}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button onClick={() => mutation.mutate()} disabled={!pickpointId || mutation.isPending}>
              {mutation.isPending ? "Saving…" : "Save"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
