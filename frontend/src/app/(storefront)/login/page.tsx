import type { Metadata } from "next";
import { LoginForm } from "@/features/auth";

export const metadata: Metadata = {
  title: "Sign In — Radolfa",
  description: "Sign in to your Radolfa account.",
};

/**
 * Login page — now gets Navbar + Footer from the storefront layout.
 */
export default function LoginPage() {
  return <LoginForm />;
}
