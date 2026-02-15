import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Search — Radolfa",
  description: "Search for products on Radolfa.",
};

export default function SearchLayout({ children }: { children: React.ReactNode }) {
  return children;
}
