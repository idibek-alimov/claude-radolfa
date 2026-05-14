import type { Metadata } from "next";
import { AdminShell } from "@/widgets/AdminShell";

export const metadata: Metadata = {
  title: "Admin — Radolfa",
  robots: { index: false, follow: false },
};

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <AdminShell>{children}</AdminShell>;
}
