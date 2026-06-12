import { forwardRef } from "react";
import type { StorefrontReview } from "../model/types";

interface Props {
  review: StorefrontReview;
}

/** Off-screen 1080×1080 template for html2canvas capture. No remote images — text only. */
export const ShareableReviewCard = forwardRef<HTMLDivElement, Props>(
  function ShareableReviewCard({ review }, ref) {
    const stars = "★".repeat(review.rating) + "☆".repeat(5 - review.rating);
    const bodyText = review.body.length > 200 ? review.body.slice(0, 197) + "…" : review.body;

    return (
      <div
        ref={ref}
        style={{
          position: "absolute",
          left: -9999,
          top: 0,
          pointerEvents: "none",
          width: 1080,
          height: 1080,
          background: "linear-gradient(135deg, #fdf4ff 0%, #f3e8ff 100%)",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          padding: "80px 90px",
          fontFamily: "'Geist', 'Inter', sans-serif",
          boxSizing: "border-box",
        }}
      >
        {/* Top: branding */}
        <div style={{ fontSize: 36, fontWeight: 700, color: "#CB11AB", letterSpacing: "-0.5px" }}>
          radolfa.tj
        </div>

        {/* Middle: rating + content */}
        <div style={{ flex: 1, display: "flex", flexDirection: "column", justifyContent: "center", gap: 36 }}>
          <div style={{ fontSize: 72, color: "#CB11AB", letterSpacing: 4, lineHeight: 1 }}>
            {stars}
          </div>

          {review.title && (
            <p style={{ fontSize: 48, fontWeight: 700, color: "#1a1a1a", lineHeight: 1.2, margin: 0 }}>
              {review.title}
            </p>
          )}

          <p style={{ fontSize: 36, color: "#333", lineHeight: 1.6, margin: 0 }}>
            {bodyText}
          </p>
        </div>

        {/* Bottom: author + verified badge */}
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <div>
            <p style={{ fontSize: 30, fontWeight: 600, color: "#1a1a1a", margin: 0 }}>
              {review.authorName}
            </p>
            <p style={{ fontSize: 24, color: "#16a34a", marginTop: 6, margin: 0 }}>
              ✓ Verified Purchase
            </p>
          </div>
          <div
            style={{
              background: "#CB11AB",
              color: "#fff",
              borderRadius: 50,
              padding: "12px 32px",
              fontSize: 26,
              fontWeight: 600,
            }}
          >
            ★ {review.rating}/5
          </div>
        </div>
      </div>
    );
  }
);
