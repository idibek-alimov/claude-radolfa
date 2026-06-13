import type { Metadata } from "next";
import { ComingSoonPanel } from "@/widgets/profile-shell";

export const metadata: Metadata = { title: "Crown Rewards — Radolfa" };

// Scaffolded in Phase 6 so the shell's nav resolves. Real Rewards dashboard
// (ProfileLoyaltyHero, ways-to-earn, points activity) is built in file 03.
export default function ProfileRewardsPage() {
  return <ComingSoonPanel titleKey="sectionRewards" messageKey="comingSoon" />;
}
