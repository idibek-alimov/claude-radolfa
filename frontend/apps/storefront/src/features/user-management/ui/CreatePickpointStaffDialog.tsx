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
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { createPickpointStaff, fetchPickpoints } from "../api";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";

const DUPLICATE_PHONE_MSG =
  "This phone number is already registered. Find the user on the Customers tab and update their role.";

interface Props {
  open: boolean;
  onClose: () => void;
}

export function CreatePickpointStaffDialog({ open, onClose }: Props) {
  const qc = useQueryClient();

  const [phone, setPhone] = useState("");
  const [name, setName] = useState("");
  const [pickpointId, setPickpointId] = useState<string>("");
  const [phoneError, setPhoneError] = useState<string | null>(null);

  const { data: pickpoints = [] } = useQuery({
    queryKey: ["admin-pickpoints"],
    queryFn: fetchPickpoints,
    staleTime: 60_000,
  });

  const mutation = useMutation({
    mutationFn: createPickpointStaff,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin-users", "pickpoint-staff"] });
      toast.success("Pickpoint staff created");
      handleClose();
    },
    onError: (err: unknown) => {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 409) {
        setPhoneError(DUPLICATE_PHONE_MSG);
      } else {
        toast.error(getErrorMessage(err));
      }
    },
  });

  function handleClose() {
    setPhone(""); setName(""); setPickpointId(""); setPhoneError(null);
    onClose();
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setPhoneError(null);
    mutation.mutate({ phone, name, pickpointId: parseInt(pickpointId) });
  }

  const isValid = phone.trim() && name.trim() && pickpointId;

  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) handleClose(); }}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Add Pickpoint Staff</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="space-y-2">
            <Label>Phone *</Label>
            <Input
              value={phone}
              onChange={(e) => { setPhone(e.target.value); setPhoneError(null); }}
              placeholder="+992901234567"
            />
            {phoneError && (
              <p className="text-sm text-destructive">{phoneError}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label>Full Name *</Label>
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Sara Staff"
            />
          </div>

          <div className="space-y-2">
            <Label>Assign to Pickpoint *</Label>
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
            <Button type="button" variant="outline" onClick={handleClose}>Cancel</Button>
            <Button type="submit" disabled={!isValid || mutation.isPending}>
              {mutation.isPending ? "Creating…" : "Create Staff"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
