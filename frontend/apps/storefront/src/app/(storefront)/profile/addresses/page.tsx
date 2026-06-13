import type { Metadata } from "next";
import { ComingSoonPanel } from "@/widgets/profile-shell";

export const metadata: Metadata = { title: "My Addresses — Radolfa" };

// Scaffolded in Phase 6 so the shell's nav resolves. Real address book
// (features/address-book on top of entities/address) is built in file 03.
export default function ProfileAddressesPage() {
  return <ComingSoonPanel titleKey="sectionAddresses" messageKey="addressEmpty" />;
}
