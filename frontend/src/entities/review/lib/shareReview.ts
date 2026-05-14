import type { RefObject } from "react";
import type { StorefrontReview } from "../model/types";

export async function shareReview(
  nodeRef: RefObject<HTMLDivElement | null>,
  review: StorefrontReview,
  title: string,
  text: string
): Promise<"shared" | "downloaded" | "cancelled"> {
  if (!nodeRef.current) throw new Error("Share card ref is not mounted");

  const html2canvas = (await import("html2canvas")).default;
  const canvas = await html2canvas(nodeRef.current, {
    scale: 2,
    backgroundColor: null,
    useCORS: false,
    logging: false,
  });

  return new Promise((resolve, reject) => {
    canvas.toBlob(async (blob) => {
      if (!blob) {
        reject(new Error("Canvas conversion failed"));
        return;
      }

      const file = new File([blob], `review-${review.id}.png`, { type: "image/png" });

      if (
        typeof navigator !== "undefined" &&
        navigator.canShare?.({ files: [file] })
      ) {
        try {
          await navigator.share({ files: [file], title, text });
          resolve("shared");
        } catch (err) {
          if (err instanceof Error && err.name === "AbortError") {
            resolve("cancelled");
          } else {
            reject(err);
          }
        }
      } else {
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `review-${review.id}.png`;
        a.click();
        URL.revokeObjectURL(url);
        resolve("downloaded");
      }
    }, "image/png");
  });
}
