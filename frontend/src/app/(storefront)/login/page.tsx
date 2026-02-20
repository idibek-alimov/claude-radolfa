import type { Metadata } from "next";
import { LoginForm } from "@/features/auth";

export const metadata: Metadata = {
  title: "Log In — Radolfa",
  description: "Log in to your Radolfa account.",
};

/**
 * Login page — now gets Navbar + Footer from the storefront layout.
 */
export default function LoginPage() {
  return <LoginForm />;
}
