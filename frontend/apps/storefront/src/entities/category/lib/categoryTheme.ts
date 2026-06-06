/**
 * Slug → gradient + text-color map for category posters and tiles.
 *
 * Gradients are extracted verbatim from the BIG CATEGORY POSTERS block in
 * design_handoff/B-Magenta-Desktop-FINAL.html. Each entry provides:
 *   gradient — Tailwind `bg-gradient-to-br` from/to classes
 *   text     — whether the card text should be white or ink (dark)
 *
 * Category slugs seeded in dev (tops, bottoms, outerwear, dresses, sleepwear,
 * footwear, accessories, clothing, electronics, shoes) are mapped to the
 * closest design archetype. Unknown slugs get a deterministic fallback colour
 * by hashing the slug mod the palette length.
 */

export interface CategoryTheme {
  gradient: string;
  text: "white" | "ink";
}

const EXPLICIT_MAP: Record<string, CategoryTheme> = {
  // Design reference exact names
  bags:       { gradient: "from-mag to-maghi",               text: "white" },
  watches:    { gradient: "from-[#0E1116] to-[#2A3140]",     text: "white" },
  fragrance:  { gradient: "from-gold to-[#E89E2E]",          text: "ink"   },
  apparel:    { gradient: "from-[#7A1F2B] to-[#B82F45]",     text: "white" },
  home:       { gradient: "from-emerald to-[#3CC58A]",       text: "white" },

  // Seeded dev slugs → closest archetype
  tops:       { gradient: "from-[#7A1F2B] to-[#B82F45]",     text: "white" },
  bottoms:    { gradient: "from-[#7A1F2B] to-[#B82F45]",     text: "white" },
  outerwear:  { gradient: "from-[#0E1116] to-[#2A3140]",     text: "white" },
  dresses:    { gradient: "from-mag to-maghi",               text: "white" },
  sleepwear:  { gradient: "from-mag to-maghi",               text: "white" },
  footwear:   { gradient: "from-emerald to-[#3CC58A]",       text: "white" },
  accessories:{ gradient: "from-gold to-[#E89E2E]",          text: "ink"   },
  clothing:   { gradient: "from-[#7A1F2B] to-[#B82F45]",     text: "white" },
  electronics:{ gradient: "from-[#0E1116] to-[#2A3140]",     text: "white" },
  shoes:      { gradient: "from-emerald to-[#3CC58A]",       text: "white" },
};

/** Palette for deterministic fallback when slug is not in the explicit map. */
const FALLBACK_PALETTE: CategoryTheme[] = [
  { gradient: "from-mag to-maghi",               text: "white" },
  { gradient: "from-[#0E1116] to-[#2A3140]",     text: "white" },
  { gradient: "from-gold to-[#E89E2E]",          text: "ink"   },
  { gradient: "from-[#7A1F2B] to-[#B82F45]",     text: "white" },
  { gradient: "from-emerald to-[#3CC58A]",       text: "white" },
];

function hashSlug(slug: string): number {
  let h = 0;
  for (let i = 0; i < slug.length; i++) {
    h = (h * 31 + slug.charCodeAt(i)) >>> 0;
  }
  return h;
}

/** Returns the gradient + text-color for a given category slug. */
export function getCategoryTheme(slug: string): CategoryTheme {
  return (
    EXPLICIT_MAP[slug] ??
    FALLBACK_PALETTE[hashSlug(slug) % FALLBACK_PALETTE.length]
  );
}
