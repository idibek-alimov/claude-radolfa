import type { Tag } from "../model/types";

interface TagBadgeProps {
  tag: Tag;
  size?: "sm" | "md";
}

export function TagBadge({ tag, size = "sm" }: TagBadgeProps) {
  const sizeClasses =
    size === "sm" ? "text-[10px] px-1.5 py-0.5" : "text-xs px-2 py-1";

  return (
    <span
      className={`inline-block rounded font-semibold uppercase tracking-wide text-white ${sizeClasses}`}
      style={{ backgroundColor: `#${tag.colorHex}` }}
    >
      {tag.name}
    </span>
  );
}
