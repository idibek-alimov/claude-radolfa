import type { Metadata } from "next";
import { ComingSoonPanel } from "@/widgets/profile-shell";

export const metadata: Metadata = { title: "Payment — Radolfa" };

// Scaffolded in Phase 6 so the shell's nav resolves. Saved payment methods
// are coming-soon per the redesign (order-scoped, redirect-based payment only).
export default function ProfilePaymentPage() {
  return <ComingSoonPanel titleKey="sectionPayment" messageKey="paymentComingSoon" />;
}
