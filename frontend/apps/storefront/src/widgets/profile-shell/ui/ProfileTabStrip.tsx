"use client";

import { useLayoutEffect, useRef } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTranslations } from "next-intl";
import { PROFILE_SECTIONS, isSectionActive } from "../lib/sections";

/** Mobile sticky section-tab strip with a sliding underline indicator —
 *  verbatim from the B-Magenta mobile reference `<!-- SECTION TAB STRIP -->`
 *  (`.tabbar` / `.tabnav` / `.tabink`), reproduced with Tailwind utilities and
 *  a React port of the underline-positioning script. */
export default function ProfileTabStrip() {
  const t = useTranslations("profile");
  const pathname = usePathname();

  const barRef = useRef<HTMLElement>(null);
  const inkRef = useRef<HTMLSpanElement>(null);
  const tabRefs = useRef<Record<string, HTMLAnchorElement | null>>({});

  const moveInk = () => {
    const bar = barRef.current;
    const ink = inkRef.current;
    const active = tabRefs.current[PROFILE_SECTIONS.find((s) => isSectionActive(s, pathname))?.seg ?? ""];
    if (!bar || !ink || !active) return;

    ink.style.width = `${active.offsetWidth}px`;
    ink.style.transform = `translateX(${active.offsetLeft}px)`;

    const left = active.offsetLeft;
    const right = left + active.offsetWidth;
    if (left < bar.scrollLeft) {
      bar.scrollTo({ left: left - 12, behavior: "smooth" });
    } else if (right > bar.scrollLeft + bar.clientWidth) {
      bar.scrollTo({ left: right - bar.clientWidth + 12, behavior: "smooth" });
    }
  };

  useLayoutEffect(() => {
    moveInk();
    window.addEventListener("resize", moveInk);
    return () => window.removeEventListener("resize", moveInk);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pathname]);

  return (
    <nav
      ref={barRef}
      aria-label="Account sections"
      className="tabbar relative flex bg-white border-b border-ink/[0.09] overflow-x-auto scrollbar-hide sticky top-0 z-30 shadow-[0_2px_10px_rgba(26,10,24,0.05)] [scroll-snap-type:x_proximity]"
    >
      {PROFILE_SECTIONS.map((section) => {
        const active = isSectionActive(section, pathname);
        const TabIcon = section.tabIcon;
        return (
          <Link
            key={section.seg}
            href={section.href}
            ref={(el) => {
              tabRefs.current[section.seg] = el;
            }}
            role="tab"
            aria-selected={active}
            className={`flex-1 min-w-[64px] flex flex-col items-center gap-1 pt-2.5 px-2 pb-[11px] text-[11px] font-bold leading-none whitespace-nowrap transition-colors [scroll-snap-align:center] ${
              active ? "text-mag" : "text-ink/50 hover:text-ink/80"
            }`}
          >
            <TabIcon size={21} className={active ? "-translate-y-px transition-transform" : "transition-transform"} />
            {t(section.i18nKey)}
          </Link>
        );
      })}
      <span
        ref={inkRef}
        className="tabink absolute bottom-0 left-0 h-[3px] rounded-t-[3px] bg-mag transition-[transform,width] duration-[260ms] ease-[cubic-bezier(.34,1.2,.5,1)] will-change-[transform,width]"
      />
    </nav>
  );
}
