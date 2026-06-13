import type { Metadata } from "next";
import { ComingSoonPanel } from "@/widgets/profile-shell";

export const metadata: Metadata = { title: "Settings — Radolfa" };

// Scaffolded in Phase 6 so the shell's nav resolves. Real Settings (profile
// details, change-phone, notification prefs) is built in file 03.
export default function ProfileSettingsPage() {
  return <ComingSoonPanel titleKey="sectionSettings" messageKey="comingSoon" />;
}
