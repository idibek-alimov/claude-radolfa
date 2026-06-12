import { useState, useEffect, type RefObject } from "react";

/**
 * Measures the height of a container element and computes how many table rows
 * fit inside it. Re-evaluates on every resize via ResizeObserver.
 *
 * @param containerRef - ref attached to the scrollable card div
 * @param rowHeight    - estimated pixel height of a single data row
 * @param headerHeight - pixel height of the table header row (default: 41)
 * @param min          - minimum page size to return (default: 10)
 */
export function useDynamicPageSize(
  containerRef: RefObject<HTMLElement | null>,
  rowHeight: number,
  headerHeight = 41,
  min = 10
): number {
  const [size, setSize] = useState(min);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    const compute = () => {
      const available = el.clientHeight - headerHeight;
      setSize(Math.max(min, Math.floor(available / rowHeight)));
    };

    compute();
    const ro = new ResizeObserver(compute);
    ro.observe(el);
    return () => ro.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return size;
}
