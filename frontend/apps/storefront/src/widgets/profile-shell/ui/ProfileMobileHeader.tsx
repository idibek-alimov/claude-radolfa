"use client";

import Link from "next/link";
import { useAuth } from "@radolfa/shared/auth";
import { getInitials } from "../lib/initials";
import { RewardsIcon, SettingsIcon } from "../lib/icons";

/** Mobile magenta gradient header — verbatim from the B-Magenta mobile
 *  reference `<!-- HEADER (magenta) -->`. Replaces the inherited Navbar on
 *  mobile profile routes (CSS-hidden via the `profile-chrome` body class). */
export default function ProfileMobileHeader() {
  const { user } = useAuth();

  const initials = getInitials(user?.name, user?.phone);
  const tier = user?.loyalty?.tier;

  return (
    <header className="bg-gradient-to-br from-mag to-maglo text-white">
      <div className="px-4 pt-3 pb-3">
        <div className="flex items-center gap-3">
          <div className="w-14 h-14 rounded-full bg-white/20 backdrop-blur flex items-center justify-center text-[18px] font-black shrink-0">
            {initials}
          </div>
          <div className="min-w-0 flex-1">
            <div className="font-black text-[18px] leading-tight truncate">
              {user?.name || user?.phone}
            </div>
            {tier && (
              <span className="mt-1 inline-flex px-2 py-0.5 rounded-full bg-[#FFCC4F] text-ink text-[10px] font-bold items-center gap-1">
                <RewardsIcon size={10} />
                Crown {tier.name}
              </span>
            )}
          </div>
          <Link
            href="/profile/settings"
            className="w-9 h-9 rounded-full bg-white/15 flex items-center justify-center shrink-0"
            aria-label="Settings"
          >
            <SettingsIcon />
          </Link>
        </div>
      </div>
    </header>
  );
}
