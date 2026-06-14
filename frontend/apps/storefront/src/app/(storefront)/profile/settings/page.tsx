import type { Metadata } from "next";
import { ProfileSettings } from "@/widgets/profile-settings";

export const metadata: Metadata = { title: "Settings — Radolfa" };

// Phase 12 — real Settings: profile details, change-phone, notification
// prefs, static preferences/help, sign out.
export default function ProfileSettingsPage() {
  return <ProfileSettings />;
}
