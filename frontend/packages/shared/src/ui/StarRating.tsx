interface StarRatingProps {
  rating: number;
  size?: "sm" | "md" | "lg";
  showValue?: boolean;
}

const sizeMap = { sm: 16, md: 20, lg: 28 };

function Star({ fill, px }: { fill: "full" | "half" | "empty"; px: number }) {
  const id = `half-${px}-${Math.random().toString(36).slice(2, 7)}`;
  return (
    <svg
      width={px}
      height={px}
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      {fill === "half" && (
        <defs>
          <linearGradient id={id}>
            <stop offset="50%" stopColor="#fbbf24" />
            <stop offset="50%" stopColor="transparent" />
          </linearGradient>
        </defs>
      )}
      <polygon
        points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"
        fill={
          fill === "full"
            ? "#fbbf24"
            : fill === "half"
            ? `url(#${id})`
            : "transparent"
        }
        stroke="#fbbf24"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function StarRating({ rating, size = "md", showValue = false }: StarRatingProps) {
  const px = sizeMap[size];

  return (
    <div className="flex items-center gap-1">
      <div className="flex items-center">
        {[1, 2, 3, 4, 5].map((i) => {
          const fill =
            rating >= i ? "full" : rating >= i - 0.5 ? "half" : "empty";
          return <Star key={i} fill={fill} px={px} />;
        })}
      </div>
      {showValue && (
        <span className="text-sm font-medium text-foreground">{rating.toFixed(1)}</span>
      )}
    </div>
  );
}
