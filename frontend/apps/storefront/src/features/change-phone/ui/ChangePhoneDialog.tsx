"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { AlertCircle, CheckCircle2 } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@radolfa/shared/ui/dialog";
import { Button } from "@radolfa/shared/ui/button";
import { Input } from "@radolfa/shared/ui/input";
import { Label } from "@radolfa/shared/ui/label";
import OtpInput from "@radolfa/shared/auth/ui/OtpInput";
import { useAuth } from "@radolfa/shared/auth";
import { getErrorMessage } from "@radolfa/shared/lib";
import { useRequestPhoneChange, useVerifyPhoneChange } from "../api";

const OTP_BOX_CLASS =
  "w-14 h-14 rounded-2xl bg-plum/50 focus:bg-white focus:ring-2 focus:ring-mag focus:outline-none text-center text-[24px] font-extrabold";

type Step = "number" | "otp" | "success";

interface ChangePhoneDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ChangePhoneDialog({ open, onOpenChange }: ChangePhoneDialogProps) {
  const t = useTranslations("profile");
  const tAuth = useTranslations("auth");
  const { refreshUser } = useAuth();
  const [step, setStep] = useState<Step>("number");
  const [phone, setPhone] = useState("");
  const [otp, setOtp] = useState("");
  const [error, setError] = useState<string | null>(null);

  const requestMutation = useRequestPhoneChange();
  const verifyMutation = useVerifyPhoneChange();

  // Reset all local state whenever the dialog closes.
  useEffect(() => {
    if (!open) {
      setStep("number");
      setPhone("");
      setOtp("");
      setError(null);
    }
  }, [open]);

  function handleSendCode() {
    const trimmed = phone.trim().replace(/[\s-]/g, "");
    if (!trimmed) {
      setError(tAuth("phoneRequired"));
      return;
    }
    if (!/^\+?992\d{9}$/.test(trimmed)) {
      setError(tAuth("invalidPhone"));
      return;
    }
    setError(null);
    requestMutation.mutate(
      { phone: trimmed },
      {
        onSuccess: () => setStep("otp"),
        onError: (err) => setError(getErrorMessage(err)),
      }
    );
  }

  function handleResend() {
    const trimmed = phone.trim().replace(/[\s-]/g, "");
    requestMutation.mutate(
      { phone: trimmed },
      { onError: (err) => setError(getErrorMessage(err)) }
    );
  }

  function handleVerify() {
    if (!otp.trim()) {
      setError(tAuth("otpRequired"));
      return;
    }
    const trimmed = phone.trim().replace(/[\s-]/g, "");
    setError(null);
    verifyMutation.mutate(
      { phone: trimmed, otp: otp.trim() },
      {
        onSuccess: () => {
          refreshUser();
          toast.success(t("phoneChanged"));
          setStep("success");
        },
        onError: (err) => setError(getErrorMessage(err)),
      }
    );
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent overlayClassName="bg-black/30" className="max-w-md">
        <DialogHeader>
          <DialogTitle>{t("changePhoneTitle")}</DialogTitle>
        </DialogHeader>

        {error && (
          <div className="flex items-center gap-2 bg-mag/10 border border-mag/20 text-mag rounded-2xl px-4 py-3 text-sm">
            <AlertCircle className="h-4 w-4 shrink-0" />
            {error}
          </div>
        )}

        {step === "number" && (
          <div className="space-y-4">
            <p className="text-ink/55 text-[13px]">{t("changePhoneIntro")}</p>
            <div className="space-y-1.5">
              <Label htmlFor="new-phone">{t("newPhoneNumber")}</Label>
              <Input
                id="new-phone"
                type="tel"
                inputMode="tel"
                placeholder="+992 123 456 789"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
            </div>
            <Button
              className="w-full rounded-full bg-mag text-white hover:bg-maglo"
              disabled={requestMutation.isPending}
              onClick={handleSendCode}
            >
              {requestMutation.isPending ? tAuth("sending") : t("sendCode")}
            </Button>
          </div>
        )}

        {step === "otp" && (
          <div className="space-y-4">
            <p className="text-ink/55 text-[13px]">{tAuth("sentCodeTo", { phone })}</p>
            <div className="flex gap-3 justify-center">
              <OtpInput value={otp} onChange={setOtp} boxClassName={OTP_BOX_CLASS} />
            </div>
            <div className="flex gap-3">
              <Button
                type="button"
                variant="outline"
                className="flex-1 rounded-full border-2 border-mag/30 text-mag hover:bg-mag/5"
                onClick={() => setStep("number")}
              >
                {tAuth("back")}
              </Button>
              <Button
                className="flex-1 rounded-full bg-mag text-white hover:bg-maglo"
                disabled={verifyMutation.isPending}
                onClick={handleVerify}
              >
                {verifyMutation.isPending ? tAuth("verifying") : tAuth("verify")}
              </Button>
            </div>
            <p className="text-center text-[13px] text-ink/50">
              {tAuth("resendPrefix")}{" "}
              <button
                type="button"
                onClick={handleResend}
                disabled={requestMutation.isPending}
                className="text-mag font-bold disabled:opacity-60"
              >
                {requestMutation.isPending ? tAuth("resending") : tAuth("resendCode")}
              </button>
            </p>
          </div>
        )}

        {step === "success" && (
          <div className="space-y-4 flex flex-col items-center text-center py-2">
            <div className="w-12 h-12 rounded-full bg-emerald/10 text-emerald flex items-center justify-center">
              <CheckCircle2 className="h-6 w-6" />
            </div>
            <p className="text-[14px] font-bold">{t("phoneChanged")}</p>
            <Button
              className="w-full rounded-full bg-mag text-white hover:bg-maglo"
              onClick={() => onOpenChange(false)}
            >
              {t("phoneChangedDone")}
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
