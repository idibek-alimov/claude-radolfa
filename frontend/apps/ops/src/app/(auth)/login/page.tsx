import type { Metadata } from "next";
import { LoginForm } from "@radolfa/shared/auth";

export const metadata: Metadata = {
  title: "Sign In — Radolfa Ops",
};

export default function OpsLoginPage() {
  return <LoginForm />;
}
