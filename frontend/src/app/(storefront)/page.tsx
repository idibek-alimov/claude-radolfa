import type { Metadata } from "next";
// import { HeroBanner } from "@/widgets/HeroBanner";
// import { HomeCollections } from "@/widgets/HomeCollections";
// import { TrustBanner } from "@/widgets/TrustBanner";
import { CatalogSection } from "@/widgets/ProductList";

export const metadata: Metadata = {
  title: "Radolfa — Premium E-Commerce",
  description: "Shop top-quality products at Radolfa. Trusted marketplace with secure payments and fast delivery.",
};

/**
 * Homepage — server component.
 *
 * Navbar and Footer are provided by the (storefront) layout.
 */
export default function HomePage() {
  return (
    <>
      {/* <HeroBanner /> */}
      {/* <HomeCollections /> */}
      {/* <TrustBanner /> */}
      <CatalogSection />
    </>
  );
}
