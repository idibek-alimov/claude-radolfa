import type { Metadata } from "next";
import { Navbar } from "@/widgets/Navbar";

export const metadata: Metadata = {
  title: "Admin — Radolfa",
  robots: { index: false, follow: false },
};

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col min-h-screen">
      <Navbar />
      <main className="flex-1">{children}</main>
    </div>
  );
}
