import type { Metadata } from "next";
import { LoginForm } from "@radolfa/shared/auth";

export const metadata: Metadata = {
  title: "Sign In — Radolfa Ops",
};

export default function OpsLoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <LoginForm />
    </div>
  );
}
