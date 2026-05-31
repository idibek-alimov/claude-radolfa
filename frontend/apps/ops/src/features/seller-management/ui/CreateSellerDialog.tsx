"use client";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@radolfa/shared/ui/dialog";
import { Button } from "@radolfa/shared/ui/button";
import { Input } from "@radolfa/shared/ui/input";
import { Label } from "@radolfa/shared/ui/label";
import { useCreateSeller } from "../api";

interface Props {
  open: boolean;
  onClose: () => void;
}

export function CreateSellerDialog({ open, onClose }: Props) {
  const [phone, setPhone] = useState("");
  const [shopName, setShopName] = useState("");
  const [logoUrl, setLogoUrl] = useState("");
  const [bio, setBio] = useState("");

  const mutation = useCreateSeller();

  function handleClose() {
    setPhone("");
    setShopName("");
    setLogoUrl("");
    setBio("");
    onClose();
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!phone.trim() || !shopName.trim()) return;
    mutation.mutate(
      {
        phone: phone.trim(),
        shopName: shopName.trim(),
        logoUrl: logoUrl.trim() || undefined,
        bio: bio.trim() || undefined,
      },
      { onSuccess: handleClose }
    );
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && handleClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Create Seller</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-5 mt-2">
          <div className="space-y-1.5">
            <Label htmlFor="cs-phone">Phone *</Label>
            <Input
              id="cs-phone"
              type="tel"
              placeholder="+992 XX XXX XX XX"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="cs-shop">Shop name *</Label>
            <Input
              id="cs-shop"
              placeholder="Brand shop name"
              value={shopName}
              onChange={(e) => setShopName(e.target.value)}
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="cs-logo">Logo URL</Label>
            <Input
              id="cs-logo"
              type="url"
              placeholder="https://…"
              value={logoUrl}
              onChange={(e) => setLogoUrl(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="cs-bio">Bio</Label>
            <Input
              id="cs-bio"
              placeholder="Short description"
              value={bio}
              onChange={(e) => setBio(e.target.value)}
            />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={handleClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Creating…" : "Create seller"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
