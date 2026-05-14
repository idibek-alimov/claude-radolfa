"use client";

import { useMemo } from "react";

interface Point {
  date: string;
  value: number;
}

interface Props {
  points: Point[];
  color?: string;
  height?: number;
}

const WIDTH = 600;
const PAD = { top: 8, bottom: 8, left: 4, right: 4 } as const;

export function MetricSparkline({ points, color = "#CB11AB", height = 80 }: Props) {
  const { path, area, minVal, maxVal, minIdx, maxIdx } = useMemo(() => {
    if (points.length === 0) return { path: "", area: "", minVal: 0, maxVal: 0, minIdx: 0, maxIdx: 0 };

    const values = points.map((p) => p.value);
    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = max - min || 1;

    const xs = points.map((_, i) =>
      PAD.left + (i / Math.max(points.length - 1, 1)) * (WIDTH - PAD.left - PAD.right)
    );
    const ys = values.map(
      (v) => PAD.top + ((max - v) / range) * (height - PAD.top - PAD.bottom)
    );

    const coords = xs.map((x, i) => `${x.toFixed(1)},${ys[i].toFixed(1)}`);
    const polyline = coords.join(" ");

    const areaPath = `M${xs[0].toFixed(1)},${height - PAD.bottom} L${coords.join(" L")} L${xs[xs.length - 1].toFixed(1)},${height - PAD.bottom} Z`;

    const minIdx = values.indexOf(min);
    const maxIdx = values.indexOf(max);

    return { path: polyline, area: areaPath, minVal: min, maxVal: max, minIdx, maxIdx };
  }, [points, height]);

  if (points.length === 0) {
    return (
      <div
        className="flex items-center justify-center text-xs text-muted-foreground border border-dashed rounded-lg"
        style={{ height }}
      >
        No data
      </div>
    );
  }

  const xs = points.map(
    (_, i) => PAD.left + (i / Math.max(points.length - 1, 1)) * (WIDTH - PAD.left - PAD.right)
  );
  const values = points.map((p) => p.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const ys = values.map(
    (v) => PAD.top + ((max - v) / range) * (height - PAD.top - PAD.bottom)
  );

  return (
    <svg
      viewBox={`0 0 ${WIDTH} ${height}`}
      className="w-full"
      style={{ height }}
      role="img"
      aria-label="sparkline"
    >
      {/* Area fill */}
      <path d={area} fill={color} fillOpacity={0.08} />

      {/* Line */}
      <polyline
        points={path}
        fill="none"
        stroke={color}
        strokeWidth={2}
        strokeLinejoin="round"
        strokeLinecap="round"
      />

      {/* Max label */}
      {maxIdx >= 0 && (
        <>
          <circle cx={xs[maxIdx]} cy={ys[maxIdx]} r={3} fill={color} />
          <text
            x={xs[maxIdx]}
            y={ys[maxIdx] - 6}
            textAnchor="middle"
            fontSize={9}
            fill={color}
            fontWeight={600}
          >
            {maxVal.toFixed(0)}
          </text>
        </>
      )}

      {/* Min label */}
      {minIdx >= 0 && min !== max && (
        <>
          <circle cx={xs[minIdx]} cy={ys[minIdx]} r={3} fill="#94a3b8" />
          <text
            x={xs[minIdx]}
            y={ys[minIdx] + 14}
            textAnchor="middle"
            fontSize={9}
            fill="#94a3b8"
          >
            {minVal.toFixed(0)}
          </text>
        </>
      )}
    </svg>
  );
}
