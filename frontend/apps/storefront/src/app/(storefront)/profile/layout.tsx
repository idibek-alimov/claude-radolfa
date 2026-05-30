import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "My Profile — Radolfa",
  description: "View and manage your Radolfa profile.",
};

export default function ProfileLayout({ children }: { children: React.ReactNode }) {
  return children;
}
