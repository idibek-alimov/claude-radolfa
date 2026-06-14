import type { Metadata } from "next";
import { AddressBookPage } from "@/features/address-book";

export const metadata: Metadata = { title: "My Addresses — Radolfa" };

// Phase 10 — real address book (features/address-book on top of entities/address).
export default function ProfileAddressesPage() {
  return <AddressBookPage />;
}
