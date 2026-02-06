import { HeroBanner } from "@/widgets/HeroBanner";
import { TopSellingSection } from "@/widgets/ProductList";
import { TrustBanner } from "@/widgets/TrustBanner";

/**
 * Homepage — server component.
 *
 * Navbar and Footer are provided by the (storefront) layout.
 */
export default function HomePage() {
  return (
    <>
      <HeroBanner />
      <TopSellingSection />
      <TrustBanner />
    </>
  );
}
