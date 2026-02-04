import { Navbar } from "@/widgets/Navbar";
import { TopSellingSection } from "@/widgets/ProductList";

/**
 * Homepage — server component.
 *
 * All data-fetching logic lives in TopSellingSection (a "use client"
 * component) so that useQuery can be used without violating the
 * server-component boundary.
 */
export default function HomePage() {
  return (
    <div>
      <Navbar />
      <main>
        <TopSellingSection />
      </main>
    </div>
  );
}
