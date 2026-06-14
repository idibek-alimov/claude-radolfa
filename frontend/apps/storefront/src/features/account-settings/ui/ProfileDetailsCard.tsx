"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { useAuth } from "@radolfa/shared/auth";
import { Button } from "@radolfa/shared/ui/button";
import { Input } from "@radolfa/shared/ui/input";
import { Label } from "@radolfa/shared/ui/label";
import { getErrorMessage } from "@radolfa/shared/lib";
import { useUpdateProfile } from "../api";

/** Avatar initials: first letters of each word in `name`, falling back to the
 *  last two digits of `phone`. */
function getInitials(name?: string | null, phone?: string | null): string {
  if (name) {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  }
  return phone?.slice(-2) ?? "?";
}

export function ProfileDetailsCard() {
  const t = useTranslations("profile");
  const { user, updateUser, refreshUser } = useAuth();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");

  const updateMutation = useUpdateProfile();

  useEffect(() => {
    setName(user?.name ?? "");
    setEmail(user?.email ?? "");
  }, [user]);

  const isDirty = name !== (user?.name ?? "") || email !== (user?.email ?? "");
  const isValid = name.trim().length > 0;

  function handleSave() {
    updateMutation.mutate(
      { name: name.trim(), email: email.trim() },
      {
        onSuccess: (updated) => {
          updateUser(updated);
          refreshUser();
          toast.success(t("profileUpdated"));
        },
        onError: (err) => toast.error(getErrorMessage(err, t("failedToUpdateProfile"))),
      }
    );
  }

  return (
    <section className="rounded-3xl bg-white border border-ink/8 p-4 lg:p-6">
      <h2 className="font-black text-lg mb-4">{t("profileDetails")}</h2>

      <div className="flex items-center gap-4 mb-5">
        <div className="w-16 h-16 rounded-full bg-mag/15 text-mag flex items-center justify-center text-[20px] font-black shrink-0">
          {getInitials(user?.name, user?.phone)}
        </div>
        <Button type="button" variant="outline" disabled className="rounded-full border-2 border-ink/15 text-[12px] font-bold h-9 px-4">
          {t("changePhoto")}
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 text-[13px]">
        <div className="space-y-1.5">
          <Label htmlFor="settings-name">{t("nameLabel")}</Label>
          <Input
            id="settings-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="rounded-2xl bg-soft px-4 py-3 h-auto border-0"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="settings-email">
            {t("emailLabel")} <span className="text-ink/40">({t("optional")})</span>
          </Label>
          <Input
            id="settings-email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="rounded-2xl bg-soft px-4 py-3 h-auto border-0"
          />
        </div>
      </div>

      <Button
        className="mt-4 h-10 px-5 rounded-full bg-mag text-white font-bold text-[13px] hover:bg-maglo"
        disabled={!isDirty || !isValid || updateMutation.isPending}
        onClick={handleSave}
      >
        {updateMutation.isPending ? t("saving") : t("saveChanges")}
      </Button>
    </section>
  );
}
