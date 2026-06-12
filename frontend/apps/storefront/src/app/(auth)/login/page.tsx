import type { Metadata } from "next";
import { StorefrontLoginForm } from "@radolfa/shared/auth";

export const metadata: Metadata = {
  title: "Sign In — Radolfa",
  description: "Sign in to your Radolfa account.",
};

export default function LoginPage() {
  return <StorefrontLoginForm />;
}
