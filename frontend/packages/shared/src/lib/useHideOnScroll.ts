"use client";

import { useEffect, useRef, useState } from "react";

interface UseHideOnScrollOptions {
  /** Minimum scroll delta (px) before a direction change is registered. */
  threshold?: number;
  /** Scroll depth (px) below which the element is always shown. */
  revealAtTop?: number;
}

/**
 * Tracks scroll direction and returns whether a fixed bottom/top bar should
 * hide itself. `hidden` becomes true when the user scrolls down past
 * `revealAtTop`, and false when they scroll up or are near the top.
 *
 * Multiple components can call this independently with the same options to
 * stay in sync without sharing state directly.
 */
export function useHideOnScroll(options: UseHideOnScrollOptions = {}): boolean {
  const { threshold = 8, revealAtTop = 80 } = options;
  const [hidden, setHidden] = useState(false);
  const lastY = useRef(0);
  const ticking = useRef(false);

  useEffect(() => {
    if (typeof window === "undefined") return;
    lastY.current = window.scrollY;

    const onScroll = () => {
      if (ticking.current) return;
      ticking.current = true;
      requestAnimationFrame(() => {
        const y = window.scrollY;
        const delta = y - lastY.current;

        if (y < revealAtTop) {
          setHidden(false);
        } else if (Math.abs(delta) > threshold) {
          setHidden(delta > 0);
        }

        lastY.current = y;
        ticking.current = false;
      });
    };

    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, [threshold, revealAtTop]);

  return hidden;
}
