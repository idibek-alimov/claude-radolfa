"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { sendOtp, verifyOtp } from "../api";
import OtpInput from "./OtpInput";
import { Package, AlertCircle, ArrowLeft, Phone } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";

type Step = "phone" | "otp";

export default function LoginForm() {
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
      setError(err.message || "Failed to send OTP");
    },
  });

  const verifyOtpMutation = useMutation({
    mutationFn: verifyOtp,
    onSuccess: () => {
      // Cookie is set by the server via Set-Cookie header.
      // No client-side token storage needed.
      window.location.href = "/";
    },
    onError: (err: Error) => {
      setError(err.message || "Invalid OTP");
    },
  });

  const handleSendOtp = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = phone.trim();
    if (!trimmed) {
      setError("Phone number is required");
      return;
    }
    // Tajik phone: +992 followed by 9 digits
    if (!/^\+?992\d{9}$/.test(trimmed.replace(/[\s-]/g, ""))) {
      setError("Enter a valid Tajik phone number (e.g. +992 123 456 789)");
      return;
    }
    sendOtpMutation.mutate({ phone: trimmed });
  };

  const handleVerifyOtp = (e: React.FormEvent) => {
    e.preventDefault();
    if (!otp.trim()) {
      setError("OTP is required");
      return;
    }
    verifyOtpMutation.mutate({ phone, otp });
  };

  return (
    <div className="min-h-[calc(100vh-4rem)] flex">
      {/* Left — Brand panel (desktop only) */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-indigo-600 via-indigo-700 to-purple-800 text-white flex-col items-center justify-center p-12 relative overflow-hidden">
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-20 -left-10 w-80 h-80 bg-purple-300 rounded-full mix-blend-multiply filter blur-3xl" />
          <div className="absolute bottom-20 right-10 w-80 h-80 bg-indigo-300 rounded-full mix-blend-multiply filter blur-3xl" />
        </div>
        <div className="relative z-10 text-center">
          <div className="flex items-center justify-center gap-3 mb-6">
            <Package className="h-12 w-12" />
            <span className="text-4xl font-bold">Radolfa</span>
          </div>
          <p className="text-xl text-indigo-200 max-w-md leading-relaxed">
            Your trusted marketplace for premium products. Sign in to access
            your account, track orders, and more.
          </p>
        </div>
      </div>

      {/* Right — Form */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-12 bg-background">
        <div className="w-full max-w-md space-y-8">
          {/* Mobile brand */}
          <div className="lg:hidden flex items-center justify-center gap-2 mb-4">
            <Package className="h-8 w-8 text-primary" />
            <span className="text-2xl font-bold text-foreground">Radolfa</span>
          </div>

          <div className="text-center">
            <h2 className="text-2xl sm:text-3xl font-bold text-foreground">
              {step === "phone" ? "Welcome back" : "Verify your phone"}
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">
              {step === "phone"
                ? "Enter your phone number to receive a verification code"
                : `We sent a code to ${phone}`}
            </p>
          </div>

          {error && (
            <div className="flex items-center gap-2 bg-destructive/10 border border-destructive/20 text-destructive px-4 py-3 rounded-lg text-sm">
              <AlertCircle className="h-4 w-4 shrink-0" />
              {error}
            </div>
          )}

          {step === "phone" ? (
            <form className="space-y-6" onSubmit={handleSendOtp}>
              <div className="space-y-2">
                <label
                  htmlFor="phone"
                  className="text-sm font-medium text-foreground"
                >
                  Phone Number
                </label>
                <div className="relative">
                  <Phone className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="phone"
                    type="tel"
                    required
                    className="pl-10 h-11"
                    placeholder="+992 123 456 789"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                  />
                </div>
              </div>

              <Button
                type="submit"
                className="w-full h-11"
                disabled={sendOtpMutation.isPending}
              >
                {sendOtpMutation.isPending ? "Sending..." : "Send Verification Code"}
              </Button>
            </form>
          ) : (
            <form className="space-y-6" onSubmit={handleVerifyOtp}>
              <div className="space-y-4">
                <label className="text-sm font-medium text-foreground block text-center">
                  Enter the 4-digit code
                </label>
                <OtpInput value={otp} onChange={setOtp} />
              </div>

              <div className="flex gap-3">
                <Button
                  type="button"
                  variant="outline"
                  className="flex-1 h-11"
                  onClick={() => {
                    setStep("phone");
                    setOtp("");
                    setError(null);
                  }}
                >
                  <ArrowLeft className="mr-2 h-4 w-4" />
                  Back
                </Button>
                <Button
                  type="submit"
                  className="flex-1 h-11"
                  disabled={verifyOtpMutation.isPending}
                >
                  {verifyOtpMutation.isPending ? "Verifying..." : "Verify"}
                </Button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
