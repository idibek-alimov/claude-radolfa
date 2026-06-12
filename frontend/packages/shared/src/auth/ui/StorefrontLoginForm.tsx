"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { sendOtp, verifyOtp } from "../api";
import OtpInput from "./OtpInput";
import { AlertCircle } from "lucide-react";
import { useTranslations } from "next-intl";
import { opsUrl, storefrontUrl } from "../../lib/appNav";

type Step = "phone" | "otp";

const OTP_BOX_CLASS =
  "w-14 h-16 rounded-2xl bg-plum/50 focus:bg-white focus:ring-2 focus:ring-mag focus:outline-none text-center text-[24px] font-extrabold";

export default function StorefrontLoginForm() {
  const t = useTranslations("auth");
  const [step, setStep] = useState<Step>("phone");
  const [phone, setPhone] = useState("");
  const [otp, setOtp] = useState("");
  const [error, setError] = useState<string | null>(null);

  const sendOtpMutation = useMutation({
    mutationFn: sendOtp,
    onSuccess: () => {
      setStep("otp");
      setError(null);
    },
    onError: (err: Error) => {
      setError(err.message || t("failedToSendOtp"));
    },
  });

  const verifyOtpMutation = useMutation({
    mutationFn: verifyOtp,
    onSuccess: (auth) => {
      const role = auth.user.role;
      // Cross-app redirect: staff → ops (port-swapped in dev, relative behind nginx),
      // regular user → storefront home.
      const target =
        role === "SELLER"            ? opsUrl("/ops/seller")    :
        role === "COURIER"           ? opsUrl("/ops/courier")   :
        role === "PICKPOINT_STAFF"   ? opsUrl("/ops/pickpoint") :
        role === "WAREHOUSE_MANAGER" ? opsUrl("/ops/warehouse") :
        role === "ADMIN" || role === "MANAGER" ? opsUrl("/ops/manage") :
        storefrontUrl("/");
      window.location.href = target;
    },
    onError: (err: Error) => {
      setError(err.message || t("invalidOtp"));
    },
  });

  const handleSendOtp = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = phone.trim();
    if (!trimmed) {
      setError(t("phoneRequired"));
      return;
    }
    // Tajik phone: +992 followed by 9 digits
    if (!/^\+?992\d{9}$/.test(trimmed.replace(/[\s-]/g, ""))) {
      setError(t("invalidPhone"));
      return;
    }
    sendOtpMutation.mutate({ phone: trimmed });
  };

  const handleVerifyOtp = (e: React.FormEvent) => {
    e.preventDefault();
    if (!otp.trim()) {
      setError(t("otpRequired"));
      return;
    }
    verifyOtpMutation.mutate({ phone, otp });
  };

  const handleBack = () => {
    setStep("phone");
    setOtp("");
    setError(null);
  };

  const handleResend = () => {
    sendOtpMutation.mutate({ phone });
  };

  const renderForm = (variant: "desktop" | "mobile") => (
    <>
      {error && (
        <div className="flex items-center gap-2 bg-mag/10 border border-mag/20 text-mag rounded-2xl px-4 py-3 text-sm mb-5">
          <AlertCircle className="h-4 w-4 shrink-0" />
          {error}
        </div>
      )}

      {step === "phone" ? (
        <form onSubmit={handleSendOtp}>
          {variant === "desktop" && (
            <>
              <h2 className="text-[28px] font-extrabold leading-none">
                {t("signInTitle")}
              </h2>
              <p className="text-[14px] text-[#1A0A18]/55 mt-2">
                {t("signInPrompt")}
              </p>
            </>
          )}

          <div className={variant === "desktop" ? "mt-7" : ""}>
            <label className="text-[12px] font-bold text-[#1A0A18]/70 ml-1">
              {t("phoneNumber")}
            </label>
            <div className="relative mt-1.5">
              <svg
                width="17"
                height="17"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                className="absolute left-4 top-1/2 -translate-y-1/2 text-[#1A0A18]/40"
              >
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.13.96.36 1.9.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.91.34 1.85.57 2.81.7A2 2 0 0 1 22 16.92z" />
              </svg>
              <input
                id={`phone-${variant}`}
                type="tel"
                inputMode="tel"
                required
                placeholder="+992 123 456 789"
                className="w-full h-12 pl-11 pr-4 rounded-full bg-plum/50 text-[14px] focus:outline-none focus:bg-white focus:ring-2 focus:ring-mag"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
            </div>
          </div>

          {variant === "mobile" && (
            <p className="text-[12px] text-[#1A0A18]/55 mt-2 ml-1">
              {t("phoneHelp")}
            </p>
          )}

          <button
            type="submit"
            disabled={sendOtpMutation.isPending}
            className={`${variant === "desktop" ? "mt-5" : "mt-4"} w-full h-12 rounded-full bg-gradient-to-r from-mag to-maghi text-white font-bold text-[15px] shadow-lg shadow-mag/25 hover:brightness-105 transition disabled:opacity-60`}
          >
            {sendOtpMutation.isPending ? t("sending") : t("sendVerificationCode")}
          </button>

          <div className="mt-5 flex items-center gap-3 bg-[#FFCC4F]/15 border border-[#FFCC4F]/35 rounded-2xl px-4 py-3">
            <div className="w-10 h-10 rounded-full bg-[#FFCC4F] flex items-center justify-center shrink-0">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="#1A0A18">
                <path d="M5 16l-3-8 5.5 4L12 4l4.5 8L22 8l-3 8H5z" />
              </svg>
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-[13px] font-bold">{t("autoAccountTitle")}</div>
              <div className="text-[11px] text-[#1A0A18]/65 leading-tight mt-0.5">
                {t("autoAccountBody")}
              </div>
            </div>
          </div>

          <p className="text-[11px] text-[#1A0A18]/45 mt-4 leading-relaxed">
            {t("termsPrefix")}{" "}
            <Link href="/terms" className="text-mag font-semibold">
              {t("termsConditions")}
            </Link>{" "}
            {t("termsAnd")}{" "}
            <Link href="/privacy" className="text-mag font-semibold">
              {t("privacyNotice")}
            </Link>
            .
          </p>
        </form>
      ) : (
        <form onSubmit={handleVerifyOtp}>
          <h2
            className={
              variant === "desktop"
                ? "text-[28px] font-extrabold leading-none"
                : "text-[24px] font-extrabold leading-none"
            }
          >
            {t("verifyYourPhone")}
          </h2>
          <p
            className={
              variant === "desktop"
                ? "text-[14px] text-[#1A0A18]/55 mt-2"
                : "text-[13px] text-[#1A0A18]/55 mt-1.5"
            }
          >
            {t("sentCodeTo", { phone })}
          </p>

          <div className="mt-7 flex gap-3 justify-center">
            <OtpInput value={otp} onChange={setOtp} boxClassName={OTP_BOX_CLASS} />
          </div>

          <div className="mt-6 flex gap-3">
            <button
              type="button"
              onClick={handleBack}
              className="flex-1 h-12 rounded-full border-2 border-mag/30 text-mag font-bold text-[15px] hover:bg-mag/5 transition inline-flex items-center justify-center gap-1.5"
            >
              ← {t("back")}
            </button>
            <button
              type="submit"
              disabled={verifyOtpMutation.isPending}
              className="flex-1 h-12 rounded-full bg-gradient-to-r from-mag to-maghi text-white font-bold text-[15px] shadow-lg shadow-mag/25 hover:brightness-105 transition disabled:opacity-60"
            >
              {verifyOtpMutation.isPending ? t("verifying") : t("verify")}
            </button>
          </div>

          <p className="text-center text-[13px] text-[#1A0A18]/50 mt-5">
            {t("resendPrefix")}{" "}
            <button
              type="button"
              onClick={handleResend}
              disabled={sendOtpMutation.isPending}
              className="text-mag font-bold disabled:opacity-60"
            >
              {sendOtpMutation.isPending ? t("resending") : t("resendCode")}
            </button>
          </p>
        </form>
      )}
    </>
  );

  return (
    <div className="min-h-screen lg:grid lg:grid-cols-2 text-[#1A0A18]">
      {/* Desktop poster */}
      <div className="hidden lg:flex relative bg-gradient-to-br from-mag via-mag to-maglo text-white p-12 flex-col justify-between overflow-hidden">
        <span className="font-black text-2xl relative z-10">Radolfa</span>
        <div className="relative z-10">
          <div className="text-[12px] tracking-[0.2em] uppercase font-bold opacity-90">
            {t("welcomeBack")}
          </div>
          <h1 className="font-black text-[52px] leading-[1.02] mt-4">
            {t("posterHeadlineLead")}
            <br />
            <span className="text-[#FFCC4F]">{t("posterHeadlineAccent")}</span>
          </h1>
          <p className="text-[15px] opacity-90 mt-5 max-w-sm leading-relaxed">
            {t("posterTagline")}
          </p>
          <div className="flex items-center gap-5 mt-8">
            <div>
              <div className="font-black text-3xl">12%</div>
              <div className="text-[12px] opacity-80">{t("perkOffForever")}</div>
            </div>
            <div className="w-px h-10 bg-white/25" />
            <div>
              <div className="font-black text-3xl">70%</div>
              <div className="text-[12px] opacity-80">{t("perkSuperSale")}</div>
            </div>
            <div className="w-px h-10 bg-white/25" />
            <div>
              <div className="font-black text-3xl">0₸</div>
              <div className="text-[12px] opacity-80">{t("perkDelivery")}</div>
            </div>
          </div>
        </div>
        <div className="relative z-10 text-[12px] opacity-70">
          © {new Date().getFullYear()} Radolfa · Dushanbe
        </div>
        <span className="absolute -right-20 -top-16 w-72 h-72 rounded-full bg-white/10" />
        <span className="absolute -right-8 bottom-20 w-44 h-44 rounded-full bg-[#FFCC4F]/20" />
      </div>

      {/* Mobile poster */}
      <section className="lg:hidden relative bg-gradient-to-br from-mag via-mag to-maglo text-white px-5 pt-8 pb-12 overflow-hidden">
        <span className="font-black text-xl relative z-10">Radolfa</span>
        <div className="mt-7 relative z-10">
          <div className="text-[11px] tracking-[0.18em] uppercase font-bold opacity-90">
            {t("welcomeBack")}
          </div>
          <h1 className="font-black text-[32px] leading-[1.05] mt-2">
            {t("posterHeadlineLead")}
            <br />
            <span className="text-[#FFCC4F]">{t("posterHeadlineAccent")}</span>
          </h1>
        </div>
        <span className="absolute -right-12 -top-10 w-40 h-40 rounded-full bg-white/10" />
        <span className="absolute right-6 bottom-2 w-24 h-24 rounded-full bg-[#FFCC4F]/25" />
      </section>

      {/* Desktop form column */}
      <div className="hidden lg:flex items-center justify-center p-12">
        <div className="w-full max-w-[400px]">{renderForm("desktop")}</div>
      </div>

      {/* Mobile form sheet */}
      <div className="lg:hidden -mt-6 bg-white rounded-t-3xl px-5 pt-7 pb-10 relative z-10">
        {renderForm("mobile")}
      </div>
    </div>
  );
}
