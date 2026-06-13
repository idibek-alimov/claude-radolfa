import type { Metadata } from "next";
import { ComingSoonPanel } from "@/widgets/profile-shell";

export const metadata: Metadata = { title: "My Orders — Radolfa" };

// Scaffolded in Phase 6 so the shell's nav resolves. Real order list
// (filters, stepper, pagination) is built in Phase 8.
export default function ProfileOrdersPage() {
  return <ComingSoonPanel titleKey="sectionOrders" messageKey="noOrdersYet" />;
}
