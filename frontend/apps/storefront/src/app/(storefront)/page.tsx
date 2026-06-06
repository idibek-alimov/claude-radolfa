import type { Metadata } from "next";
import { HomeHero } from "@/widgets/HomeHero";
import { CategoryPosters } from "@/widgets/CategoryPosters";
import { HomeProductRows } from "@/widgets/HomeProductRows";

export const metadata: Metadata = {
  title: "Radolfa — Premium E-Commerce",
  description: "Shop top-quality products at Radolfa. Trusted marketplace with secure payments and fast delivery.",
};

export default function HomePage() {
  return (
    <>
      <HomeHero />
      <CategoryPosters />
      <HomeProductRows />
    </>
  );
}
