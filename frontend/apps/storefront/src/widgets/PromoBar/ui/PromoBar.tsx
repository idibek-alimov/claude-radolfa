/** Static promotional bar pinned at the top of every storefront page.
 *  Server Component — no browser APIs, no hooks. */
export default function PromoBar() {
  return (
    <div className="bg-gradient-to-r from-mag to-maghi text-white text-[12px] font-semibold">
      <div className="max-w-[1440px] mx-auto px-6 h-9 flex items-center justify-center gap-3">
        {/* Crown SVG */}
        <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
          <path d="M5 16l-3-8 5.5 4L12 4l4.5 8L22 8l-3 8H5z" />
        </svg>
        <span>
          SUPER SALE — up to 70% off · use code{" "}
          <span className="bg-white/25 px-1.5 rounded">RADO70</span>
          {" "}· free shipping over 500 TJS
        </span>
      </div>
    </div>
  );
}
