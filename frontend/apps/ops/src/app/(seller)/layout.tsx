"use client";

import { SellerShell } from "@/widgets/seller-shell";

export default function SellerLayout({ children }: { children: React.ReactNode }) {
  return <SellerShell>{children}</SellerShell>;
}
